package com.example.araknet.data

import android.util.Log
import com.example.araknet.MainActivity
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SOCKSException(override val message: String) : Exception()

fun newSocks5Connection(
    proxyHost: String, proxyPort: Int,
    targetHost: String, targetPort: Int,
): Socket {
    Log.d(MainActivity.TAG, "Connecting to SOCKS5 proxy $proxyHost:$proxyPort")

    val socket = Socket()
    socket.connect(InetSocketAddress(proxyHost, proxyPort))

    val inputStream: InputStream = socket.getInputStream()
    val outputStream: OutputStream = socket.getOutputStream()

    // Send handshake request
    outputStream.write(byteArrayOf(0x05, 0x01, 0x02)) // SOCKS5, 1 method, username/password
    outputStream.flush()

    // Read server response
    var response = ByteArray(2)
    inputStream.read(response)

    Log.d(MainActivity.TAG, "Handshake successful")

    if (response[1] == 0x02.toByte()) {
        // Send authentication credentials
        Log.d(MainActivity.TAG, "Sending authentication request")

        val userBytes = "john".toByteArray()
        val passBytes = "password1234".toByteArray()
        outputStream.write(
            byteArrayOf(0x05, userBytes.size.toByte()) + userBytes + byteArrayOf(
                passBytes.size.toByte()
            ) + passBytes
        )
        outputStream.flush()

        // Read authentication response
        inputStream.read(response)
        if (response[1] != 0x00.toByte()) {
            Log.d(MainActivity.TAG, "Authentication failed")
            throw SOCKSException("SOCKS5 authentication failed")
        }

        Log.d(MainActivity.TAG, "Authentication successful")
    }

    Log.d(MainActivity.TAG, "Sending CONNECT request")

    // Send connect request
    val targetHostBytes = InetAddress.getByName(targetHost).address
    val targetPortBytes = ByteBuffer.allocate(2)
        .order(ByteOrder.BIG_ENDIAN)
        .putShort(targetPort.toShort())
        .array()
    outputStream.write(
        byteArrayOf(
            0x05,
            0x01,
            0x00,
            0x01
        ) // SOCKS5 version, CONNECT command, reserved byte, address type
                + targetHostBytes
                + targetPortBytes
    )
    outputStream.flush()

    // Read connect response
    response = ByteArray(3)
    inputStream.read(response)

    // success = 0x00, failure = 0x01
    if (response[1] != 0x00.toByte()) {
        Log.d(MainActivity.TAG, "Connect request failed")
        throw SOCKSException("SOCKS5 connect request failed")
    }

    Log.d(
        MainActivity.TAG,
        "SOCKS5 connection established with $targetHost:$targetPort. Can now begin sending raw data"
    )
    return socket
}
