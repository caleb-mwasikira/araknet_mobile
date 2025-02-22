package com.example.araknet.data

import androidx.annotation.DrawableRes
import java.util.UUID

data class Country(
    val name: String,
    val code: String? = null,
    @DrawableRes val flag: Int
)

data class ProxyServer(
    val id: UUID,
    val country: Country,
    val city: String? = null,
    var status: ProxyStatus,
    var ping: Int,
    var downloadSpeed: Float = 0f,
    var uploadSpeed: Float = 0f,
    var isConnected: Boolean = false,
)
