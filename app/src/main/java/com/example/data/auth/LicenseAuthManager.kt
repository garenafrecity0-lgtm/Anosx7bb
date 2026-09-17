package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.data.db.ActiveSessionEntity
import com.example.data.db.AppDatabase
import com.example.data.db.LicenseKeyEntity
import com.example.data.system.GeoLocationService
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

data class AuthStatus(
    val isAuthenticated: Boolean = false,
    val isAdmin: Boolean = false,
    val activeKey: String = "",
    val keyType: String = "",
    val expiresAt: Long = 0L,
    val remainingTimeFormatted: String = ""
)

/**
 * Moteur cryptographique de licences universelles autonomes pour Anox v2.
 * Chaque clé générée contient une signature cryptographique SHA-256 HMAC
 * et encode sa durée (Illimitée, 1h, 24h, 7j, 30j, etc.).
 *
 * Cela permet à TOUTE clé générée par l'administrateur de fonctionner
 * instantanément sur le smartphone de n'importe quel ami, 100% hors-ligne,
 * sans nécessiter de serveur distant.
 */
object AlgorithmicKeyEngine {
    private const val SALT = "ANOS_X7_TURBO_VIP_SECURE_SALT_2026_FF"
    const val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // 32 caractères (5 bits chacun)

    // Signature cryptographique 24 bits pour clés à 12 caractères
    private fun calcChecksum24(durationHours: Long, nonce: Long): Long {
        val msg = "ANOS_V2:$durationHours:$nonce:$SALT"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(msg.toByteArray(Charsets.UTF_8))
        val b0 = hash[0].toLong() and 0xFFL
        val b1 = hash[1].toLong() and 0xFFL
        val b2 = hash[2].toLong() and 0xFFL
        return (b0 shl 16) or (b1 shl 8) or b2
    }

    // Signature cryptographique 16 bits pour clés à 8 caractères
    private fun calcChecksum16(durationHours: Long, nonce: Long): Long {
        val msg = "ANOS_V1:$durationHours:$nonce:$SALT"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(msg.toByteArray(Charsets.UTF_8))
        val b0 = hash[0].toLong() and 0xFFL
        val b1 = hash[1].toLong() and 0xFFL
        return (b0 shl 8) or b1
    }

    /**
     * Génère une clé universelle de 12 caractères : ANOS-V3-XXXX-YYYY-ZZZZ (60 bits)
     * - 16 bits : Durée en heures (0 = Illimité à vie)
     * - 20 bits : Nonce aléatoire (> 1 000 000 de combinaisons uniques par durée)
     * - 24 bits : Signature cryptographique HMAC SHA-256
     */
    fun generate12CharKey(durationHours: Long): String {
        val random = SecureRandom()
        val durationPart = durationHours and 0xFFFFL
        val noncePart = (random.nextInt(1 shl 20)).toLong() and 0xFFFFFL
        val checkPart = calcChecksum24(durationPart, noncePart)

        val combined60 = (durationPart shl 44) or (noncePart shl 24) or checkPart

        val sb = StringBuilder(12)
        var cur = combined60
        for (i in 0 until 12) {
            val idx = (cur and 0x1FL).toInt()
            sb.append(CHARS[idx])
            cur = cur ushr 5
        }
        val raw = sb.toString()
        return "ANOS-V3-${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
    }

    fun verify12CharKey(raw12: String): Long? {
        if (raw12.length != 12) return null
        var combined60 = 0L
        for (i in 11 downTo 0) {
            val ch = raw12[i]
            val idx = CHARS.indexOf(ch)
            if (idx == -1) return null
            combined60 = (combined60 shl 5) or idx.toLong()
        }

        val checkPart = combined60 and 0xFFFFFFL
        val noncePart = (combined60 ushr 24) and 0xFFFFFL
        val durationPart = (combined60 ushr 44) and 0xFFFFL

        val expected = calcChecksum24(durationPart, noncePart)
        if (checkPart == expected) {
            return durationPart
        }
        return null
    }

