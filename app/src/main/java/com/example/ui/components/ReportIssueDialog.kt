package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderGlowing
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SUPPORT_WHATSAPP_NUMBER = "23407071776576"
private const val SUPPORT_WHATSAPP_DISPLAY = "+234 0707 177 6576"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportIssueDialog(
    activeKey: String = "",
    isAdmin: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val categories = listOf(
        "🐛 Bug Affichage",
        "🔑 Problème Clé VIP",
        "⚡ Chute FPS / Lag",
        "❄️ Surchauffe Téléphone",
        "💾 Cache / Stockage",
        "💡 Suggestion / Autre"
    )

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var userMessage by remember { mutableStateOf("") }
    var includeDeviceDiagnostics by remember { mutableStateOf(true) }

    val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    val model = Build.MODEL
    val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    val androidVer = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

    fun buildDiagnosticReport(): String {
        return buildString {
            appendLine("🚨 *[SIGNALEMENT DE PROBLÈME - ANOS V4]* 🚨")
            appendLine("📅 Date : $timestamp")
            appendLine("📂 Catégorie : $selectedCategory")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("📝 *Description de l'utilisateur :*")
            appendLine(userMessage.ifBlank { "Aucune description renseignée." })
            appendLine("━━━━━━━━━━━━━━━━━━━")
            if (includeDeviceDiagnostics) {
                appendLine("📱 *Diagnostic Appareil :*")
                appendLine("• Téléphone : $deviceName")
                appendLine("• Système : $androidVer")
                appendLine("• Version App : Anos v4 (Game Booster VIP)")
                appendLine("• Licence : ${if (isAdmin) "Administrateur Maître" else if (activeKey.isNotBlank()) activeKey else "Non renseignée / En attente"}")
                appendLine("━━━━━━━━━━━━━━━━━━━")
            }
            appendLine("Merci de m'aider à résoudre ce problème ! 🙏")
        }
    }

    fun sendToWhatsApp() {
        val reportText = buildDiagnosticReport()
        try {
            val encodedText = URLEncoder.encode(reportText, "UTF-8")
            val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$SUPPORT_WHATSAPP_NUMBER&text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, directUri)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback to generic browser / wa.me link
                val encodedText = URLEncoder.encode(reportText, "UTF-8")
                val fallbackUri = Uri.parse("https://wa.me/$SUPPORT_WHATSAPP_NUMBER?text=$encodedText")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                clipboard.setText(AnnotatedString(reportText))
                Toast.makeText(context, "WhatsApp non trouvé. Rapport copié dans le presse-papier !", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkBackground)
                .border(BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.5f)), RoundedCornerShape(24.dp)),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberCrimson.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, CyberCrimson.copy(alpha = 0.4f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = CyberCrimson,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SIGNALER UN PROBLÈME",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Support WhatsApp Développeur",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WhatsApp Contact Card Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberNeonGreen.copy(alpha = 0.08f))
                        .border(BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = CyberNeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Contact WhatsApp Officiel :",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = SUPPORT_WHATSAPP_DISPLAY,
                                color = CyberNeonGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category selection
                Text(
                    text = "CATÉGORIE DU PROBLÈME",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) CyberCyan else DarkBorder
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) CyberCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User description input
                Text(
                    text = "DÉCRIVEZ VOTRE PROBLÈME",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    placeholder = {
                        Text(
                            text = "Expliquez ce qui ne fonctionne pas ou votre question (ex: ma clé ne s'active pas, Free Fire lag après 10 minutes...)",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("issue_description_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CyberGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Données techniques jointes",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (includeDeviceDiagnostics) "Inclus" else "Désactivé",
                                color = if (includeDeviceDiagnostics) CyberNeonGreen else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { includeDeviceDiagnostics = !includeDeviceDiagnostics }
                                    .padding(4.dp)
                            )
                        }

                        if (includeDeviceDiagnostics) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Appareil : $deviceName\n• Système : $androidVer\n• App : Anos v4 Game Booster\n• Licence : ${if (activeKey.isNotBlank()) activeKey else "Non connectée"}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Send Button to WhatsApp
                Button(
                    onClick = { sendToWhatsApp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("send_whatsapp_report_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberNeonGreen,
                        contentColor = Color(0xFF00240B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENVOYER SUR WHATSAPP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary copy button
                OutlinedButton(
                    onClick = {
                        val report = buildDiagnosticReport()
                        clipboard.setText(AnnotatedString(report))
                        Toast.makeText(context, "Rapport de diagnostic copié !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copier le rapport complet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
