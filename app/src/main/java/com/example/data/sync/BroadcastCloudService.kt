package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.BroadcastMessageEntity
import com.example.data.db.ProtectedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BroadcastCloudService(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("anos_cloud_broadcasts_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_LAST_FETCH_TIME = "last_fetch_time"
        private const val PREF_LOCAL_CACHE = "cloud_broadcast_cache_json"
        
        // Cloud Relay endpoint for Anos FF Global Broadcast Network
        private const val CLOUD_BUCKET = "anos_ff_vip_global_channel_v3"
        private const val CLOUD_API_URL = "https://kvdb.io/A8fX19J6kK8xY2N8zK7qP9/$CLOUD_BUCKET"
        
        // Cloud Relay endpoint for Protected App Config sync
        private const val CLOUD_APP_BUCKET = "anos_ff_protected_app_v4"
        private const val CLOUD_APP_URL = "https://kvdb.io/A8fX19J6kK8xY2N8zK7qP9/$CLOUD_APP_BUCKET"
        private const val PREF_APP_CACHE = "cloud_protected_app_cache_json"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Publie une diffusion sur le Cloud mondial Anos FF pour que tous les joueurs la reçoivent en direct.
     */
    suspend fun publishToCloud(broadcast: BroadcastMessageEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Récupérer les diffusions existantes
            val existingList = fetchFromCloud().toMutableList()
            
            // Éviter les doublons
            existingList.removeAll { it.title == broadcast.title && it.message == broadcast.message }
            existingList.add(0, broadcast)
            
            // Garder les 20 plus récentes
            val trimmedList = existingList.take(20)
            
            val jsonArray = JSONArray()
            trimmedList.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("category", item.category)
                    put("timestamp", item.timestamp)
                    put("senderAdmin", item.senderAdmin)
                }
                jsonArray.put(obj)
            }
            
            val jsonPayload = jsonArray.toString()
            
            // Sauvegarder dans le cache local partagé
            prefs.edit().putString(PREF_LOCAL_CACHE, jsonPayload).apply()

            // Envoyer au relai Cloud KVDB
            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(CLOUD_API_URL)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            // En cas de coupure temporaire, on garde le cache local
            false
        }
    }

    /**
     * Récupère toutes les diffusions publiées par l'administrateur Anos FF depuis le Cloud.
     */
    suspend fun fetchFromCloud(): List<BroadcastMessageEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BroadcastMessageEntity>()
        try {
            val request = Request.Builder()
                .url(CLOUD_API_URL)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                if (jsonString.isNotBlank() && jsonString.startsWith("[")) {
                    prefs.edit().putString(PREF_LOCAL_CACHE, jsonString).apply()
                    return@withContext parseBroadcastsJson(jsonString)
                }
            }
        } catch (_: Exception) {
            // Fallback sur le cache local
        }

        // Fallback Cache Local
        val cached = prefs.getString(PREF_LOCAL_CACHE, null)
        if (!cached.isNullOrBlank()) {
            return@withContext parseBroadcastsJson(cached)
        }

        return@withContext result
    }

    private fun parseBroadcastsJson(jsonString: String): List<BroadcastMessageEntity> {
        val list = mutableListOf<BroadcastMessageEntity>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BroadcastMessageEntity(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        title = obj.optString("title", "Diffusion VIP"),
                        message = obj.optString("message", ""),
                        category = obj.optString("category", "INFO"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        senderAdmin = obj.optString("senderAdmin", "Anos FF (Créateur)"),
                        isRead = false
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Publie la configuration de l'application protégée sur le Cloud mondial Anos FF.
     */
    suspend fun publishProtectedAppToCloud(app: ProtectedAppEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val obj = JSONObject().apply {
                put("appName", app.appName)
                put("packageName", app.packageName)
                put("versionName", app.versionName)
                put("appType", app.appType)
                put("embeddedAppUrl", app.embeddedAppUrl)
                put("apkFileSizeMb", app.apkFileSizeMb)
                put("cloudDownloadUrl", app.cloudDownloadUrl)
                put("customIconBase64", app.customIconBase64)
                put("description", app.description)
                put("isLocked", app.isLocked)
                put("lastUpdated", app.lastUpdated)
            }
            val jsonPayload = obj.toString()
            prefs.edit().putString(PREF_APP_CACHE, jsonPayload).apply()

            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(CLOUD_APP_URL)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Récupère la configuration actuelle de l'application protégée depuis le Cloud.
     */
    suspend fun fetchProtectedAppFromCloud(): ProtectedAppEntity? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CLOUD_APP_URL)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                if (jsonString.isNotBlank() && jsonString.startsWith("{")) {
                    prefs.edit().putString(PREF_APP_CACHE, jsonString).apply()
                    return@withContext parseProtectedAppJson(jsonString)
                }
            }
        } catch (_: Exception) {}

        val cached = prefs.getString(PREF_APP_CACHE, null)
        if (!cached.isNullOrBlank()) {
            return@withContext parseProtectedAppJson(cached)
        }
        return@withContext null
    }

    private fun parseProtectedAppJson(jsonString: String): ProtectedAppEntity? {
        return try {
            val obj = JSONObject(jsonString)
            ProtectedAppEntity(
                id = 1,
                appName = obj.optString("appName", "Free Fire MAX VIP Hub"),
                packageName = obj.optString("packageName", "com.dts.freefireth"),
                versionName = obj.optString("versionName", "v3.102.1 VIP"),
                appType = obj.optString("appType", "IN_APP_CONTAINER"),
                embeddedAppUrl = obj.optString("embeddedAppUrl", ""),
                apkFileSizeMb = obj.optDouble("apkFileSizeMb", 0.0),
                cloudDownloadUrl = obj.optString("cloudDownloadUrl", ""),
                customIconBase64 = obj.optString("customIconBase64", ""),
                description = obj.optString("description", "Application exécutée directement à l'intérieur du Booster Anos FF. Clé VIP obligatoire."),
                isLocked = obj.optBoolean("isLocked", false),
                lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                launchCount = 0
            )
        } catch (_: Exception) {
            null
        }
    }
}
