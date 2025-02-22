package com.example.araknet.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.araknet.MainActivity
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class AuthError {
    abstract val errMessage: String

    data class ValidationError(val fieldName: String, override val errMessage: String) : AuthError()
    data class ConnectionError(
        override val errMessage: String = "Server currently unavailable. Please try again later"
    ) : AuthError()

    data class ServerError(override val errMessage: String) : AuthError()
}

enum class AuthState {
    Idle, Loading, Error, Success
}

class AuthViewModel : ViewModel() {
    companion object {
        private const val MIN_NAME_LENGTH = 3
        private const val MIN_PASSWORD_LENGTH = 6
    }

    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var agreedToTerms by mutableStateOf(false)

    var authState = MutableStateFlow(AuthState.Idle)
        private set

    private val _authErrors = MutableSharedFlow<AuthError>()
    val authErrors = _authErrors.asSharedFlow()

    // TODO: check internet connection before sending auth requests

    fun loginUser() = viewModelScope.launch {
        Log.d(MainActivity.TAG, "Attempting user login")

        // Validate login form
        val loginErrors: List<AuthError> = buildList {
            add(validateEmail(email))
            add(validatePassword(password))
        }.filterNotNull()

        if (loginErrors.isNotEmpty()) {
            Log.d(MainActivity.TAG, "Login failed. Validation errors; $loginErrors")
            loginErrors
                .reversed() // Reversing this list so that the topmost field error is shown first
                .forEach { error -> _authErrors.emit(error) }
            return@launch
        }

        authState.value = AuthState.Loading

        try {
            val credentials = LoginCredentials(email = email, password = password)
            val httpResponse = Api.retrofitService.loginUser(credentials)

            val responseBody: LoginResponse = httpResponse.body() ?: let {
                val jsonResponse = httpResponse.errorBody()?.string()
                    ?: return@let LoginResponse("Internal Server Error")

                val gsonBuilder = GsonBuilder().create()
                val loginResponse = gsonBuilder.fromJson(jsonResponse, LoginResponse::class.java)
                loginResponse
            }

            Log.d(MainActivity.TAG, "Login HTTP response; $httpResponse")
            Log.d(MainActivity.TAG, "Login HTTP body; $responseBody")

            if (httpResponse.isSuccessful) {
                Log.d(MainActivity.TAG, "Login success")
                authState.value = AuthState.Success

                // TODO: save jwt received from server in local storage

            } else {
                // Handle unsuccessful responses
                Log.d(MainActivity.TAG, "Login failed")
                authState.value = AuthState.Error
                _authErrors.emit(
                    AuthError.ServerError(responseBody.message)
                )
            }
        } catch (e: IOException) {
            // Handle connection errors
            Log.d(MainActivity.TAG, "Login failed. Connection error; ${e.message}")
            authState.value = AuthState.Error
            _authErrors.emit(
                AuthError.ConnectionError()
            )
        }
    }

    fun registerUser() = viewModelScope.launch {
        Log.d(MainActivity.TAG, "Attempting user registration")

        // Validate register form
        val registrationErrors: List<AuthError> = buildList {
            add(validateUsername(username))
            add(validateEmail(email))
            add(validatePassword(password))

            if (!agreedToTerms) {
                add(
                    AuthError.ValidationError(
                        "terms", "You must agree to terms and conditions before continuing"
                    )
                )
            }
        }.filterNotNull()

        if (registrationErrors.isNotEmpty()) {
            Log.d(MainActivity.TAG, "Registration failed. Validation errors; $registrationErrors")
            registrationErrors
                .reversed() // Reversing this list so that the topmost field error is shown first
                .forEach { error -> _authErrors.emit(error) }
            return@launch
        }

        authState.value = AuthState.Loading

        try {
            val credentials = RegisterCredentials(
                username = username, email = email, password = password
            )
            val httpResponse = Api.retrofitService.registerUser(credentials)
            val responseBody: RegisterResponse = httpResponse.body() ?: let {
                val jsonResponse = httpResponse.errorBody()?.string()
                    ?: return@let RegisterResponse("Internal Server Error")

                val gsonBuilder = GsonBuilder().create()
                val registerResponse =
                    gsonBuilder.fromJson(jsonResponse, RegisterResponse::class.java)
                registerResponse
            }

            Log.d(MainActivity.TAG, "Register HTTP response; $httpResponse")
            Log.d(MainActivity.TAG, "Register HTTP body; $responseBody")

            if (httpResponse.isSuccessful) {
                Log.d(MainActivity.TAG, "Registration success")
                authState.value = AuthState.Success

            } else {
                // Handle unsuccessful responses
                Log.d(MainActivity.TAG, "Registration failed")
                authState.value = AuthState.Error
                _authErrors.emit(
                    AuthError.ServerError(responseBody.message)
                )
            }

        } catch (e: IOException) {
            // Handle connection errors
            Log.d(MainActivity.TAG, "Registration failed. Connection error; ${e.message}")
            authState.value = AuthState.Error
            _authErrors.emit(
                AuthError.ConnectionError()
            )
        }
    }

    private fun validateEmail(email: String): AuthError? {
        if (email.isEmpty()) {
            return AuthError.ValidationError("email", "Email cannot be empty")
        }

        val emailRegex = Regex("[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}")
        if (!emailRegex.matches(email)) {
            return AuthError.ValidationError("email", "Invalid email format")

        }
        return null
    }

    private fun validatePassword(password: String): AuthError? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return AuthError.ValidationError(
                "password", "Password cannot be less than $MIN_PASSWORD_LENGTH characters long"
            )
        }

        // TODO: check password strength
        return null
    }

    private fun validateUsername(username: String): AuthError? {
        if (username.length < MIN_NAME_LENGTH) {
            return AuthError.ValidationError(
                "username",
                "Username too short. Minimum of $MIN_NAME_LENGTH characters acceptable"
            )
        }
        return null
    }

}