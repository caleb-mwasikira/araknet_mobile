package com.example.araknet.data

import android.app.Application
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
import kotlinx.coroutines.coroutineScope
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
import java.io.IOException
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
    return "${this.host}:${this.port}"
}

@RequiresApi(Build.VERSION_CODES.O)
class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val api: ApiService? = MainActivity.retrofitService
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
                    // Changing proxy's host to a known static IP address
                    val proxyUrl = URI(BuildConfig.REMOTE_URL)
                    proxy.host = proxyUrl.host

                    val isAvailable = testProxyConnection(proxy)

                    // Create proxy server object
                    val proxyServer = ProxyServer(
                        id = proxy.address(),
                        protocol = proxy.protocol,
                        host = proxy.host,
                        port = proxy.port,
                        ipInfo = proxy.ipInfo,
                        status = if (isAvailable) ProxyStatus.Connecting else ProxyStatus.Offline,
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
            Log.d(MainActivity.TAG, "Error fetching proxy servers; ${e.message}")
            _errors.emit(
                Error(
                    message = "Backend server currently unreachable"
                )
            )
        }
    }

    private suspend fun testProxyConnection(proxy: ProxyDto): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress(proxy.host, proxy.port),
                        30000
                    )
                }
                true
            } catch (e: Exception) {
                Log.d(MainActivity.TAG, "Error testing proxy connection; ${e.message}")
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun measureSpeeds(proxy: ProxyServer): Pair<Double, Double> {
        return try {
            val targetUrl = URI(BuildConfig.REMOTE_URL)
            val proxySocket: Socket = newSocks5Connection(
                proxy.host, proxy.port,
                targetUrl.host, targetUrl.port
            )

            // Ensure the socket remains open while measuring speeds
            proxySocket.use { socket ->
                socket.soTimeout = 10000 // 10s

                coroutineScope {
                    val uploadDeferred = async { measureUploadSpeed(socket, targetUrl) }
                    val downloadDeferred = async { measureDownloadSpeed(socket, targetUrl) }

                    // Await both results concurrently
                    val uploadSpeed = uploadDeferred.await()
                    val downloadSpeed = downloadDeferred.await()

                    return@coroutineScope uploadSpeed to downloadSpeed
                }
            }
        } catch (e: Exception) {
            Log.d(
                MainActivity.TAG,
                "Error measuring upload/download speeds for ${proxy.address()}; ${e.message}"
            )
            0.0 to 0.0
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun measureUploadSpeed(proxySocket: Socket, targetUrl: URI): Double {
        return withContext(Dispatchers.IO) {
            Log.d(
                MainActivity.TAG,
                "Measuring upload speed for proxy server ${proxySocket.remoteSocketAddress}"
            )

            val uploadEndpoint: URI = targetUrl.resolve("/api/speed/upload")
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
                .post(
                    b64EncodedData.toRequestBody(
                        "application/octet-stream".toMediaType(),
                    )
                )
                .build()

            val timeMs = measureTimeMillis {
                val inputStream = proxySocket.getInputStream()
                val outputStream = proxySocket.getOutputStream()

                val requestRawString = request.toRawString()
                Log.d(MainActivity.TAG, "Upload request body:\n$requestRawString")

                outputStream.write(requestRawString.toByteArray())
                outputStream.flush()

                try {
                    // Read response sent by proxy until server closes connection
                    val responseBytes = ByteArray(1 * 1024 * 1024) // 1MB
                    inputStream.read(responseBytes)

                } catch(e: IOException) {
                    Log.d(MainActivity.TAG, "Connection closed by proxy/server: ${e.message}")
                }
            }.toDouble()

            // Convert bytes to Megabytes and ms to seconds
            val megaBytes = (data.size * 8) / 1_000_000
            val timeSec = timeMs / 1000

            Log.d(MainActivity.TAG, "Uploaded $megaBytes MBs in $timeSec ms")
            megaBytes / timeSec
        }
    }

    private suspend fun measureDownloadSpeed(proxySocket: Socket, targetUrl: URI): Double {
        return withContext(Dispatchers.IO) {
            Log.d(
                MainActivity.TAG,
                "Measuring download speed for proxy server ${proxySocket.remoteSocketAddress}"
            )

            val downloadEndpoint: URI = targetUrl.resolve("/api/speed/download")
            val request = Request.Builder()
                .url(downloadEndpoint.toURL())
                .addHeader("Cookie", getCookies.joinToString("; ") { cookie ->
                    "${cookie.name}=${cookie.value}"
                })
                .addHeader("Authorization", "Bearer ${BuildConfig.ANDROID_API_KEY}")
                .addHeader("Accept-Encoding", "gzip")
                .build()

            val data = ByteArray(5 * 1024 * 1024) // 5MB

            val timeMs = measureTimeMillis {
                val inputStream = proxySocket.getInputStream()
                val outputStream = proxySocket.getOutputStream()

                outputStream.write(request.toRawString().toByteArray())
                outputStream.flush()

                // Read response sent by proxy
                inputStream.read(data)
            }.toDouble()

            // Convert bytes to Megabytes and ms to seconds
            val megaBytes = (data.size * 8) / 1_000_000
            val timeSec = timeMs / 1000

            Log.d(MainActivity.TAG, "Downloaded $megaBytes MBs in $timeSec ms")
            megaBytes / timeSec // Speed in MB/s
        }
    }
}
