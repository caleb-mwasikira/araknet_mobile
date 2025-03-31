package com.example.araknet.data

import androidx.compose.ui.graphics.Color
import com.example.araknet.ui.theme.connectingColor
import com.example.araknet.ui.theme.connectingSecondaryColor
import com.example.araknet.ui.theme.offlineColor
import com.example.araknet.ui.theme.offlineSecondaryColor
import com.example.araknet.ui.theme.onlineColor
import com.example.araknet.ui.theme.onlineSecondaryColor
import com.google.gson.annotations.SerializedName

data class LoginDto(
    val email: String,
    val password: String
)

data class ApiResponse<T>(
    val message: String,
    val data: T? = null,
    val errors: Map<String, String>? = null
)

data class RegisterDto(
    val username: String,
    val email: String,
    val password: String,
)

data class EmailDto(
    val email: String,
)

data class PasswordResetDto(
    @SerializedName("password_reset_token") val passwordResetToken: String,
    val email: String,
    @SerializedName("new_password") val newPassword: String,
)

data class IPInfo(
    val query: String,
    val status: String,
    val country: String,
    val countryCode: String,
    val city: String,
)

enum class ProxyStatus(
    val primaryColor: Color,
    val secondaryColor: Color
) {
    Online(onlineColor, onlineSecondaryColor),
    Offline(offlineColor, offlineSecondaryColor),
    Connecting(connectingColor, connectingSecondaryColor),
}

data class ProxyDto(
    val id: String,
    val protocol: String,
    var host: String,
    val port: Int,
    @SerializedName("ip_info") val ipInfo: IPInfo?,
)

fun ProxyDto.address(): String {
    return "${this.host}:${this.port}"
}
