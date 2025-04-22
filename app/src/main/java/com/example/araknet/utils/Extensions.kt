package com.example.araknet.utils

import okhttp3.Request
import okio.Buffer


fun String.titlecase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
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
