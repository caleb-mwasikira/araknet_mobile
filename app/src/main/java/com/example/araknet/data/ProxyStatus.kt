package com.example.araknet.data

import androidx.compose.ui.graphics.Color
import com.example.araknet.ui.theme.connectingColor
import com.example.araknet.ui.theme.connectingSecondaryColor
import com.example.araknet.ui.theme.offlineColor
import com.example.araknet.ui.theme.offlineSecondaryColor
import com.example.araknet.ui.theme.onlineColor
import com.example.araknet.ui.theme.onlineSecondaryColor

sealed class ProxyStatus {
    abstract val primaryColor: Color
    abstract val secondaryColor: Color
    abstract override fun toString(): String

    class Online(
        override val primaryColor: Color = onlineColor,
        override val secondaryColor: Color = onlineSecondaryColor,
    ): ProxyStatus() {
        override fun toString(): String {
            return "(Online)"
        }
    }

    class Offline(
        override val primaryColor: Color = offlineColor,
        override val secondaryColor: Color = offlineSecondaryColor,
    ): ProxyStatus() {
        override fun toString(): String {
            return "(Offline)"
        }
    }

    class Connecting(
        override val primaryColor: Color = connectingColor,
        override val secondaryColor: Color = connectingSecondaryColor,
    ): ProxyStatus() {
        override fun toString(): String {
           return "(Connecting)"
        }
    }
}
