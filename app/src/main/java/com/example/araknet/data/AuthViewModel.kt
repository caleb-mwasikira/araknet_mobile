package com.example.araknet.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.araknet.utils.titlecase
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import java.io.IOException

sealed class AuthError {
    abstract val errMessage: String

    data class FieldError(val fieldName: String, override val errMessage: String) : AuthError()
    data class ConnectionError(
        override val errMessage: String = "Connection Error. Please check your internet connection and try again"
    ) : AuthError()

    data class ServerError(override val errMessage: String) : AuthError()
}

enum class AuthState {
    Idle, Loading, Error, Success
}

class AuthViewModel : ViewModel() {
    companion object {
        const val TAG = "AuthViewModel"
        private const val MIN_NAME_LENGTH = 3
        private const val MIN_PASSWORD_LENGTH = 6
    }

    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var agreedToTerms by mutableStateOf(false)

    var authState by mutableStateOf(AuthState.Idle)
        private set

    private var authErrors by mutableStateOf(listOf<AuthError>())
    val fieldErrors
        get() = authErrors.filterIsInstance<AuthError.FieldError>()
    val connectionErrors
        get() = authErrors.filterIsInstance<AuthError.ConnectionError>()
    val serverErrors
        get() = authErrors.filterIsInstance<AuthError.ServerError>()

    fun loginUser() {
        authErrors = validateLogin()
        if (authErrors.isNotEmpty()) {
            return
        }

        Log.d(TAG, "Attempting user login")
        authState = AuthState.Loading

        runBlocking {
            try {
                val credentials = LoginCredentials(email = email, password = password)
                val httpResponse = Api.retrofitService.loginUser(credentials)

                val responseBody: LoginResponse = httpResponse.body() ?: let {
                    val jsonResponse = httpResponse.errorBody()?.string()
                    if (jsonResponse == null) {
                        LoginResponse("Internal Server Error")
                    }

                    val gsonBuilder = GsonBuilder().create()
                    val loginResponse = gsonBuilder.fromJson(jsonResponse, LoginResponse::class.java)
                    loginResponse
                }
                Log.d(TAG, "Login server response; $httpResponse\nBody: $responseBody")

                if (httpResponse.isSuccessful) {
                    Log.d(TAG, "Login success")
                    authState = AuthState.Success

                    // TODO: save jwt received from server in local storage

                } else {
                    // Handle unsuccessful responses
                    Log.d(TAG, "Login failed")
                    authState = AuthState.Error

                    authErrors = listOf(
                        AuthError.ServerError(responseBody.message)
                    )
                }
            } catch (e: IOException) {
                // Handle connection errors
                Log.d(TAG, "Login failed; Connection error; ${e.message}")
                authState = AuthState.Error
                authErrors = listOf(AuthError.ConnectionError())
            }
        }

        Log.d(TAG, "Auth errors; $authErrors")
    }

    fun registerUser() {
        authErrors = validateRegister()

        if (authErrors.isNotEmpty()) {
            return
        }

        // attempt user registration
        Log.d(TAG, "Attempting user registration")
        authState = AuthState.Loading

        runBlocking {
            try {
                val credentials = RegisterCredentials(
                    username = username,
                    email = email,
                    password = password
                )
                val httpResponse = Api.retrofitService.registerUser(credentials)
                val responseBody: RegisterResponse = httpResponse.body() ?: let {
                    val jsonResponse = httpResponse.errorBody()?.string()
                        ?: return@let RegisterResponse("Internal Server Error")

                    val gsonBuilder = GsonBuilder().create()
                    val registerResponse = gsonBuilder.fromJson(jsonResponse, RegisterResponse::class.java)
                    registerResponse
                }
                Log.d(TAG, "Register server response; $httpResponse\nBody: $responseBody")

                if (httpResponse.isSuccessful) {
                    Log.d(TAG, "Registration success")
                    authState = AuthState.Success

                } else {
                    // Handle unsuccessful responses
                    Log.d(TAG, "Registration failed")
                    authState = AuthState.Error
                    authErrors = listOf(
                        AuthError.ServerError(responseBody.message)
                    )
                }

            } catch (e: IOException) {
                // Handle connection errors
                Log.d(TAG, "Registration failed; Connection error; ${e.message}")
                authState = AuthState.Error
                authErrors = listOf(AuthError.ConnectionError())
            }

        }

        Log.d(TAG, "Auth errors; $authErrors")
    }

    private fun validateLogin(): MutableList<AuthError> {
        return buildList {
            addAll(validateEmail())
            addAll(validatePassword())
        }.toMutableList()
    }

    private fun validateRegister(): MutableList<AuthError> {
        return buildList {
            addAll(validateString("username", username))
            addAll(validateEmail())
            addAll(validatePassword())

            if (!agreedToTerms) {
                add(
                    AuthError.FieldError(
                        "terms",
                        "You must agree to terms and conditions before continuing"
                    )
                )
            }
        }.toMutableList()
    }

    private fun validateEmail(): List<AuthError> {
        val emailErrors = mutableListOf<AuthError>()

        if (email.isEmpty()) {
            emailErrors.add(
                AuthError.FieldError("email", "Email cannot be empty")
            )
        }

        val emailRegex = Regex("[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}")
        if (!emailRegex.matches(email)) {
            emailErrors.add(
                AuthError.FieldError("email", "Invalid email format")
            )
        }
        return emailErrors
    }

    private fun validatePassword(): List<AuthError> {
        val passwordErrors = mutableListOf<AuthError>()

        if (password.length < MIN_PASSWORD_LENGTH) {
            passwordErrors.add(
                AuthError.FieldError(
                    "password",
                    "Password cannot be less than $MIN_PASSWORD_LENGTH characters long"
                )
            )
        }

        // TODO: check password strength

        return passwordErrors
    }

    private fun validateString(fieldName: String, value: String): List<AuthError> {
        val errors = mutableListOf<AuthError>()

        if (value.length < MIN_NAME_LENGTH) {
            errors.add(
                AuthError.FieldError(
                    fieldName,
                    "${fieldName.titlecase()} cannot be less than $MIN_NAME_LENGTH characters long"
                )
            )
        }
        return errors
    }

}