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
            seedDefaultProductsIfEmpty()
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
                val mode = if (status.isAdmin) "Mode Administrateur" else "Mode Client (Acheteur)"
                onResult(true, "Connexion réussie ($mode)")
            } else {
                onResult(false, "Clé invalide. Clé client : 123 | Clé admin : com.dts")
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

    private suspend fun seedDefaultProductsIfEmpty() {
        val count = storeProductDao.getProductCount()
        if (count == 0) {
            val defaults = listOf(
                StoreProductEntity(
                    name = "Free Fire Injector VIP v12",
                    description = "Menu flottant premium avec auto-headshot 99%, antenna laser, pas de recul et bypass anti-ban permanent.",
                    price = "5 000 FCFA / 10 €",
                    imageUrl = "",
                    buyUrl = "https://wa.me/?text=Bonjour%20Anos%20Store%2C%20je%20veux%20acheter%20Free%20Fire%20Injector%20VIP%20v12",
                    category = "Injecteur VIP",
                    badge = "BEST SELLER",
                    rating = 5.0f,
                    downloadsCount = 3420
                ),
                StoreProductEntity(
                    name = "Headshot AIM Pro Macro",
                    description = "Macro tactile ultra-fluide 120 FPS, stabilisation du viseur et DPI personnalisé pour tous les téléphones Android.",
                    price = "3 500 FCFA / 7 €",
                    imageUrl = "",
                    buyUrl = "https://wa.me/?text=Bonjour%20Anos%20Store%2C%20je%20veux%20acheter%20Headshot%20AIM%20Pro%20Macro",
                    category = "Sensibilité",
                    badge = "POPULAIRE",
                    rating = 4.9f,
                    downloadsCount = 2150
                ),
                StoreProductEntity(
                    name = "Panel Mod Menu Anos FF Exclusif",
                    description = "Le panel ultime créé par Anos FF avec ESP Line, Box, Chams colorés et activation instantanée en jeu.",
                    price = "10 000 FCFA / 18 €",
                    imageUrl = "",
                    buyUrl = "https://wa.me/?text=Bonjour%20Anos%20Store%2C%20je%20veux%20acheter%20Panel%20Mod%20Menu%20Anos%20FF",
                    category = "Mod Menu",
                    badge = "VIP EXCLUSIF",
                    rating = 5.0f,
                    downloadsCount = 4890
                ),
                StoreProductEntity(
                    name = "Pack Skins & Emotes Débloqués",
                    description = "Outil de configuration visuelle pour débloquer toutes les tenues légendaires, armes évolutives et emotes en jeu.",
                    price = "2 500 FCFA / 5 €",
                    imageUrl = "",
                    buyUrl = "https://wa.me/?text=Bonjour%20Anos%20Store%2C%20je%20veux%20acheter%20Pack%20Skins%20VIP",
                    category = "Pack VIP",
                    badge = "NOUVEAU",
                    rating = 4.8f,
                    downloadsCount = 1870
                )
            )
            storeProductDao.insertProducts(defaults)
        }
    }
}
