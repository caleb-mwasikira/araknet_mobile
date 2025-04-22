package com.example.araknet.data

import android.util.Log
import com.example.araknet.MainActivity
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress


fun isInternalAddress(inetSocketAddress: InetSocketAddress): Boolean {
    val ip = inetSocketAddress.address ?: return true // Null = treat as internal

    // Internal means local-only, no routing involved
    return ip.isAnyLocalAddress ||     // 0.0.0.0 or ::
            ip.isLoopbackAddress ||     // 127.0.0.1 or ::1
            ip.isLinkLocalAddress       // 169.254.x.x or fe80::/10 (interface-local)
}

/**
 * Extracts protocol and destination address from packet buffer
 */
fun extractPacketInfo(data: ByteArray): Packet? {
    if (data.isEmpty()) {
        Log.w(MainActivity.TAG, "Dropping empty packet")
        return null
    }

    val version = (data[0].toInt() shr 4) and 0x0F

    val packet: Packet = when (version) {
        4 -> extractIPv4PacketInfo(data)
        6 -> extractIPv6PacketInfo(data)
        else -> null
    } ?: run {
        Log.w(MainActivity.TAG, "Unsupported packet version $version")
        return null
    }

    val supportedProtocols = listOf(6, 17) // TCP, UDP
    return if (packet.protocol in supportedProtocols) {
        packet
    } else {
        Log.w(
            MainActivity.TAG,
            "Dropping packet: Unsupported protocol ${packet.protocol} - ${parseProtocol(packet.protocol)}"
        )
        null
    }
}

fun extractIPv4PacketInfo(packet: ByteArray): Packet? {
    if (packet.size < 20) return null

    val protocol = packet[9].toInt() and 0xFF
    val dstIpBytes = packet.copyOfRange(16, 20)
    val dstIp = InetAddress.getByAddress(dstIpBytes).hostAddress
    val dstPort = when (protocol) {
        6, 17 -> ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
        else -> return null
    }

    return Packet(
        protocol = protocol,
        destination = InetSocketAddress(dstIp, dstPort),
        data = packet,
    )
}

fun extractIPv6PacketInfo(packet: ByteArray): Packet? {
    if (packet.size < 40) return null

    var protocol = packet[6].toInt() and 0xFF
    var offset = 40 // After fixed IPv6 header

    // List of known extension header types
    val extensionHeaders = setOf(
        0,   // Hop-by-Hop Options
        43,  // Routing
        44,  // Fragment
        60,  // Destination Options
        51,  // Authentication Header
        50   // Encapsulating Security Payload (ESP) — special handling usually
    )

    while (extensionHeaders.contains(protocol)) {
        if (offset + 2 > packet.size) return null

        val headerType = protocol
        protocol = packet[offset].toInt() and 0xFF

        val headerLength = when (headerType) {
            44 -> 8 // Fragment header is always 8 bytes
            else -> (packet[offset + 1].toInt() and 0xFF + 1) * 8
        }

        offset += headerLength
        if (offset > packet.size) return null
    }

    if (packet.size < offset + 4) return null

    val dstIpBytes = packet.copyOfRange(24, 40)
    val dstIp = InetAddress.getByAddress(dstIpBytes).hostAddress
    val dstPort = ((packet[offset + 2].toInt() and 0xFF) shl 8) or
            (packet[offset + 3].toInt() and 0xFF)

    return Packet(
        protocol = protocol,
        destination = InetSocketAddress(dstIp, dstPort),
        data = packet,
    )
}

fun parseProtocol(protocol: Int): String {
    return when (protocol) {
        1 -> "ICMP"
        2 -> "IGMP"
        6 -> "TCP"
        17 -> "UDP"
        41 -> "IPv6"
        47 -> "GRE"
        50 -> "ESP (Encrypted Security Payload)"
        51 -> "AH (Authentication Header)"
        58 -> "ICMPv6"
        88 -> "EIGRP"
        89 -> "OSPF"
        132 -> "SCTP"
        else -> "Unknown ($protocol)"
    }
}

fun getAddressType(addr: InetSocketAddress): String {
    val inetAddress = addr.address

    return when {
        inetAddress != null -> {
            when (inetAddress) {
                is Inet4Address -> "IPv4"
                is Inet6Address -> "IPv6"
                else -> throw IllegalArgumentException("Unknown IP type")
            }
        }

        else -> {
            // If address is null, it means a domain name is used and hasn't been resolved yet
            "Domain"
        }
    }
}