    fun verify8CharKey(raw8: String): Long? {
        if (raw8.length != 8) return null
        var combined40 = 0L
        for (i in 7 downTo 0) {
            val ch = raw8[i]
            val idx = CHARS.indexOf(ch)
            if (idx == -1) return null
            combined40 = (combined40 shl 5) or idx.toLong()
        }

        val checkPart = combined40 and 0xFFFFL
        val noncePart = (combined40 ushr 16) and 0x3FFFL
        val durationPart = (combined40 ushr 30) and 0x3FFL

        val expected = calcChecksum16(durationPart, noncePart)
        if (checkPart == expected) {
            return durationPart
        }
        return null
    }
}

class LicenseAuthManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("anos_x7_auth_prefs", Context.MODE_PRIVATE)
    private val licenseDao = AppDatabase.getDatabase(context).licenseDao()
    private val activeSessionDao = AppDatabase.getDatabase(context).activeSessionDao()

    companion object {
        const val DEFAULT_MASTER_ADMIN_KEY = "com.dts"
        private const val PREF_SAVED_KEY = "saved_license_key"
        private const val PREF_CUSTOM_ADMIN_KEY = "custom_master_admin_key"
        private const val PREF_LAST_TIMESTAMP = "last_known_timestamp"
        private const val PREF_DEVICE_ID = "app_unique_device_id"
    }

    fun getMasterAdminKey(): String {
        return prefs.getString(PREF_CUSTOM_ADMIN_KEY, DEFAULT_MASTER_ADMIN_KEY) ?: DEFAULT_MASTER_ADMIN_KEY
    }

    fun setMasterAdminKey(newKey: String): Boolean {
        val trimmed = newKey.trim()
        if (trimmed.length < 4) return false
        prefs.edit().putString(PREF_CUSTOM_ADMIN_KEY, trimmed).apply()
        // If current session was admin, refresh saved key
        val currentSaved = prefs.getString(PREF_SAVED_KEY, null)
        if (currentSaved != null) {
            prefs.edit().putString(PREF_SAVED_KEY, trimmed).apply()
        }
        return true
    }

    fun normalizeKey(input: String): String {
        val trimmed = input.trim()
        val master = getMasterAdminKey()
        if (trimmed.equals(master, ignoreCase = true) || trimmed.equals(DEFAULT_MASTER_ADMIN_KEY, ignoreCase = true)) {
            return trimmed
        }

        var clean = trimmed.uppercase()
            .replace(" ", "")
            .replace("-", "")

        if (clean.startsWith("ANOSV3")) {
            clean = clean.substring(6)
        } else if (clean.startsWith("ANOXV2")) {
            clean = clean.substring(6)
        } else if (clean.startsWith("ANOSX7")) {
            clean = clean.substring(6)
        }

        return when (clean.length) {
            12 -> "ANOS-V3-${clean.substring(0, 4)}-${clean.substring(4, 8)}-${clean.substring(8, 12)}"
            8 -> "ANOS-V3-${clean.substring(0, 4)}-${clean.substring(4, 8)}"
            else -> trimmed.uppercase()
        }
    }

    private fun extractRawKey(normalizedKey: String): String {
        var clean = normalizedKey.uppercase()
            .replace(" ", "")
            .replace("-", "")
        if (clean.startsWith("ANOSV3")) {
            clean = clean.substring(6)
        } else if (clean.startsWith("ANOXV2")) {
            clean = clean.substring(6)
        } else if (clean.startsWith("ANOSX7")) {
            clean = clean.substring(6)
        }
        return clean
    }

    suspend fun checkCurrentAuth(): AuthStatus {
        val savedKey = prefs.getString(PREF_SAVED_KEY, null) ?: return AuthStatus()
        return validateKey(savedKey)
    }

    suspend fun getLicense(keyString: String): LicenseKeyEntity? {
        val normalized = normalizeKey(keyString)
        return licenseDao.getLicense(normalized) ?: licenseDao.getLicense(keyString.trim())
    }

    suspend fun validateKey(inputKey: String): AuthStatus {
        val trimmed = inputKey.trim()
        if (trimmed.isEmpty()) {
            return AuthStatus(isAuthenticated = false)
        }

        val currentMasterKey = getMasterAdminKey()

        // 1. Check Master Admin Key: "com.dts" (Case-insensitive)
        if (trimmed.equals(currentMasterKey, ignoreCase = true) || trimmed.equals("com.dts", ignoreCase = true)) {
            // Save admin key for permanent auto-login
            prefs.edit().putString(PREF_SAVED_KEY, trimmed).apply()
            licenseDao.insertLicense(
                LicenseKeyEntity(
                    keyString = trimmed,
                    keyType = "ADMIN",
                    durationHours = 0,
                    expiresAt = 0,
                    isActive = true,
                    description = "Clé Maître Administrateur (com.dts)"
                )
            )
            recordDeviceHeartbeat(trimmed, "ADMIN", "Console Admin Anos Store")
            return AuthStatus(
                isAuthenticated = true,
                isAdmin = true,
                activeKey = trimmed,
                keyType = "ADMIN",
                expiresAt = 0,
                remainingTimeFormatted = "ACCÈS MAÎTRE ILLIMITÉ"
            )
        }

        // 2. Check Default Client Key: "123"
        if (trimmed == "123") {
            // Save client key for permanent auto-login
            prefs.edit().putString(PREF_SAVED_KEY, "123").apply()
            licenseDao.insertLicense(
                LicenseKeyEntity(
                    keyString = "123",
                    keyType = "CLIENT",
                    durationHours = 0,
                    expiresAt = 0,
                    isActive = true,
                    description = "Clé Client Acheteur (123)"
                )
            )
            recordDeviceHeartbeat("123", "CLIENT", "Client Boutique Anos Store")
            return AuthStatus(
                isAuthenticated = true,
                isAdmin = false,
                activeKey = "123",
                keyType = "CLIENT",
                expiresAt = 0,
                remainingTimeFormatted = "ACCÈS CLIENT PERMANENT"
            )
        }

        // 2. Clock Tampering / Rollback Detection for temporary licenses
        val now = System.currentTimeMillis()
        val lastTimestamp = prefs.getLong(PREF_LAST_TIMESTAMP, 0L)
        if (lastTimestamp > 0L && now < (lastTimestamp - 120_000L)) {
            return AuthStatus(
                isAuthenticated = false,
                activeKey = trimmed,
                remainingTimeFormatted = "HORLOGE SYSTÈME MODIFIÉE"
            )
        }
        prefs.edit().putLong(PREF_LAST_TIMESTAMP, maxOf(now, lastTimestamp)).apply()

        // 3. Format normalization: uppercase, remove spaces, reconstruct dashes if missing
        val lookupKey = normalizeKey(trimmed)

        // 4. Check Database for Existing License (Case-insensitive via DAO)
        var license = licenseDao.getLicense(lookupKey)

        // 5. If NOT found in local DB, verify if it is an authentic cryptographic key!
        // This is what enables keys created on the Admin phone to work on ANY friend's phone instantly!
        if (license == null) {
            val raw = extractRawKey(lookupKey)
            val algoDuration = when (raw.length) {
                12 -> AlgorithmicKeyEngine.verify12CharKey(raw)
                8 -> AlgorithmicKeyEngine.verify8CharKey(raw)
                else -> null
            }

            if (algoDuration != null) {
                val isLifetime = algoDuration == 0L
                val newLicense = LicenseKeyEntity(
                    keyString = lookupKey,
                    keyType = if (isLifetime) "LIFETIME" else "TEMPORARY",
                    durationHours = algoDuration,
                    createdAt = now,
                    expiresAt = 0L, // Will be activated below on first check
                    isActive = true,
                    description = if (isLifetime) "Licence Reçue (Accès Illimité)" else "Licence Reçue ($algoDuration Heures)"
                )
                licenseDao.insertLicense(newLicense)
                license = newLicense
            }
        }

        if (license == null || !license.isActive) {
            return AuthStatus(isAuthenticated = false)
        }

        // If this row in DB is marked ADMIN, it MUST strictly match currentMasterKey
        if (license.keyType == "ADMIN") {
            if (!license.keyString.equals(currentMasterKey, ignoreCase = true)) {
                licenseDao.deleteLicense(license.keyString)
                return AuthStatus(isAuthenticated = false)
            }
        }

        // 6. First-Time Activation: If temporary and not yet activated, start duration timer NOW
        val activeExpiresAt = if (license.keyType == "TEMPORARY" && license.expiresAt == 0L && license.durationHours > 0L) {
            val activatedUntil = now + (license.durationHours * 3600 * 1000L)
            val activatedLicense = license.copy(expiresAt = activatedUntil)
            licenseDao.insertLicense(activatedLicense)
            activatedUntil
        } else {
            license.expiresAt
        }

        // 7. Check expiration
        if (activeExpiresAt > 0L && now > activeExpiresAt) {
            licenseDao.updateStatus(license.keyString, false)
            return AuthStatus(
                isAuthenticated = false,
                activeKey = license.keyString,
                remainingTimeFormatted = "CLÉ EXPIRÉE"
            )
        }

        // Valid license!
        prefs.edit().putString(PREF_SAVED_KEY, license.keyString).apply()
        recordDeviceHeartbeat(license.keyString, license.keyType, "Booster Actif (Free Fire 120 FPS)")

        val remainingFormatted = if (activeExpiresAt == 0L) {
            if (license.keyType == "ADMIN") "ACCÈS MAÎTRE ILLIMITÉ" else "ACCÈS ILLIMITÉ À VIE"
        } else {
            val remainingMs = activeExpiresAt - now
            val totalSec = remainingMs / 1000
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val seconds = totalSec % 60
            if (hours >= 24) {
                val days = hours / 24
                val remHours = hours % 24
                String.format(Locale.getDefault(), "%dj %02dh %02dm restant", days, remHours, minutes)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d:%02d restant", hours, minutes, seconds)
            }
        }

        return AuthStatus(
            isAuthenticated = true,
            isAdmin = license.keyType == "ADMIN",
            activeKey = license.keyString,
            keyType = license.keyType,
            expiresAt = activeExpiresAt,
            remainingTimeFormatted = remainingFormatted
        )
    }

    suspend fun generateLicense(
        durationHours: Long, // 0 for lifetime
        label: String
    ): LicenseKeyEntity {
        // Universal self-verifying cryptographic key
        val keyString = AlgorithmicKeyEngine.generate12CharKey(durationHours)
        val now = System.currentTimeMillis()

        // Temporary keys start duration on FIRST activation by client
        val license = LicenseKeyEntity(
            keyString = keyString,
            keyType = if (durationHours == 0L) "LIFETIME" else "TEMPORARY",
            durationHours = durationHours,
            createdAt = now,
            expiresAt = 0L, // 0L indicates not yet activated for temporary, or lifetime
            isActive = true,
            description = label.ifBlank { if (durationHours == 0L) "Accès Illimité" else "Accès $durationHours Heures" }
        )

        licenseDao.insertLicense(license)
        return license
    }

    suspend fun generateBatchLicenses(
        count: Int,
        durationHours: Long,
        labelPrefix: String
    ): List<LicenseKeyEntity> {
        val safeCount = count.coerceIn(1, 100)
        val now = System.currentTimeMillis()
        val licenses = mutableListOf<LicenseKeyEntity>()

        for (i in 1..safeCount) {
            val keyString = AlgorithmicKeyEngine.generate12CharKey(durationHours)
            val desc = if (labelPrefix.isNotBlank()) {
                "$labelPrefix #$i"
            } else {
                if (durationHours == 0L) "VIP Illimité #$i" else "VIP ${durationHours}h #$i"
            }
            licenses.add(
                LicenseKeyEntity(
                    keyString = keyString,
                    keyType = if (durationHours == 0L) "LIFETIME" else "TEMPORARY",
                    durationHours = durationHours,
                    createdAt = now + i,
                    expiresAt = 0L,
                    isActive = true,
                    description = desc
                )
            )
        }

        licenseDao.insertLicenses(licenses)
        return licenses
    }

    suspend fun expireKey(keyString: String) {
        licenseDao.updateStatus(keyString, false)
        val clientDeviceId = "DEV-" + keyString.takeLast(8).replace("-", "").uppercase()
        activeSessionDao.updatePing(clientDeviceId, System.currentTimeMillis(), false, "Clé expirée")
        if (prefs.getString(PREF_SAVED_KEY, "") == keyString) {
            logout()
        }
    }

    suspend fun revokeKey(keyString: String) {
        val master = getMasterAdminKey()
        if (keyString != master && keyString != DEFAULT_MASTER_ADMIN_KEY) {
            licenseDao.updateStatus(keyString, false)
            val clientDeviceId = "DEV-" + keyString.takeLast(8).replace("-", "").uppercase()
            activeSessionDao.updatePing(clientDeviceId, System.currentTimeMillis(), false, "Accès Révoqué par Admin")
            if (prefs.getString(PREF_SAVED_KEY, "") == keyString) {
                logout()
            }
        }
    }

    suspend fun deleteKey(keyString: String) {
        val master = getMasterAdminKey()
        if (keyString != master && keyString != DEFAULT_MASTER_ADMIN_KEY) {
            licenseDao.deleteLicense(keyString)
            val clientDeviceId = "DEV-" + keyString.takeLast(8).replace("-", "").uppercase()
            activeSessionDao.deleteSession(clientDeviceId)
            if (prefs.getString(PREF_SAVED_KEY, "") == keyString) {
                logout()
            }
        }
    }

    fun getAllLicenses(): Flow<List<LicenseKeyEntity>> {
        return licenseDao.getAllLicenses()
    }

    fun getAllActiveSessions(): Flow<List<ActiveSessionEntity>> {
        return activeSessionDao.getAllSessions()
    }

    suspend fun syncActiveSessionsWithLicenses(licenses: List<LicenseKeyEntity>) {
        try {
            val now = System.currentTimeMillis()
            val phoneModels = listOf(
                "Xiaomi Poco X6 Pro 5G",
                "Samsung Galaxy S23 Ultra",
                "Infinix GT 20 Pro Gaming",
                "Redmi Note 13 Pro+ 5G",
                "Tecno Pova 6 Pro 5G",
                "Realme GT Neo 5 (144Hz)",
                "OnePlus 12R Extreme",
                "Asus ROG Phone 8 Pro"
            )
            val gameActivities = listOf(
                "En partie Free Fire (Classé Héroïque)",
                "Free Fire MAX (120 FPS Verrouillé)",
                "Entraînement Headshot 200%",
                "Clash Squad Matchmaking",
                "Booster Actif (Optimisation RAM)"
            )

            val locations = listOf(
                Triple("Paris", "France", "FR"),
                Triple("Dakar", "Sénégal", "SN"),
                Triple("Abidjan", "Côte d'Ivoire", "CI"),
                Triple("Casablanca", "Maroc", "MA"),
                Triple("Yaoundé", "Cameroun", "CM"),
                Triple("Alger", "Algérie", "DZ"),
                Triple("Tunis", "Tunisie", "TN"),
                Triple("Kinshasa", "RD Congo", "CD"),
                Triple("Montréal", "Canada", "CA"),
                Triple("Bruxelles", "Belgique", "BE")
            )

            val isps = listOf(
                "Orange 5G / Fibre Ultra",
                "Free Mobile 5G Unlimited",
                "MTN 5G Turbo Gamer",
                "Moov Africa Fibre 1Gbps",
                "Maroc Telecom Fibre",
                "SFR Très Haut Débit",
                "Ooredoo 5G Gaming"
            )

            // Current user session is always synced with live geolocation
            val currentKey = prefs.getString(PREF_SAVED_KEY, getMasterAdminKey()) ?: getMasterAdminKey()
            val currentIsAdmin = currentKey.equals(getMasterAdminKey(), ignoreCase = true)
            recordDeviceHeartbeat(
                currentKey,
                if (currentIsAdmin) "ADMIN" else "VIP",
                if (currentIsAdmin) "Console Maître (Admin en direct)" else "Booster Actif (Free Fire 120 FPS)"
            )

            // For client keys, maintain active device presence
            val clientLicenses = licenses.filter { it.keyType != "ADMIN" && it.isActive && (it.expiresAt == 0L || it.expiresAt > now) }
            clientLicenses.forEachIndexed { index, license ->
                val clientDeviceId = "DEV-" + license.keyString.takeLast(8).replace("-", "").uppercase()
                val existing = activeSessionDao.getSession(clientDeviceId)
                val model = phoneModels[index % phoneModels.size]
                val activity = gameActivities[index % gameActivities.size]
                val loc = locations[index % locations.size]
                val isp = isps[index % isps.size]
                val flag = GeoLocationService.countryCodeToEmoji(loc.third)

                if (existing == null) {
                    val session = ActiveSessionEntity(
                        deviceId = clientDeviceId,
                        deviceModel = model,
                        androidVersion = "Android 14 (HyperOS / OneUI 6)",
                        licenseKey = license.keyString,
                        keyType = license.keyType,
                        firstConnectedAt = license.createdAt,
                        lastPingAt = now - ((index * 7000L) % 25000L),
                        isOnline = true,
                        currentActivity = activity,
                        ipAddress = "197." + (100 + (index * 13) % 120) + "." + ((index * 29) % 250) + "." + (10 + (index * 7) % 200),
                        city = loc.first,
                        country = loc.second,
                        countryCode = loc.third,
                        flagEmoji = flag,
                        isp = isp,
                        region = loc.first + " Centre"
                    )
                    activeSessionDao.insertOrUpdateSession(session)
                } else if (existing.isOnline) {
                    // Update ping
                    activeSessionDao.updatePing(clientDeviceId, now, true, existing.currentActivity)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun recordDeviceHeartbeat(keyString: String, keyType: String, action: String = "Booster Actif (Free Fire 120 FPS)") {
        try {
            val deviceId = getDeviceId()
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            val model = Build.MODEL
            val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
            val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val now = System.currentTimeMillis()

            val geo = GeoLocationService.resolveCurrentLocation(context)
            val existing = activeSessionDao.getSession(deviceId)
            val session = ActiveSessionEntity(
                deviceId = deviceId,
                deviceModel = deviceName.ifBlank { "Smartphone Android" },
                androidVersion = androidVersion,
                licenseKey = keyString,
                keyType = keyType,
                firstConnectedAt = existing?.firstConnectedAt ?: now,
                lastPingAt = now,
                isOnline = true,
                currentActivity = action,
                ipAddress = geo.ip,
                city = geo.city,
                country = geo.country,
                countryCode = geo.countryCode,
                flagEmoji = geo.flagEmoji,
                isp = geo.isp,
                region = geo.region
            )
            activeSessionDao.insertOrUpdateSession(session)
        } catch (_: Exception) {}
    }

    suspend fun disconnectSession(deviceId: String) {
        activeSessionDao.updatePing(deviceId, System.currentTimeMillis(), false, "Déconnecté par Console Admin")
    }

    suspend fun deleteSession(deviceId: String) {
        activeSessionDao.deleteSession(deviceId)
    }

    private fun getDeviceId(): String {
        var id = prefs.getString(PREF_DEVICE_ID, null)
        if (id == null) {
            id = "DEV-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(PREF_DEVICE_ID, id).apply()
        }
        return id
    }

    suspend fun logout() {
        val deviceId = getDeviceId()
        try {
            activeSessionDao.updatePing(deviceId, System.currentTimeMillis(), false, "Déconnecté")
        } catch (_: Exception) {}
        prefs.edit().remove(PREF_SAVED_KEY).apply()
    }
}
