package com.example.araknet.data

import android.util.Log
import com.example.araknet.MainActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Packet(
    val protocol: Int,
    val destination: InetSocketAddress,
    val data: ByteArray,
)

interface Connection {
    val remoteAddress: SocketAddress

    suspend fun send(data: ByteArray, destination: InetSocketAddress?)
    suspend fun receive(buffer: ByteArray): Int
    fun isOpen(): Boolean
    fun close()
}

class TCPConnection(private val socket: Socket) : Connection {
    private val inputStream = socket.getInputStream()
    private val outputStream = socket.getOutputStream()
    override val remoteAddress: SocketAddress = socket.remoteSocketAddress

    override fun isOpen(): Boolean = !socket.isClosed

    override suspend fun send(data: ByteArray, destination: InetSocketAddress?) {
        Log.d(MainActivity.TAG, "Sending ${data.size} TCP bytes to $destination")
        outputStream.write(data)
        outputStream.flush()
    }

    override suspend fun receive(buffer: ByteArray): Int {
        val bytesRead = inputStream.read(buffer)
        Log.d(MainActivity.TAG, "Received $bytesRead TCP bytes from $remoteAddress")
        return bytesRead
    }

    override fun close() {
        socket.close()
    }
}

class UDPConnection(private val socket: DatagramSocket) : Connection {
    override val remoteAddress: SocketAddress = socket.remoteSocketAddress

    override fun isOpen(): Boolean = !socket.isClosed

    override suspend fun send(data: ByteArray, destination: InetSocketAddress?) {
        requireNotNull(destination) { "Sending UDP packet requires destination address" }

        val destIPBytes = destination.address.address
        val portBytes = ByteBuffer.allocate(2)
            .order(ByteOrder.BIG_ENDIAN)
            .putShort(destination.port.toShort())
            .array()
        val addressType: Byte = when (getAddressType(destination)) {
            "IPv4" -> 0x01
            "IPv6" -> 0x04
            "Domain" -> 0x03
            else -> throw IllegalArgumentException("Invalid address type ${destination.address}")
        }

        val payload = byteArrayOf(
            0x00, 0x00,             // RSV
            0x00,                   // FRAG (0x00 No Fragmentation)
            addressType,            // ATYP
            *destIPBytes,
            *portBytes,
            *data
        )

        Log.d(MainActivity.TAG, "Sending ${payload.size} UDP bytes to $destination")
        val packet = DatagramPacket(payload, payload.size)
        socket.send(packet)
    }

    override suspend fun receive(buffer: ByteArray): Int {
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        Log.d(MainActivity.TAG, "Received ${packet.length} UDP bytes from $remoteAddress")
        return packet.length
    }

    override fun close() {
        Log.i(MainActivity.TAG, "Closing connection to $remoteAddress")
        socket.close()
    }
}
