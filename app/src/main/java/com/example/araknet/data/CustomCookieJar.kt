package com.example.araknet.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

fun getSecurePrefs(context: Context): SharedPreferences {
    return EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

class CustomCookieJar(val context: Context) : CookieJar {
    private val sharedPreferences = getSecurePrefs(context)

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookiesSet: Set<String>? = sharedPreferences.getStringSet(url.host, emptySet())
        return cookiesSet?.mapNotNull { Cookie.parse(url,it) } ?: listOf()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookiesSet = cookies.map { it.toString() }.toSet()
        sharedPreferences.edit().putStringSet(url.host, cookiesSet).apply()
    }

}