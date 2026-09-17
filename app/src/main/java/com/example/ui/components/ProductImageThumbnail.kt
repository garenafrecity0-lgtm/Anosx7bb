package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated

@Composable
fun ProductImageThumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    cornerRadius: Dp = 14.dp
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(DarkSurfaceElevated, Color(0xFF161E2E))
                )
            )
            .border(1.dp, CyberGold.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Preset icons
            imageUrl.startsWith("preset:") -> {
                val presetName = imageUrl.removePrefix("preset:")
                val (icon, color) = getPresetIconAndColor(presetName)
                Icon(
                    imageVector = icon,
                    contentDescription = presetName,
                    tint = color,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
            // Web URL or Device Content / File URI
            imageUrl.startsWith("http://", ignoreCase = true) ||
            imageUrl.startsWith("https://", ignoreCase = true) ||
            imageUrl.startsWith("content://", ignoreCase = true) ||
            imageUrl.startsWith("file://", ignoreCase = true) -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Image du produit",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(size * 0.4f),
                                strokeWidth = 2.dp,
                                color = CyberGold
                            )
                        }
                    },
                    error = {
                        // Fallback icon if URL failed to load
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = CyberGold,
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                )
            }
            // Default icon if blank or unrecognised
            else -> {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = CyberGold,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
    }
}

fun getPresetIconAndColor(preset: String): Pair<ImageVector, Color> {
    return when (preset.lowercase()) {
        "crown", "vip", "etoile" -> Pair(Icons.Default.Star, CyberGold)
        "rocket", "injector", "injecteur" -> Pair(Icons.Default.RocketLaunch, CyberGold)
        "crosshair", "aim", "headshot" -> Pair(Icons.Default.MyLocation, CyberCyan)
        "flash", "sensi", "speed" -> Pair(Icons.Default.Bolt, CyberGold)
        "shield", "antiban", "securite" -> Pair(Icons.Default.Security, CyberCyan)
        "gamepad", "mod", "menu" -> Pair(Icons.Default.SportsEsports, Color(0xFF00FFCC))
        "flame", "fire", "feu" -> Pair(Icons.Default.LocalFireDepartment, Color(0xFFFF5722))
        "diamond", "diamant" -> Pair(Icons.Default.Diamond, CyberCyan)
        else -> Pair(Icons.Default.RocketLaunch, CyberGold)
    }
}
