package com.example.araknet.utils

import java.util.UUID

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