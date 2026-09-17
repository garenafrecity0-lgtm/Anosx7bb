package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.StoreProductEntity
import com.example.ui.components.ProductImageThumbnail
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun AdminStoreScreen(
    viewModel: StoreViewModel,
    onBackToStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<StoreProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<StoreProductEntity?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // =========================================================================
            // TOP BAR ADMIN
            // =========================================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back & Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackToStore,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, DarkBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = CyberGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PANEL ADMIN",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberGold)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "GESTION",
                                        color = DarkBackground,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Mettre en ligne & Gérer les applications",
                                color = CyberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Actions en haut à droite
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onBackToStore)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BOUTIQUE",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Bouton Actualiser Cloud
                        IconButton(
                            onClick = {
                                viewModel.syncFromCloudNow { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Synchroniser Cloud",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { showLogoutConfirmDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, CyberCrimson.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Déconnexion",
                                tint = CyberCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // BANDEAU CLOUD DIRECT ADMIN
            // =========================================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberGold.copy(alpha = 0.08f))
                        .border(1.dp, CyberGold.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCloudConnected) CyberNeonGreen else CyberCrimson)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCloudConnected) "Serveur Cloud Actif • Tous les clients synchronisés" else "Mode Hors-ligne",
                            color = if (isCloudConnected) CyberNeonGreen else CyberCrimson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Diffusion instantanée",
                        color = CyberGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // =========================================================================
            // ADMIN ACTION BANNER : AJOUTER UNE NOUVELLE APPLICATION
            // =========================================================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(DarkSurfaceElevated, Color(0xFF1B2234))
                            )
                        )
                        .border(
                            BorderStroke(1.dp, CyberGold.copy(alpha = 0.6f)),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "METTRE EN LIGNE UNE APP",
                                color = CyberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${products.size} En vente",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ajouter une application à la boutique",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Importez une image depuis votre galerie ou un lien URL, fixez le prix et configurez le bouton d'achat direct.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_new_product_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGold,
                                contentColor = DarkBackground
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+ METTRE EN LIGNE UNE NOUVELLE APPLICATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bouton d'envoi / synchronisation Cloud forcée pour tous les clients
                        OutlinedButton(
                            onClick = {
                                viewModel.pushCatalogToCloudNow { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = CyberCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DIFFUSER TOUT LE CATALOGUE DANS LE CLOUD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // LISTE DES APPLICATIONS EN GESTION ADMIN
            // =========================================================================
            item {
                Text(
                    text = "APPLICATIONS ACTUELLEMENT EN VENTE (${products.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = CyberGold,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Votre boutique est vide",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Aucune application n'est mise en vente pour l'instant.\nCliquez sur le bouton ci-dessus pour ajouter votre premier produit.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    AdminProductCard(
                        product = product,
                        onEdit = { productToEdit = product },
                        onDelete = { productToDelete = product },
                        onTestLink = {
                            viewModel.openExternalBuyLink(context, product.buyUrl, product.name)
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // DIALOGUE AJOUTER / MODIFIER APPLICATION
    // =========================================================================
    if (showAddDialog) {
        ProductFormDialog(
            title = "Mettre en ligne une application",
            initialProduct = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, desc, price, img, buyUrl, cat, badge ->
                viewModel.addProduct(name, desc, price, img, buyUrl, cat, badge) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) showAddDialog = false
                }
            }
        )
    }

    productToEdit?.let { product ->
        ProductFormDialog(
            title = "Modifier l'application",
            initialProduct = product,
            onDismiss = { productToEdit = null },
            onSave = { name, desc, price, img, buyUrl, cat, badge ->
                viewModel.updateProduct(product.id, name, desc, price, img, buyUrl, cat, badge) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) productToEdit = null
                }
            }
        )
    }

    // =========================================================================
    // DIALOGUE CONFIRMATION SUPPRESSION
    // =========================================================================
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = CyberCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supprimer l'application ?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Êtes-vous sûr de vouloir retirer « ${product.name} » de la boutique ?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product.id) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            productToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCrimson)
                ) {
                    Text("SUPPRIMER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("ANNULER", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // =========================================================================
    // DIALOGUE CONFIRMATION DECONNEXION
    // =========================================================================
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Déconnexion Admin", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous fermer la session Administrateur ?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCrimson)
                ) {
                    Text("DÉCONNEXION", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("ANNULER", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun AdminProductCard(
    product: StoreProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestLink: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val isRecentlyAdded = (System.currentTimeMillis() - product.createdAt) < (48L * 60 * 60 * 1000)
            val displayBadge = if (isRecentlyAdded) "NOUVEAU" else product.badge
            val isGreenNouveau = displayBadge.equals("NOUVEAU", ignoreCase = true) || isRecentlyAdded

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(product.category, color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isGreenNouveau) CyberNeonGreen.copy(alpha = 0.2f)
                                else CyberGold.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isGreenNouveau) CyberNeonGreen.copy(alpha = 0.8f)
                                else CyberGold.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = displayBadge,
                            color = if (isGreenNouveau) CyberNeonGreen else CyberGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Prix en direct
                Text(
                    text = product.price,
                    color = CyberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Icon/Image + Name + Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RENDER VÉRITABLE IMAGE DU PRODUIT
                ProductImageThumbnail(
                    imageUrl = product.imageUrl,
                    size = 50.dp,
                    cornerRadius = 12.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lien d'achat configuré
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (product.buyUrl.isBlank()) "Lien par défaut (WhatsApp)" else product.buyUrl,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Admin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tester le lien d'achat ↗",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onTestLink)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = CyberGold, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = CyberCrimson, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: StoreProductEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, price: String, img: String, buyUrl: String, cat: String, badge: String) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var price by remember { mutableStateOf(initialProduct?.price ?: "") }
    var buyUrl by remember { mutableStateOf(initialProduct?.buyUrl ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Injecteur VIP") }
    var badge by remember { mutableStateOf(initialProduct?.badge ?: "NOUVEAU") }
    var imageUrl by remember { mutableStateOf(initialProduct?.imageUrl ?: "") }

    // Launcher pour ouvrir la galerie / sélection d'image du téléphone
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                imageUrl = uri.toString()
            }
        }
    )

    val presetIcons = listOf(
        Pair("🚀 Injecteur", "preset:rocket"),
        Pair("👑 VIP Gold", "preset:crown"),
        Pair("🎯 Aimbot / Headshot", "preset:crosshair"),
        Pair("⚡ Sensibilité Pro", "preset:flash"),
        Pair("🛡️ Anti-Ban Shield", "preset:shield"),
        Pair("🎮 Mod Menu", "preset:gamepad"),
        Pair("🔥 Flame", "preset:flame"),
        Pair("💎 Diamant", "preset:diamond")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =========================================================================
                // SECTION IMAGE DU PRODUIT (AVEC GALERIE, URL ET ICÔNES VIP)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, CyberGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "IMAGE DU PRODUIT",
                                color = CyberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            if (imageUrl.isNotBlank()) {
                                Text(
                                    text = "Effacer",
                                    color = CyberCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { imageUrl = "" }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live preview row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ProductImageThumbnail(
                                imageUrl = imageUrl,
                                size = 60.dp,
                                cornerRadius = 12.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (imageUrl.isBlank()) "Aucune image choisie" else "Aperçu de l'image",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (imageUrl.isBlank()) "Choisissez une photo de la galerie ou une icône" else "Image active prête pour la boutique",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Bouton Ouvrir la Galerie
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberGold,
                                        contentColor = DarkBackground
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Choisir une photo (Galerie)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 2: Saisie de lien URL Web d'image
                        Text(
                            text = "Ou coller un lien URL d'image direct :",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = if (imageUrl.startsWith("preset:")) "" else imageUrl,
                            onValueChange = { imageUrl = it },
                            placeholder = {
                                Text("Ex: https://i.imgur.com/image.png", color = TextMuted, fontSize = 11.sp)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 3: Icônes rapides VIP prédéfinies
                        Text(
                            text = "Ou choisir une icône VIP prête à l'emploi :",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetIcons.forEach { (label, presetVal) ->
                                val isSelected = imageUrl == presetVal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberGold.copy(alpha = 0.25f) else DarkBackground)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberGold else DarkBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { imageUrl = presetVal }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CyberGold else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nom de l'application
                Text("Nom de l'application *", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Free Fire Injector VIP v15", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Prix
                Text("Prix de vente *", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    placeholder = { Text("Ex: 5 000 FCFA ou 10 €", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Lien externe d'achat
                Text("Lien externe du bouton Acheter (WhatsApp, Telegram, Paiement) *", color = CyberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = buyUrl,
                    onValueChange = { buyUrl = it },
                    placeholder = { Text("Ex: https://wa.me/message/... ou lien direct", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                Text("Description de l'application", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Détails des fonctionnalités, avantages VIP, etc.", color = TextMuted, fontSize = 12.sp) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Catégorie
                Text("Catégorie", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    placeholder = { Text("Injecteur VIP, Sensibilité, Mod Menu, Pack VIP...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badge
                Text("Badge VIP (Auto: 'NOUVEAU' en vert pendant 48h)", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("NOUVEAU", "BEST SELLER", "VIP EXCLUSIF", "POPULAIRE").forEach { b ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (badge.equals(b, ignoreCase = true)) {
                                        if (b == "NOUVEAU") CyberNeonGreen.copy(alpha = 0.25f)
                                        else CyberGold.copy(alpha = 0.25f)
                                    } else DarkSurfaceElevated
                                )
                                .border(
                                    1.dp,
                                    if (badge.equals(b, ignoreCase = true)) {
                                        if (b == "NOUVEAU") CyberNeonGreen else CyberGold
                                    } else DarkBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { badge = b }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = b,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.equals(b, ignoreCase = true)) {
                                    if (b == "NOUVEAU") CyberNeonGreen else CyberGold
                                } else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = badge,
                    onValueChange = { badge = it },
                    placeholder = { Text("POPULAIRE, BEST SELLER, NOUVEAU, VIP EXCLUSIF...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSave(name, description, price, imageUrl, buyUrl, category, badge)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGold,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("ENREGISTRER & METTRE EN LIGNE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
