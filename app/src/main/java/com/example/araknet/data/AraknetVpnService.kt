package com.example.araknet.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.araknet.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

@RequiresApi(Build.VERSION_CODES.O)
class AraknetVpnService : VpnService() {
    private val channelID = "vpn_service_channel"
    private val notificationID = 1

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnInputStream: FileInputStream? = null
    private var vpnOutputStream: FileOutputStream? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val internalConnections = ConcurrentHashMap<InetSocketAddress, Connection>()
    private val externalConnections = ConcurrentHashMap<InetSocketAddress, Connection>()
    private val destChannels = ConcurrentHashMap<InetSocketAddress, Channel<Packet>>()

    private var selectedProxy: InetSocketAddress? = null

    private val proxyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == HomeScreenViewModel.SELECT_PROXY_ACTION) {
                val broadcastMessage: String? = intent.getStringExtra("proxy_address")
                broadcastMessage?.let {
                    Log.d(MainActivity.TAG, "BROADCAST: New proxy server selected $it")

                    val proxyUri = URI(it)
                    val newProxySocketAddress = InetSocketAddress(proxyUri.host, proxyUri.port)

                    if (selectedProxy != newProxySocketAddress) {
                        selectedProxy = newProxySocketAddress
                        restartVpn()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        Log.i(MainActivity.TAG, "Creating VPN service")

        // Register broadcast receiver; for sending messages from
        // the UI layer to the VPN service
        val filter = IntentFilter(HomeScreenViewModel.SELECT_PROXY_ACTION)
        registerReceiver(proxyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Create a notification channel for Android O and higher
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(channelID, "VPN Service", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        // Create and start the foreground service
        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle("VPN Service Active")
            .setContentText("Your VPN connection is active.")
            .build()

        startForeground(notificationID, notification)

        selectedProxy?.let {
            startVpn()
        }
    }

    private fun restartVpn() {
        Log.i(MainActivity.TAG, "Restarting VPN service")

        val intent = Intent(this, AraknetVpnService::class.java)
        intent.putExtra("proxy_address", selectedProxy)
        startService(intent)

        startVpn()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        selectedProxy?.let {
            startVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.cancel()
        vpnInterface?.close()
        vpnInterface = null
        unregisterReceiver(proxyReceiver) // Unregister receiver to prevent memory leaks

        Log.i(MainActivity.TAG, "VPN service stopped")
    }

    private fun startVpn() {
        Log.i(MainActivity.TAG, "Starting VPN service")
        val builder = Builder()
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0) // Route all IPv4 packets
            .addRoute("::", 0) // Route all IPv6 packets
            .addDnsServer("8.8.8.8")
            .setSession("Araknet VPN")

        try {
            builder.addAllowedApplication("com.android.chrome")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(MainActivity.TAG, "Error adding allowed application; $e")
        }

        vpnInterface = builder.establish()
        if (vpnInterface == null) {
            Log.e(MainActivity.TAG, "Error starting VPN service; NULL vpnInterface")
            stopSelf()
            return
        }

        vpnOutputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        vpnInputStream = FileInputStream(vpnInterface!!.fileDescriptor)

        coroutineScope.launch {
            try {
                val buffer = ByteArray(4096)
                val batch = mutableListOf<Packet>()
                var lastFlushTime: Long = System.currentTimeMillis()
                val batchTimeout = 10L // ms

                while (true) {
                    val bytesRead = vpnInputStream?.read(buffer) ?: break
                    if (bytesRead > 0) {
                        val data = buffer.sliceArray(0 until bytesRead)
                        val packet = extractPacketInfo(data) ?: continue
                        batch.add(packet)
                    }

                    val currentTime = System.currentTimeMillis()
                    val batchTimeoutExceeded = (currentTime - lastFlushTime) > batchTimeout

                    if(batch.isNotEmpty() && (batchTimeoutExceeded || batch.size > 25)) { // Time to flush
                        batch.groupBy { it.destination }.forEach { (_, packets) ->
                            val channel = getOrCreateDestChannel(packets.first())
                            for (p in packets) {
                                channel.send(p)
                            }
                        }
                        batch.clear()
                        lastFlushTime = currentTime
                    }
                }

            } catch (e: IOException) {
                Log.e(MainActivity.TAG, "Error reading bytes from VPN interface; $e")
            }
        }
    }

    private fun getOrCreateDestChannel(packet: Packet): Channel<Packet> {
        return destChannels.computeIfAbsent(packet.destination) {
            Channel<Packet>(Channel.UNLIMITED).also { channel ->
                // Launch a coroutine to consume packets
                // sent to destination channel
                coroutineScope.launch {
                    proxyPackets(packet.protocol, packet.destination, channel)
                }
            }
        }
    }

    private fun proxyPackets(
        protocol: Int,
        destination: InetSocketAddress,
        inputChannel: Channel<Packet>
    ) {
        try {
            // Establish connection with destination either directly
            // or via selected proxy
            val conn: Connection = if (isInternalAddress(destination)) {
                getInternalConnection(protocol, destination)
            } else {
                getExternalConnections(protocol, destination)
            }

            // Coroutine: Reads data from the channel
            // and sends it to the destination
            coroutineScope.launch {
                try {
                    Log.i(MainActivity.TAG, "Reading packets from Channel<$destination>")

                    for (packet in inputChannel) {
                        if (packet.destination != destination) {
                            throw IllegalStateException("Received packet with invalid destination ${packet.destination} in Channel<$destination>")
                        }

                        conn.send(packet.data, packet.destination)
                    }

                    Log.w(MainActivity.TAG, "Channel<$destination> closed")

                } catch (e: Exception) {
                    Log.e(MainActivity.TAG, "Error sending data to destination $destination: $e")
                }
            }

            // Coroutine: Read responses from connection
            // and sends it to the VPN output stream
            coroutineScope.launch {
                try {
                    Log.i(MainActivity.TAG, "Reading responses from $destination")
                    val buffer = ByteArray(4096)

                    while (true) {
                        val bytesRead = conn.receive(buffer)
                        if (bytesRead <= 0) break // Connection closed or EOF

                        val data = buffer.copyOf(bytesRead)
                        vpnOutputStream?.let {
                            it.write(data)
                            it.flush()
                        }
                    }

                    Log.w(MainActivity.TAG, "Peer closed connection to $destination")

                } catch (e: Exception) {
                    Log.e(MainActivity.TAG, "Error receiving data from $destination or writing to VPN output stream: $e")
                }
            }

        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Error sending/receiving data to/from $destination: $e")
        }
    }

    private fun getInternalConnection(
        protocol: Int,
        destination: InetSocketAddress
    ): Connection {
        return internalConnections.computeIfAbsent(destination) {
            Log.i(
                MainActivity.TAG,
                "Creating new ${parseProtocol(protocol)} connection to $destination"
            )

            when (protocol) {
                6 -> { // TCP
                    val socket = Socket()
                    socket.connect(destination)
                    TCPConnection(socket)
                }

                17 -> { // UDP
                    val udpSocket = DatagramSocket()
                    UDPConnection(udpSocket)
                }

                else -> throw IllegalArgumentException("Unsupported protocol $protocol")
            }
        }
    }

    private fun getExternalConnections(
        protocol: Int,
        destination: InetSocketAddress
    ): Connection {
        return externalConnections.computeIfAbsent(destination) {
            Log.i(
                MainActivity.TAG,
                "Creating new ${parseProtocol(protocol)} SOCKS tunnel to $destination"
            )

            when (protocol) {
                6 -> { // TCP
                    val socket = SOCKS5.newTCPConnection(
                        selectedProxy!!.hostName, selectedProxy!!.port,
                        destination.hostName, destination.port,
                    )
                    TCPConnection(socket)
                }

                17 -> { // UDP
                    val udpSocket = SOCKS5.newUDPConnection(
                        selectedProxy!!.hostName, selectedProxy!!.port,
                        destination.hostName, destination.port,
                    )
                    UDPConnection(udpSocket)
                }

                else -> throw IllegalArgumentException("Unsupported protocol ${protocol}")
            }
        }
    }

}
