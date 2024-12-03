package com.example.araknet.data

import java.util.UUID

data class ProxyServer(
    val id: UUID,
    val country: String,
    val city: String,
    var status: ProxyStatus,
    var ping: Int,
    var downloadSpeed: Float = 0f,
    var uploadSpeed: Float = 0f,
    var isConnected: Boolean = false,
)
