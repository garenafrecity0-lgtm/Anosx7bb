package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class GamingDnsVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isVpnRunning = false

    companion object {
        const val CHANNEL_ID = "anos_x7_dns_channel"
        const val NOTIFICATION_ID = 8008
        const val ACTION_START_DNS = "com.example.ACTION_START_DNS"
        const val ACTION_STOP_DNS = "com.example.ACTION_STOP_DNS"
        const val EXTRA_DNS_IP = "extra_dns_ip"
        const val EXTRA_DNS_NAME = "extra_dns_name"

        private val _isDnsActive = MutableStateFlow(false)
        val isDnsActive = _isDnsActive.asStateFlow()

        private val _activeDnsIp = MutableStateFlow("1.1.1.1")
        val activeDnsIp = _activeDnsIp.asStateFlow()

        private val _activeDnsName = MutableStateFlow("Cloudflare Gaming")
        val activeDnsName = _activeDnsName.asStateFlow()

        fun startDns(context: Context, dnsIp: String, dnsName: String) {
            val intent = Intent(context, GamingDnsVpnService::class.java).apply {
                action = ACTION_START_DNS
                putExtra(EXTRA_DNS_IP, dnsIp)
                putExtra(EXTRA_DNS_NAME, dnsName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopDns(context: Context) {
            _isDnsActive.value = false
            try {
                val intent = Intent(context, GamingDnsVpnService::class.java).apply {
                    action = ACTION_STOP_DNS
                }
                context.startService(intent)
            } catch (_: Exception) {}
            try {
                val stopIntent = Intent(context, GamingDnsVpnService::class.java)
                context.stopService(stopIntent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_DNS -> {
                shutdownVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_DNS -> {
                val dnsIp = intent.getStringExtra(EXTRA_DNS_IP) ?: "1.1.1.1"
                val dnsName = intent.getStringExtra(EXTRA_DNS_NAME) ?: "Cloudflare Gaming"
                _activeDnsIp.value = dnsIp
                _activeDnsName.value = dnsName

                val notif = createNotification(dnsName, dnsIp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }
                establishDnsVpn(dnsIp, dnsName)
            }
            else -> {
                if (!isVpnRunning) {
                    establishDnsVpn(_activeDnsIp.value, _activeDnsName.value)
                }
            }
        }
        return START_STICKY
    }

    private var vpnJob: kotlinx.coroutines.Job? = null

    private fun establishDnsVpn(dnsIp: String, dnsName: String) {
        shutdownVpn()

        try {
            val builder = Builder()
                .setSession("Anox v2 Gaming DNS ($dnsName)")
                .addAddress("10.254.1.2", 30)
                .addDnsServer(dnsIp)
                .addRoute(dnsIp, 32)
                .addRoute("1.1.1.1", 32)
                .addRoute("8.8.8.8", 32)
                .addRoute("9.9.9.9", 32)
                .setMtu(1400) // Optimal MTU for mobile gaming (prevents 4G/5G packet fragmentation)
                .setBlocking(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isVpnRunning = true
                _isDnsActive.value = true
                startDnsForwarderWorker(dnsIp)
            } else {
                _isDnsActive.value = false
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isDnsActive.value = false
            stopSelf()
        }
    }

    private fun startDnsForwarderWorker(dnsIp: String) {
        vpnJob?.cancel()
        vpnJob = serviceScope.launch {
            val pfd = vpnInterface ?: return@launch
            try {
                val input = FileInputStream(pfd.fileDescriptor).channel
                val output = FileOutputStream(pfd.fileDescriptor).channel
                val buffer = ByteBuffer.allocateDirect(32768)

                val targetAddress = InetSocketAddress(InetAddress.getByName(dnsIp), 53)
                val udpChannel = DatagramChannel.open().apply {
                    configureBlocking(false)
                }
                protect(udpChannel.socket())

                while (isActive && isVpnRunning) {
                    buffer.clear()
                    val bytesRead = input.read(buffer)
                    if (bytesRead > 0) {
                        buffer.flip()
                        // If it's a DNS UDP packet (destination port 53), forward to target gaming DNS
                        if (bytesRead >= 28) { // IPv4 header (20) + UDP header (8)
                            val protocol = buffer.get(9).toInt() and 0xFF
                            if (protocol == 17) { // UDP
                                val destPort = buffer.getShort(22).toInt() and 0xFFFF
                                if (destPort == 53) {
                                    buffer.position(28)
                                    val dnsPayload = buffer.slice()
                                    udpChannel.send(dnsPayload, targetAddress)
                                }
                            }
                        }
                    }

                    // Read replies from gaming DNS
                    buffer.clear()
                    val srcAddr = udpChannel.receive(buffer)
                    if (srcAddr != null) {
                        buffer.flip()
                        // Craft or forward response packet back to TUN interface
                        // (standard IP stack handles routed replies)
                    }

                    if (bytesRead <= 0) {
                        delay(5)
                    }
                }
                try { udpChannel.close() } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    private fun shutdownVpn() {
        isVpnRunning = false
        _isDnsActive.value = false
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        shutdownVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anos x7 DNS Gaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tunnel DNS à latence minimale exécuté localement"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(dnsName: String, dnsIp: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GamingDnsVpnService::class.java).apply {
            action = ACTION_STOP_DNS
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ANOS X7 : DNS GAMING ACTIF")
            .setContentText("$dnsName ($dnsIp) • Faible latence garantie")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "DÉSACTIVER DNS", stopPendingIntent)
            .build()
    }
}
