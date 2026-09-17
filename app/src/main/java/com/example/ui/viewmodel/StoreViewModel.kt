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
import com.example.data.db.AppDatabase
import com.example.data.db.StoreProductEntity
import com.example.util.NotificationHelper
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

                // Notification réelle envoyée aux clients avec les détails du nouveau produit
                NotificationHelper.postNewProductNotification(
                    context = getApplication(),
                    productName = product.name,
                    productPrice = product.price,
                    productCategory = product.category,
                    productDescription = product.description
                )

                onComplete(true, "Application ajoutée à la boutique avec succès !")
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
                onComplete(true, "Application mise à jour avec succès !")
            } catch (e: Exception) {
                onComplete(false, "Erreur : ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(id: Long, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                storeProductDao.deleteProduct(id)
                onComplete(true, "Application supprimée de la boutique.")
            } catch (e: Exception) {
                onComplete(false, "Erreur lors de la suppression.")
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
