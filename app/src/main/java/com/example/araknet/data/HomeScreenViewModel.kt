package com.example.araknet.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.araknet.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _proxyServers = MutableStateFlow<List<ProxyServer>>(listOf())
    val proxyServers = _proxyServers.asStateFlow()

    private val _currentIndex = MutableStateFlow<Int?>(null)
    val currentIndex = _currentIndex.asStateFlow()

    companion object {
        const val TAG: String = "HomeScreenViewModel"
    }

    init {
        _proxyServers.value = getProxyServers(5).toMutableList()
        _currentIndex.value = if (_proxyServers.value.isEmpty()) null else 0

        Log.d(TAG, "proxy servers: ${_proxyServers.value}")
    }

    private fun getCountries(): List<Country> {
        val context = getApplication<Application>().applicationContext
        val countryNames = context.resources.getStringArray(R.array.country_names)
        val countryCodes = context.resources.getStringArray(R.array.country_codes)

        return countryNames.zip(countryCodes)
            .map { pair ->
                val flagResId = getFlagResId(pair.second)

                Country(
                    name = pair.first,
                    code = pair.second,
                    flag = flagResId,
                )
            }
    }

    private fun getFlagResId(countryCode: String): Int {
        val context = getApplication<Application>().applicationContext
        val resName = countryCode.lowercase()
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        return resId
    }

    private fun getProxyServers(n: Int): List<ProxyServer> {
        val proxyServers = mutableListOf<ProxyServer>()
        val countries = getCountries()
        val proxyStatuses =
            listOf(ProxyStatus.Online(), ProxyStatus.Offline(), ProxyStatus.Connecting())

        for (i in 0..<n) {
            val randomCountry = countries.random()
            val randomProxyStatus = proxyStatuses[Random.nextInt(proxyStatuses.indices)]
            val randomPing = Random.nextInt(15, 300)

            val maxSpeed = Random.nextInt(500, 1000) // 500 - 1000 Mbps
            val minSpeed = Random.nextInt(0, 50) // 0 - 50 Mbps

            val proxyServer = ProxyServer(
                id = UUID.randomUUID(),
                country = randomCountry,
                status = randomProxyStatus,
                ping = randomPing,
                downloadSpeed = minSpeed + (Random.nextFloat() * maxSpeed),
                uploadSpeed = minSpeed + (Random.nextFloat() * maxSpeed),
                isConnected = randomProxyStatus is ProxyStatus.Online && Random.nextBoolean()
            )
            proxyServers.add(proxyServer)
        }
        return proxyServers
    }

    fun nextProxyServer() {
        // circularly add current proxy index
        _currentIndex.value = (_currentIndex.value?.plus(1))?.rem(_proxyServers.value.size)
    }

    suspend fun testProxyConnection(id: UUID): Boolean {
        Log.d(TAG, "testing connection on proxy server: $id")
        delay(500) // simulate busy work; eg testing network connection

        val isSuccessful = Random.nextBoolean()
        if (isSuccessful) {
            // update connection status of proxy server
            _proxyServers.value = _proxyServers.value.map { proxyServer ->
                if (proxyServer.id == id) {
                    proxyServer.copy(
                        status = ProxyStatus.Online(),
                        isConnected = true
                    )
                } else {
                    proxyServer
                }
            }
            return true
        }
        return false
    }

    suspend fun closeProxyConnection(id: UUID): Boolean {
        Log.d(TAG, "closing proxy connection on server: $id")
        delay(500) // simulate busy work; eg closing network connection

        val isSuccessful = Random.nextBoolean()
        if (isSuccessful) {
            // update connection status of proxy server
            _proxyServers.value = _proxyServers.value.map { proxyServer ->
                if (proxyServer.id == id) {
                    proxyServer.copy(
                        status = ProxyStatus.Offline(),
                        isConnected = false
                    )
                } else {
                    proxyServer
                }
            }
            return true
        }
        return false
    }
}
