package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthStatus
import com.example.data.auth.LicenseAuthManager
import com.example.data.cloud.CloudStoreSyncService
import com.example.data.cloud.SyncStatus
import com.example.data.db.AppDatabase
import com.example.data.db.StoreProductEntity
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactConfig(
    val supportName: String = "Anos FF Support Officiel",
    val whatsappNumber: String = "+33700000000",
    val whatsappLink: String = "https://wa.me/message/ANOSSTORE",
    val telegramUsername: String = "@AnosFF_Official",
    val telegramLink: String = "https://t.me/AnosStoreVIP",
    val discordLink: String = "https://discord.gg/anosstore",
    val email: String = "contact.anosstore@gmail.com",
    val workingHours: String = "7j/7 • 24h/24 • Réponse rapide"
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val storeProductDao = db.storeProductDao()
    private val authManager = LicenseAuthManager(application)
    private val cloudSyncService = CloudStoreSyncService.getInstance(application)

    val syncStatus: StateFlow<SyncStatus> = cloudSyncService.syncStatus
    val isCloudConnected: StateFlow<Boolean> = cloudSyncService.isCloudConnected

    private val _authStatus = MutableStateFlow(AuthStatus())
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val _isAuthChecking = MutableStateFlow(true)
    val isAuthChecking: StateFlow<Boolean> = _isAuthChecking.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tous")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _contactConfig = MutableStateFlow(ContactConfig())
    val contactConfig: StateFlow<ContactConfig> = _contactConfig.asStateFlow()

    val allProducts: StateFlow<List<StoreProductEntity>> = storeProductDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredProducts: StateFlow<List<StoreProductEntity>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "Tous" || product.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            try {
                storeProductDao.deleteDemoProducts()
            } catch (e: Exception) {
                // Ignore
            }
            checkSavedAuth()
            // Première synchronisation immédiate au lancement pour charger les produits du Cloud
            cloudSyncService.syncFromCloud(silent = false)

            // Boucle de synchronisation automatique toutes les 25 secondes pour détecter
            // immédiatement les ajouts de produits par l'admin et faire sonner les téléphones des clients !
            while (true) {
                delay(25_000)
                try {
                    cloudSyncService.syncFromCloud(silent = false)
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun checkSavedAuth() {
        _isAuthChecking.value = true
        try {
            val current = authManager.checkCurrentAuth()
            _authStatus.value = current
        } catch (e: Exception) {
            _authStatus.value = AuthStatus(isAuthenticated = false)
        } finally {
            _isAuthChecking.value = false
        }
    }

    fun login(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val status = authManager.validateKey(key)
            _authStatus.value = status
            if (status.isAuthenticated) {
                val mode = if (status.isAdmin) "Mode Administrateur" else "Mode Client"
                onResult(true, "Connexion réussie ($mode)")
            } else {
                onResult(false, "Clé d'accès incorrecte. Accès Client : 123")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
            _authStatus.value = AuthStatus(isAuthenticated = false)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addProduct(
        name: String,
        description: String,
        price: String,
        imageUrl: String,
        buyUrl: String,
        category: String,
        badge: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (name.isBlank() || price.isBlank()) {
            onComplete(false, "Veuillez renseigner au moins le nom et le prix.")
            return
        }

        viewModelScope.launch {
            try {
                val finalBuyUrl = if (buyUrl.isNotBlank() && !buyUrl.startsWith("http://") && !buyUrl.startsWith("https://")) {
                    "https://$buyUrl"
                } else {
                    buyUrl
                }

                val product = StoreProductEntity(
                    name = name.trim(),
                    description = description.trim(),
                    price = price.trim(),
                    imageUrl = imageUrl.trim(),
                    buyUrl = finalBuyUrl.trim(),
                    category = if (category.isBlank()) "Application VIP" else category.trim(),
                    badge = if (badge.isBlank()) "NOUVEAU" else badge.trim()
                )
                storeProductDao.insertProduct(product)

                // Publier automatiquement vers le Cloud pour que TOUS les autres clients le reçoivent
                val allCurrent = storeProductDao.getAllProductsList()
                cloudSyncService.publishCatalogToCloud(allCurrent)

                // Notification locale immédiate pour confirmation
                NotificationHelper.postNewProductNotification(
                    context = getApplication(),
                    productName = product.name,
                    productPrice = product.price,
                    productCategory = product.category,
                    productDescription = product.description
                )

                onComplete(true, "Application ajoutée et synchronisée dans le Cloud avec succès !")
            } catch (e: Exception) {
                onComplete(false, "Erreur : ${e.localizedMessage}")
            }
        }
    }

    fun updateProduct(
        id: Long,
        name: String,
        description: String,
        price: String,
        imageUrl: String,
        buyUrl: String,
        category: String,
        badge: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (name.isBlank() || price.isBlank()) {
            onComplete(false, "Veuillez renseigner au moins le nom et le prix.")
            return
        }

        viewModelScope.launch {
            try {
                val finalBuyUrl = if (buyUrl.isNotBlank() && !buyUrl.startsWith("http://") && !buyUrl.startsWith("https://")) {
                    "https://$buyUrl"
                } else {
                    buyUrl
                }

                storeProductDao.updateProduct(
                    id = id,
                    name = name.trim(),
                    description = description.trim(),
                    price = price.trim(),
                    imageUrl = imageUrl.trim(),
                    buyUrl = finalBuyUrl.trim(),
                    category = if (category.isBlank()) "Application VIP" else category.trim(),
                    badge = if (badge.isBlank()) "POPULAIRE" else badge.trim()
                )

                // Synchroniser la mise à jour avec le Cloud
                val allCurrent = storeProductDao.getAllProductsList()
                cloudSyncService.publishCatalogToCloud(allCurrent)

                onComplete(true, "Application mise à jour et synchronisée avec succès !")
            } catch (e: Exception) {
                onComplete(false, "Erreur : ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(id: Long, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                storeProductDao.deleteProduct(id)
                val allCurrent = storeProductDao.getAllProductsList()
                cloudSyncService.publishCatalogToCloud(allCurrent)
                onComplete(true, "Application supprimée et synchronisée avec succès.")
            } catch (e: Exception) {
                onComplete(false, "Erreur lors de la suppression.")
            }
        }
    }

    /**
     * Force une synchronisation manuelle depuis le Cloud
     */
    fun syncFromCloudNow(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cloudSyncService.syncFromCloud(silent = false)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                onComplete(true, "Synchronisation réussie ($count produits à jour)")
            } else {
                onComplete(false, "Échec de connexion : ${result.exceptionOrNull()?.localizedMessage ?: "Inconnu"}")
            }
        }
    }

    /**
     * Force l'envoi de la base locale vers le Cloud (utile pour l'administrateur)
     */
    fun pushCatalogToCloudNow(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = storeProductDao.getAllProductsList()
            val result = cloudSyncService.publishCatalogToCloud(current)
            if (result.isSuccess) {
                onComplete(true, "${current.size} produit(s) mis en ligne dans le Cloud pour tous les clients !")
            } else {
                onComplete(false, "Échec d'envoi Cloud : ${result.exceptionOrNull()?.localizedMessage ?: "Inconnu"}")
            }
        }
    }

    fun exportCatalogJson(): String {
        return cloudSyncService.exportCatalogJson(allProducts.value)
    }

    fun importCatalogJson(json: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = cloudSyncService.importCatalogJson(json)
            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                val all = storeProductDao.getAllProductsList()
                cloudSyncService.publishCatalogToCloud(all)
                onComplete(true, "$count produit(s) importés et partagés en ligne !")
            } else {
                onComplete(false, "Format invalide : ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun openExternalBuyLink(context: Context, buyUrl: String, productName: String) {
        if (buyUrl.isBlank()) {
            // Si aucun lien spécifique, ouvrir le support WhatsApp / Telegram par défaut
            val fallbackLink = "https://wa.me/?text=Bonjour%20Anos%20Store%2C%20je%20souhaite%20acheter%20l%27application%20%3A%20${Uri.encode(productName)}"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackLink))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Impossible d'ouvrir le lien d'achat.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val finalUrl = if (!buyUrl.startsWith("http://") && !buyUrl.startsWith("https://")) {
                "https://$buyUrl"
            } else {
                buyUrl
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Lien externe invalide : $buyUrl", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllProducts(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                storeProductDao.clearAllProducts()
                onComplete(true, "Tous les produits ont été supprimés.")
            } catch (e: Exception) {
                onComplete(false, "Erreur lors du vidage de la boutique.")
            }
        }
    }
}
