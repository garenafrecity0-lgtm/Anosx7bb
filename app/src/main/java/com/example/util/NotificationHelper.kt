package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_BROADCAST = "anos_v3_broadcast_channel"
    const val CHANNEL_ADMIN_ALERTS = "anos_v3_admin_alerts_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Broadcast Channel (High importance, vibration & lights for all users)
            val broadcastChannel = NotificationChannel(
                CHANNEL_BROADCAST,
                "Anos v3 Annonces & Notifications Joueurs",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Diffusions et alertes prioritaires envoyées par l'administrateur Anos v3"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(broadcastChannel)

            // 2. Admin Alerts Channel (Immediate notification when any user logs in)
            val adminChannel = NotificationChannel(
                CHANNEL_ADMIN_ALERTS,
                "Anos v3 Alertes Connexions Admin",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes instantanées de nouvelles connexions et géolocalisation des joueurs"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(adminChannel)
        }
    }

    fun postBroadcastNotification(
        context: Context,
        title: String,
        message: String,
        category: String = "INFO",
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        initNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryPrefix = when (category) {
            "BOOST" -> "🔥 [BOOST] "
            "ALERT" -> "⚠️ [ALERTE VIP] "
            "VIP" -> "💎 [COMMUNAUTÉ VIP] "
            else -> "📢 [MESSAGE ADMIN] "
        }

        val fullTitle = if (title.startsWith("📢") || title.startsWith("🔥") || title.startsWith("⚠️") || title.startsWith("💎")) {
            title
        } else {
            "$categoryPrefix$title"
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_BROADCAST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(fullTitle)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(fullTitle)
                    .setSummaryText("Anos v3 VIP Network")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#00E5FF"))
            .build()

        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS runtime permission on Android 13+
        }
    }

    fun postAdminLoginAlert(
        context: Context,
        deviceModel: String,
        city: String,
        country: String,
        flagEmoji: String,
        licenseKey: String,
        ipAddress: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        initNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚨 NOUVELLE CONNEXION : $deviceModel"
        val locationText = "📍 $city, $country $flagEmoji • IP: $ipAddress"
        val body = "$locationText\n🔑 Clé Joueur : $licenseKey\n⚡ Session enregistrée en direct dans la Console Admin."

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ADMIN_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(locationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
                    .setSummaryText("Sécurité & Traçage Anos v3")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 200, 100, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#FF1744"))
            .build()

        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS runtime permission on Android 13+
        }
    }
}
