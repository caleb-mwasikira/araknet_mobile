package com.example.araknet.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.araknet.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.time.Duration

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        // Add request headers to request
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.ANDROID_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(newRequest)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getRetrofitBuilder(context: Context): Retrofit {
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .cookieJar(CustomCookieJar(context))
        .callTimeout(Duration.ofSeconds(30))
        .build()

    return Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BuildConfig.REMOTE_URL)
        .client(client)
        .build()
}

interface ApiService {
    @POST("/api/login")
    suspend fun loginUser(@Body loginRequest: LoginDto): Response<ApiResponse<Any>>

    @POST("/api/register")
    suspend fun registerUser(@Body registerResponse: RegisterDto): Response<ApiResponse<Any>>

    @POST("/api/verify-auth")
    suspend fun verifyAuth(): Response<ApiResponse<Any>>

    @POST("/api/forgot-password")
    suspend fun forgotPassword(@Body email: EmailDto): Response<ApiResponse<Any>>

    @POST("/api/reset-password")
    suspend fun resetPassword(@Body passwordResetDto: PasswordResetDto): Response<ApiResponse<Any>>
}
