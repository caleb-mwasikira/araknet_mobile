package com.example.araknet.utils

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

fun compressData(data: ByteArray): ByteArray {
    val outputStream = ByteArrayOutputStream()
    GZIPOutputStream(outputStream).use { gzipOutputStream ->
        gzipOutputStream.write(data)
        gzipOutputStream.flush()
    }

    return outputStream.toByteArray()
}