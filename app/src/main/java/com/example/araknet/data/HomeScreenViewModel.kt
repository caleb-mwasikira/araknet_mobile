package com.example.araknet.data

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.araknet.BuildConfig
import com.example.araknet.MainActivity
import com.example.araknet.utils.compressData
import com.example.araknet.utils.toRawString
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import kotlin.system.measureTimeMillis


data class ProxyServer(
    val id: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val ipInfo: IPInfo?,
    var status: ProxyStatus = ProxyStatus.Offline,
    var ping: Int = 0,
    var downloadSpeed: Float = 0f,
    var uploadSpeed: Float = 0f,
    var isConnected: Boolean = false,
)

fun ProxyServer.address(): String {
    return "${this.protocol}://${this.host}:${this.port}"
}

@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val api: ApiService? = MainActivity.retrofitService
        const val SELECT_PROXY_ACTION: String = "com.example.SELECT_PROXY_ACTION"
    }

    private val _proxyServers = MutableStateFlow<List<ProxyServer>>(listOf())
    val proxyServers = _proxyServers.asStateFlow()

    private val _errors = MutableStateFlow<Error?>(null)
    val errors
        get() = _errors.asStateFlow()

    private val getCookies: List<Cookie> = lazy {
        val cookieJar = CustomCookieJar(application.baseContext)
        val url = URI(BuildConfig.REMOTE_URL).toHttpUrlOrNull() ?: return@lazy emptyList()

        cookieJar.loadForRequest(url)
    }.value

    init {
        viewModelScope.launch {
            getProxyServers()
        }
    }

    fun selectProxy(proxy: ProxyServer) {
        viewModelScope.launch {
            val isActive = testProxyConnection(proxy.host, proxy.port)
            if (isActive) {
                val (upSpeed, downSpeed) = measureSpeeds(proxy)

                // Atomically update speed values
                _proxyServers.update { list ->
                    list.map { _proxyServer ->
                        if (_proxyServer.id == proxy.id) {
                            _proxyServer.copy(
                                uploadSpeed = upSpeed.toFloat(),
                                downloadSpeed = downSpeed.toFloat(),
                                status = ProxyStatus.Connected,
                            )
                        } else {
                            _proxyServer
                        }
                    }
                }
            } else {
                _proxyServers.update { list ->
                    list.map { _proxyServer ->
                        if (_proxyServer.id == proxy.id) {
                            _proxyServer.copy(
                                status = ProxyStatus.Offline,
                            )
                        } else {
                            _proxyServer
                        }
                    }
                }

                _errors.emit(
                    Error("Proxy server ${proxy.id} is currently offline")
                )
            }

            // Send broadcast to notify VPN service
            Log.d(MainActivity.TAG, "Sending BROADCAST to select proxy server ${proxy.address()}")

            val intent = Intent(SELECT_PROXY_ACTION)
            intent.putExtra("proxy_address", proxy.address())

            val context = getApplication<Application>().applicationContext
            context.sendBroadcast(intent)
        }
    }

    private suspend fun <T> extractResponseData(response: Response<ApiResponse<T>>): T? {
        if (response.isSuccessful) {
            Log.d(MainActivity.TAG, "Response successful; $response")
            val apiResponse: ApiResponse<T>? = response.body()
            return apiResponse?.data
        }

        Log.d(MainActivity.TAG, "Response failed; $response")
        val responseString: String = response.errorBody()?.string() ?: run {
            _errors.emit(
                Error("Request failed with unknown error message")
            )
            return null
        }

        val apiResponse: ApiResponse<*> = Gson().fromJson(responseString, ApiResponse::class.java)
        val errors = apiResponse.errors?.values ?: run {
            _errors.emit(
                Error(apiResponse.message)
            )
            return null
        }

        errors.forEach { error ->
            _errors.emit(
                Error(error)
            )
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getProxyServers() {
        Log.d(MainActivity.TAG, "Fetching proxy servers")

        try {
            val response = api?.getProxies() ?: run {
                Log.d(MainActivity.TAG, "NULL retrofitService")
                return
            }
            val proxyServers: Set<ProxyDto> = extractResponseData(response)?.toSet() ?: run {
                Log.d(MainActivity.TAG, "Zero proxies connected to network")
                _errors.emit(
                    Error("Zero proxies connected to network")
                )
                return
            }
            Log.d(MainActivity.TAG, "Collected ${proxyServers.size} proxies from network")

            proxyServers.forEach { proxy ->
                viewModelScope.launch(Dispatchers.IO) {
                    // Using static IP address. TODO: Change to dynamic IP address in production
                    val remoteURL = URI(BuildConfig.REMOTE_URL)
                    proxy.host = remoteURL.host

                    val isAvailable = testProxyConnection(proxy.host, proxy.port)

                    // Create proxy server object
                    val proxyServer = ProxyServer(
                        id = proxy.address(),
                        protocol = proxy.protocol,
                        host = proxy.host,
                        port = proxy.port,
                        ipInfo = proxy.ipInfo,
                        status = if (isAvailable) ProxyStatus.Online else ProxyStatus.Offline,
                    )

                    // Atomically update state
                    _proxyServers.update { list -> list + proxyServer }

                    if (isAvailable) {
                        Log.d(MainActivity.TAG, "Proxy server ${proxy.address()} is online")

                        val (upSpeed, downSpeed) = measureSpeeds(proxyServer)

                        // Atomically update speed values
                        _proxyServers.update { list ->
                            list.map { _proxyServer ->
                                if (_proxyServer.id == proxyServer.id) {
                                    _proxyServer.copy(
                                        uploadSpeed = upSpeed.toFloat(),
                                        downloadSpeed = downSpeed.toFloat(),
                                    )
                                } else {
                                    _proxyServer
                                }
                            }
                        }
                    }
                }
            }


        } catch (e: Exception) {
            Log.d(MainActivity.TAG, "Error fetching proxy servers; $e")
            _errors.emit(
                Error(
                    message = "Backend server currently unreachable"
                )
            )
        }
    }

    private suspend fun testProxyConnection(proxyHost: String, proxyPort: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress(proxyHost, proxyPort),
                        30000
                    )
                }
                true
            } catch (e: Exception) {
                Log.d(MainActivity.TAG, "Error testing proxy connection; $e")
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun measureSpeeds(proxy: ProxyServer): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(MainActivity.TAG, "Measuring proxy speeds for ${proxy.address()}")

                val destination = URI(BuildConfig.REMOTE_URL)
                val uploadDeferred = async { measureUploadSpeed(proxy, destination) }
                val downloadDeferred = async { measureDownloadSpeed(proxy, destination) }

                // Await both results concurrently
                val uploadSpeed = uploadDeferred.await()
                val downloadSpeed = downloadDeferred.await()

                return@withContext uploadSpeed to downloadSpeed
            } catch (e: Exception) {
                Log.d(
                    MainActivity.TAG,
                    "Error measuring upload/download speeds for ${proxy.address()}: $e",
                )
                0.0 to 0.0
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun measureUploadSpeed(proxy: ProxyServer, dest: URI): Double {
        try {
            Log.d(
                MainActivity.TAG,
                "Measuring upload speed for proxy server ${proxy.address()}"
            )

            // Connect to SOCKS proxy
            val proxySocket: Socket = SOCKS5.newTCPConnection(
                proxy.host, proxy.port,
                dest.host, dest.port
            )

            proxySocket.use { socket ->
                val uploadEndpoint: URI = dest.resolve("/api/speed/upload")
                val data = ByteArray(5 * 1024 * 1024) { 'A'.code.toByte() } // 5MB
                val compressedData = compressData(data)
                val b64EncodedData = java.util.Base64.getEncoder().encodeToString(compressedData)

                val request = Request.Builder()
                    .url(uploadEndpoint.toURL())
                    .addHeader("Cookie", getCookies.joinToString("; ") { cookie ->
                        "${cookie.name}=${cookie.value}"
                    })
                    .addHeader("Authorization", "Bearer ${BuildConfig.ANDROID_API_KEY}")
                    .addHeader("Content-Type", "application/octet-stream")
                    .addHeader("Content-Encoding", "gzip")
                    .addHeader("Connection", "close")
                    .post(
                        b64EncodedData.toRequestBody(
                            "application/octet-stream".toMediaType(),
                        )
                    )
                    .build()

                val timeMs = measureTimeMillis {
                    val proxyInputStream = socket.getInputStream()
                    val proxyOutputStream = socket.getOutputStream()

                    val requestRawString = request.toRawString()
                    Log.d(MainActivity.TAG, "Upload request body:\n$requestRawString")

                    proxyOutputStream.write(requestRawString.toByteArray())
                    proxyOutputStream.flush()

                    // Read response sent by proxy
                    val buffer = ByteArray(1024)
                    while (true) {
                        val bytesRead = proxyInputStream.read(buffer)
                        if (bytesRead == -1) break // Connection closed or EOF
                    }

                }.toDouble()

                // Convert bytes to Megabytes and ms to seconds
                val megaBytes = data.size / 1024 / 1024
                val timeSec = timeMs / 1000

                Log.d(MainActivity.TAG, "Uploaded $megaBytes MBs in $timeSec s")
                return megaBytes / timeSec
            }

        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Error measuring upload speed; $e")
            return 0.0
        }
    }

    private fun measureDownloadSpeed(proxy: ProxyServer, dest: URI): Double {
        try {
            Log.d(
                MainActivity.TAG,
                "Measuring download speed for proxy server ${proxy.address()}"
            )

            // Connect to SOCKS proxy
            val proxySocket: Socket = SOCKS5.newTCPConnection(
                proxy.host, proxy.port,
                dest.host, dest.port
            )

            proxySocket.use { socket ->
                val downloadEndpoint: URI = dest.resolve("/api/speed/download")
                val request = Request.Builder()
                    .url(downloadEndpoint.toURL())
                    .addHeader("Cookie", getCookies.joinToString("; ") { cookie ->
                        "${cookie.name}=${cookie.value}"
                    })
                    .addHeader("Authorization", "Bearer ${BuildConfig.ANDROID_API_KEY}")
                    .addHeader("Accept-Encoding", "gzip")
                    .addHeader("Connection", "close")
                    .build()

                var totalBytesRead: Int
                val timeMs = measureTimeMillis {
                    val proxyInputStream = socket.getInputStream()
                    val proxyOutputStream = socket.getOutputStream()

                    proxyOutputStream.write(request.toRawString().toByteArray())
                    proxyOutputStream.flush()

                    // Read response sent by proxy
                    val responseBody = readHttpResponseBody(proxyInputStream)
                    totalBytesRead = responseBody.size
                }.toDouble()

                // Convert bytes to Megabytes and ms to seconds
                val megaBytes = totalBytesRead / 1024 / 1024
                val timeSec = timeMs / 1000

                Log.d(MainActivity.TAG, "Downloaded $megaBytes MBs in $timeSec s")
                return megaBytes / timeSec // Speed in MB/s
            }

        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Error measuring download speed; $e")
            return 0.0
        }
    }

    private fun readHttpResponseBody(inputStream: InputStream): ByteArray {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val headers = mutableMapOf<String, String>()

        // Read HTTP response headers
        while (true) {
            val line: String = reader.readLine() ?: break
            if (line.isEmpty()) break // Headers end at empty line

            val parts = line.split(": ", limit = 2)
            if (parts.size == 2) {
                headers[parts[0]] = parts[1]
            }
        }

        Log.d(MainActivity.TAG, "Response Headers: $headers")

        // Read raw data
        val responseBody = ByteArrayOutputStream()
        val buffer = ByteArray(10 * 1024) // 10Kb buffer

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break // Connection closed or EOF
            responseBody.write(buffer, 0, bytesRead)
        }

        return responseBody.toByteArray()
    }
}
