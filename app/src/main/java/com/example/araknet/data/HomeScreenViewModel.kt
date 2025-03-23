package com.example.araknet.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.araknet.MainActivity
import com.example.araknet.utils.shortString
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.UUID


data class ProxyServer(
    val id: String,
    val protocol: String,
    val address: String,
    val ipInfo: IPInfo?,
    var status: ProxyStatus = ProxyStatus.Offline,
    var ping: Int = 0,
    var downloadSpeed: Float = 0f,
    var uploadSpeed: Float = 0f,
    var isConnected: Boolean = false,
)


class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val api: ApiService? = MainActivity.retrofitService
    }

    private val _proxyServers = MutableStateFlow<List<ProxyServer>>(listOf())
    val proxyServers = _proxyServers.asStateFlow()

    private val _currentIndex = MutableStateFlow<Int?>(null)
    val currentIndex = _currentIndex.asStateFlow()

    private val _errors = MutableStateFlow<Error?>(null)
    val errors
        get() = _errors.asStateFlow()

    init {
        viewModelScope.launch {
            getProxyServers()
            _currentIndex.value = if (_proxyServers.value.isEmpty()) null else 0
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

    suspend fun getProxyServers() {
        Log.d(MainActivity.TAG, "Fetching proxy servers")

        try {
            val response = api?.getProxies() ?: run {
                Log.d(MainActivity.TAG, "Null retrofitService")
                return
            }
            val proxyServers: List<ProxyDto> = extractResponseData(response) ?: run {
                Log.d(MainActivity.TAG, "Null proxyServers")
                return
            }
            _proxyServers.value = proxyServers.map { proxy ->
                val proxyId = UUID.randomUUID().shortString()

                ProxyServer(
                    id = proxyId, protocol = proxy.protocol,
                    address = proxy.address, ipInfo = proxy.ipInfo,
                )
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

    fun nextProxyServer() {
        // circularly add current proxy index
        _currentIndex.value = (_currentIndex.value?.plus(1))?.rem(_proxyServers.value.size)
    }

    suspend fun testProxyConnection(id: UUID): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun closeProxyConnection(id: UUID): Boolean {
        TODO("Not yet implemented")
    }
}
