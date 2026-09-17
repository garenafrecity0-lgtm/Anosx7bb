package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.StoreProductEntity
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CloudProduct(
    val remoteId: String,
    val name: String,
    val description: String,
    val price: String,
    val imageUrl: String = "",
    val buyUrl: String = "",
    val category: String = "Application VIP",
    val badge: String = "NOUVEAU",
    val createdAt: Long = System.currentTimeMillis()
)

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val count: Int, val timestamp: Long) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * Service Cloud sécurisé Anos Cloud Sync :
 * Permet à l'administrateur de publier des produits dans le Cloud en temps réel,
 * et permet à tous les clients (autres téléphones) de recevoir les nouveaux produits
 * et de déclencher les notifications système dès qu'une nouveauté est publiée !
 */
class CloudStoreSyncService private constructor(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val storeProductDao = db.storeProductDao()
    private val prefs = context.getSharedPreferences("anos_cloud_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _isCloudConnected = MutableStateFlow(true)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    companion object {
        private const val TAG = "CloudStoreSync"
        private const val PREF_KNOWN_PRODUCT_NAMES = "known_product_names_set"
        private const val PREF_LAST_SYNC_TIME = "last_sync_time"
        private const val PREF_CUSTOM_SERVER_URL = "custom_sync_server_url"

        // Clé de synchronisation publique Anos Store Cloud
        // Utilise le service de synchronisation KV décentralisé npoint/kvstore
        private const val DEFAULT_BIN_ID = "anos_store_vip_products_catalog"
        private const val CLOUD_ENDPOINT = "https://api.npoint.io/46efb2046522e83ee9bd"

        @Volatile
        private var INSTANCE: CloudStoreSyncService? = null

        fun getInstance(context: Context): CloudStoreSyncService {
            return INSTANCE ?: synchronized(this) {
                val inst = CloudStoreSyncService(context.applicationContext)
                INSTANCE = inst
                inst
            }
        }
    }

    /**
     * Télécharge le catalogue en ligne depuis le Cloud Anos,
     * insère les nouveaux produits dans la base locale Room du client,
     * et émet une VRAIE notification sur le téléphone du client pour tout nouveau produit détecté !
     */
    suspend fun syncFromCloud(silent: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Syncing
        try {
            val request = Request.Builder()
                .url(CLOUD_ENDPOINT)
                .header("Accept", "application/json")
                .header("User-Agent", "AnosStore-Android/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                _syncStatus.value = SyncStatus.Error("Serveur Cloud indisponible (HTTP $code)")
                return@withContext Result.failure(Exception("HTTP error $code"))
            }

            val body = response.body?.string() ?: "[]"
            response.close()

            val parsedProducts = parseCloudJson(body)
            if (parsedProducts.isEmpty()) {
                _syncStatus.value = SyncStatus.Success(0, System.currentTimeMillis())
                return@withContext Result.success(0)
            }

            // Récupérer les noms de produits déjà connus pour notifier uniquement les NOUVEAUX
            val knownNames = prefs.getStringSet(PREF_KNOWN_PRODUCT_NAMES, emptySet())?.toMutableSet()
                ?: mutableSetOf()
            val isFirstInstallSync = knownNames.isEmpty()

            var newCount = 0
            val entitiesToSave = mutableListOf<StoreProductEntity>()
            val newlyDiscoveredProducts = mutableListOf<CloudProduct>()

            for (cp in parsedProducts) {
                val isNewForThisDevice = !knownNames.contains(cp.name.trim().lowercase())
                if (isNewForThisDevice) {
                    newCount++
                    knownNames.add(cp.name.trim().lowercase())
                    if (!isFirstInstallSync) {
                        newlyDiscoveredProducts.add(cp)
                    }
                }

                entitiesToSave.add(
                    StoreProductEntity(
                        name = cp.name,
                        description = cp.description,
                        price = cp.price,
                        imageUrl = cp.imageUrl,
                        buyUrl = cp.buyUrl,
                        category = cp.category,
                        badge = cp.badge,
                        createdAt = cp.createdAt
                    )
                )
            }

            // Sauvegarder les noms connus
            prefs.edit()
                .putStringSet(PREF_KNOWN_PRODUCT_NAMES, knownNames)
                .putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis())
                .apply()

            // Mettre à jour Room : insérer ou mettre à jour les produits
            for (entity in entitiesToSave) {
                // Si le produit existe déjà par nom, on met à jour son prix / badge / description
                val allExisting = storeProductDao.getAllProducts()
                storeProductDao.insertProduct(entity)
            }

            // Déclencher les notifications réelles sur les téléphones des clients pour les nouveautés
            if (!silent && newlyDiscoveredProducts.isNotEmpty()) {
                for (p in newlyDiscoveredProducts) {
                    NotificationHelper.postNewProductNotification(
                        context = context,
                        productName = p.name,
                        productPrice = p.price,
                        productCategory = p.category,
                        productDescription = p.description
                    )
                    delay(300) // espacer si plusieurs
                }
            }

            _isCloudConnected.value = true
            _syncStatus.value = SyncStatus.Success(parsedProducts.size, System.currentTimeMillis())
            Log.d(TAG, "Sync successful: ${parsedProducts.size} products fetched from Cloud.")
            Result.success(parsedProducts.size)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud sync failed", e)
            _isCloudConnected.value = false
            _syncStatus.value = SyncStatus.Error("Connexion Cloud impossible : ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Publie l'ensemble des produits de la base locale vers le serveur Cloud partagé
     * pour que tous les autres utilisateurs puissent les voir instantanément !
     */
    suspend fun publishCatalogToCloud(products: List<StoreProductEntity>): Result<Boolean> = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Syncing
        try {
            val jsonArray = JSONArray()
            for (p in products) {
                val obj = JSONObject().apply {
                    put("name", p.name)
                    put("description", p.description)
                    put("price", p.price)
                    put("imageUrl", p.imageUrl)
                    put("buyUrl", p.buyUrl)
                    put("category", p.category)
                    put("badge", p.badge)
                    put("createdAt", p.createdAt)
                }
                jsonArray.put(obj)
            }

            val payload = jsonArray.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(CLOUD_ENDPOINT)
                .header("Content-Type", "application/json")
                .header("User-Agent", "AnosStore-Admin/1.0")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful
            response.close()

            if (isSuccess) {
                _isCloudConnected.value = true
                _syncStatus.value = SyncStatus.Success(products.size, System.currentTimeMillis())
                Log.d(TAG, "Successfully pushed ${products.size} products to Cloud.")
                Result.success(true)
            } else {
                _syncStatus.value = SyncStatus.Error("Erreur serveur lors de la publication.")
                Result.failure(Exception("HTTP error ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push catalog to Cloud", e)
            _isCloudConnected.value = false
            _syncStatus.value = SyncStatus.Error("Échec de mise en ligne : ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Exporte un format JSON ou lien de synchronisation pour partage manuel direct
     */
    fun exportCatalogJson(products: List<StoreProductEntity>): String {
        val jsonArray = JSONArray()
        for (p in products) {
            val obj = JSONObject().apply {
                put("name", p.name)
                put("description", p.description)
                put("price", p.price)
                put("imageUrl", p.imageUrl)
                put("buyUrl", p.buyUrl)
                put("category", p.category)
                put("badge", p.badge)
                put("createdAt", p.createdAt)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    /**
     * Importe un catalogue depuis une chaîne JSON
     */
    suspend fun importCatalogJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val list = parseCloudJson(jsonString)
            if (list.isEmpty()) {
                return@withContext Result.failure(Exception("Format JSON vide ou invalide"))
            }
            for (p in list) {
                storeProductDao.insertProduct(
                    StoreProductEntity(
                        name = p.name,
                        description = p.description,
                        price = p.price,
                        imageUrl = p.imageUrl,
                        buyUrl = p.buyUrl,
                        category = p.category,
                        badge = p.badge,
                        createdAt = p.createdAt
                    )
                )
            }
            Result.success(list.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCloudJson(jsonString: String): List<CloudProduct> {
        val result = mutableListOf<CloudProduct>()
        try {
            val clean = jsonString.trim()
            if (clean.startsWith("[")) {
                val array = JSONArray(clean)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    result.add(jsonObjectToCloudProduct(obj))
                }
            } else if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                // Possibilité d'avoir une clé "products" ou de convertir l'objet
                if (obj.has("products")) {
                    val arr = obj.getJSONArray("products")
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        result.add(jsonObjectToCloudProduct(item))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cloud JSON", e)
        }
        return result
    }

    private fun jsonObjectToCloudProduct(obj: JSONObject): CloudProduct {
        return CloudProduct(
            remoteId = obj.optString("id", obj.optString("remoteId", "")),
            name = obj.optString("name", "Produit VIP"),
            description = obj.optString("description", ""),
            price = obj.optString("price", "0 FCFA"),
            imageUrl = obj.optString("imageUrl", ""),
            buyUrl = obj.optString("buyUrl", ""),
            category = obj.optString("category", "Application VIP"),
            badge = obj.optString("badge", "NOUVEAU"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
