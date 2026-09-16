package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boost_logs")
data class BoostLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val ramFreedMb: Long,
    val previousRamPercent: Int,
    val newRamPercent: Int,
    val modeUsed: String,
    val durationMs: Long
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isCustomAdded: Boolean = false,
    val lastBoosted: Long = 0L,
    val boostCount: Int = 0
)

@Entity(tableName = "licenses")
data class LicenseKeyEntity(
    @PrimaryKey
    val keyString: String,
    val keyType: String, // "ADMIN", "LIFETIME", "TEMPORARY"
    val durationHours: Long, // 0 for lifetime/admin, or N hours
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L, // 0 if lifetime/admin
    val isActive: Boolean = true,
    val description: String = ""
)

@Entity(tableName = "active_sessions")
data class ActiveSessionEntity(
    @PrimaryKey
    val deviceId: String,
    val deviceModel: String,
    val androidVersion: String,
    val licenseKey: String,
    val keyType: String = "TEMPORARY",
    val firstConnectedAt: Long = System.currentTimeMillis(),
    val lastPingAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val currentActivity: String = "Booster Actif (Free Fire 120 FPS)",
    val ipAddress: String = "192.168.1.42",
    val country: String = "France",
    val city: String = "Paris",
    val countryCode: String = "FR",
    val flagEmoji: String = "🇫🇷",
    val isp: String = "Orange 5G / Fibre",
    val region: String = "Île-de-France"
)

@Entity(tableName = "broadcast_messages")
data class BroadcastMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val message: String,
    val category: String = "INFO", // "INFO", "BOOST", "ALERT", "VIP"
    val timestamp: Long = System.currentTimeMillis(),
    val senderAdmin: String = "Console Maître Anos v3",
    val isRead: Boolean = false
)

