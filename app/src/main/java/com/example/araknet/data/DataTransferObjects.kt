package com.example.araknet.data

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