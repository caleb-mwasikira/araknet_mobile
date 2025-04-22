package com.example.araknet.data

import android.util.Log
import com.example.araknet.BuildConfig
import com.example.araknet.MainActivity
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SOCKSException(override val message: String) : Exception()

object SOCKS5 {
    fun newTCPConnection(
        proxyHost: String, proxyPort: Int,
        destHost: String, destPort: Int,
    ): Socket {
        val proxyAddress = InetSocketAddress(proxyHost, proxyPort)
        val destAddress = InetSocketAddress(destHost, destPort)

        return newTCPProxyConnection(proxyAddress, destAddress)
    }

    fun newUDPConnection(
        proxyHost: String, proxyPort: Int,
        destHost: String, destPort: Int,
    ): DatagramSocket {
        val proxyAddress = InetSocketAddress(proxyHost, proxyPort)
        val destAddress = InetSocketAddress(destHost, destPort)

        return newUDPProxyConnection(proxyAddress, destAddress)
    }

    private fun connectToSOCKS5Proxy(proxyAddress: InetSocketAddress): Socket {
        Log.d(MainActivity.TAG, "Connecting to SOCKS5 proxy $proxyAddress")

        val proxySocket = Socket()
        proxySocket.connect(proxyAddress)

        val proxyInputStream: InputStream = proxySocket.getInputStream()
        val proxyOutputStream: OutputStream = proxySocket.getOutputStream()

        // Send handshake request
        proxyOutputStream.write(
            byteArrayOf(
                0x05,
                0x01,
                0x02
            )
        ) // SOCKS5, 1 method, username/password
        proxyOutputStream.flush()

        // Read handshake response
        val response = ByteArray(2)
        proxyInputStream.read(response)

        if (response[1] != 0x02.toByte()) { // SOCKS5 proxy does NOT require authentication
            return proxySocket
        }

        // SOCKS5 proxy requires authentication
        val userBytes = "john".toByteArray()
        val passBytes = "password1234".toByteArray()
        proxyOutputStream.write(
            byteArrayOf(0x05, userBytes.size.toByte()) + userBytes + byteArrayOf(
                passBytes.size.toByte()
            ) + passBytes
        )
        proxyOutputStream.flush()

        response.fill(0) // Clear previous response data

        // Read authentication response
        proxyInputStream.read(response)
        if (response[1] != 0x00.toByte()) {
            Log.d(MainActivity.TAG, "Authentication failed")
            throw SOCKSException("SOCKS5 authentication failed")
        }

        Log.i(
            MainActivity.TAG,
            "Successfully connected to SOCKS5 proxy at ${proxySocket.remoteSocketAddress}"
        )
        return proxySocket
    }

    private fun newTCPProxyConnection(
        proxyAddress: InetSocketAddress,
        destAddress: InetSocketAddress,
    ): Socket {
        val proxySocket = connectToSOCKS5Proxy(proxyAddress)

        val proxyInputStream = proxySocket.getInputStream()
        val proxyOutputStream = proxySocket.getOutputStream()

        Log.d(MainActivity.TAG, "Sending CONNECT request from $proxyAddress -> $destAddress")

        // Send connect request
        val destIPBytes = destAddress.address.address
        val destPortBytes = ByteBuffer.allocate(2)
            .order(ByteOrder.BIG_ENDIAN)
            .putShort(destAddress.port.toShort())
            .array()
        val addressType: Byte = when (getAddressType(destAddress)) {
            "IPv4" -> 0x01
            "IPv6" -> 0x04
            "Domain" -> 0x03
            else -> throw IllegalArgumentException("Invalid address type ${destAddress.address}")
        }

        proxyOutputStream.write(
            byteArrayOf(
                0x05,           // VER
                0x01,           // CMD (0x01 Connect)
                0x00,           // RSV
                addressType,    // ATYP
                *destIPBytes,
                *destPortBytes
            )
        )
        proxyOutputStream.flush()

        // Read connect response
        val response = ByteArray(3)
        proxyInputStream.read(response)

        // success = 0x00, failure = 0x01
        if (response[1] != 0x00.toByte()) {
            throw SOCKSException("SOCKS5 connect request failed")
        }

        Log.d(
            MainActivity.TAG,
            "TCP tunnel created successfully $proxyAddress -> $destAddress"
        )
        return proxySocket
    }

    private fun newUDPProxyConnection(
        proxyAddress: InetSocketAddress,
        destAddress: InetSocketAddress
    ): DatagramSocket {
        val socket = connectToSOCKS5Proxy(proxyAddress)

        val proxyInputStream = socket.getInputStream()
        val proxyOutputStream = socket.getOutputStream()

        Log.d(MainActivity.TAG, "Sending UDP Associate request from $proxyAddress -> $destAddress")

        // Send UDP Associate request
        proxyOutputStream.write(
            byteArrayOf(
                0x05,   // VER
                0x03,   // CMD (UDP ASSOCIATE)
                0x00,   // RSV
                0x01,   // ATYP (IPv4)
            )
        )
        proxyOutputStream.flush()

        // Read UDP Associate response
        val response = ByteArray(10)
        proxyInputStream.read(response)

        if (response[1] != 0x00.toByte()) {
            throw SOCKSException("UDP ASSOCIATE request failed")
        }

        // Extract UDP relay address returned by the proxy server
        val addressType = response[3]
        var udpRelayAddress: InetSocketAddress = when (addressType) {
            0x01.toByte() -> {  // IPv4
                val ipBytes = response.copyOfRange(4, 8)
                val ipv4 = InetAddress.getByAddress(ipBytes)

                val portBytes = response.copyOfRange(8, response.size)
                val port: Int = ByteBuffer.wrap(portBytes)
                    .order(ByteOrder.BIG_ENDIAN)
                    .short.toInt() and 0xFFFF // Unsigned short

                InetSocketAddress(ipv4, port)
            }

            else -> {
                throw SOCKSException("Unsupported address type")
            }
        }

        // Using static IP address as UDP relay address
        // TODO: Change to dynamic UDP relay address returned by proxy during production
        val remoteURL = URI(BuildConfig.REMOTE_URL)
        udpRelayAddress = InetSocketAddress(remoteURL.host, udpRelayAddress.port)

        Log.d(
            MainActivity.TAG,
            "UDP relay address created successfully $udpRelayAddress -> $destAddress"
        )

        // Set destination address for UDP socket
        val udpSocket = DatagramSocket()
        udpSocket.connect(udpRelayAddress)
        return udpSocket
    }
}
