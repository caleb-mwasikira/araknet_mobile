package com.example.araknet.utils

import android.os.Build
import androidx.annotation.RequiresApi
import okhttp3.Request
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.GZIPOutputStream


fun String.titlecase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}

fun UUID.shortString(): String {
    return this.toString().take(13)
}

fun Request.toRawString(): String {
    val url = this.url
    val params = url.encodedQuery?.let { "?${url.encodedQuery}" } ?: ""
    val path = "${url.encodedPath}$params"
    val headers = this.headers.joinToString("\r\n") { (key, value) -> "$key: $value" }

    val bodyString = this.body?.let { body ->
        val buffer = Buffer()
        body.writeTo(buffer)
        buffer.readUtf8()
    } ?: ""

    // Build raw request
    return buildString {
        append("${this@toRawString.method} $path HTTP/1.1\r\n")
        append("Host: ${url.host}\r\n")
        append("$headers\r\n")
        if (bodyString.isNotEmpty()) {
            append("Content-Length: ${bodyString.toByteArray().size}")
        }
        append("\r\n\r\n") // End headers
        append(bodyString)
    }
}
