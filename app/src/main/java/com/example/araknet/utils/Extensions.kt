package com.example.araknet.utils

import java.util.UUID

fun String.titlecase(): String {
    return this.replaceFirstChar { char -> char.titlecaseChar() }
}

fun UUID.shortString(): String {
    return this.toString().take(13)
}