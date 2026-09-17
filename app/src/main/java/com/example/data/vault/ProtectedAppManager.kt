package com.example.data.vault

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.db.ProtectedAppEntity
import com.example.data.sync.BroadcastCloudService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProtectedAppManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val cloudService = BroadcastCloudService(context)

    val protectedAppFlow: Flow<ProtectedAppEntity?> = db.protectedAppDao().getProtectedApp()

    suspend fun getProtectedApp(): ProtectedAppEntity {
        val existing = db.protectedAppDao().getProtectedAppDirect()
        if (existing != null) return existing
        val defaultApp = ProtectedAppEntity()
        db.protectedAppDao().saveProtectedApp(defaultApp)
        return defaultApp
    }

    /**
     * Importe un fichier WebApp/HTML/ZIP/APK pour analyse des métadonnées ou bundle interne.
     */
    suspend fun importApkFromUri(uri: Uri): Result<ProtectedAppEntity> = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
            val destinationApk = File(vaultDir, "protected_in_app.apk")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationApk).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Impossible de lire le fichier sélectionné"))

            val sizeMb = (destinationApk.length() / (1024.0 * 1024.0) * 10.0).toInt() / 10.0

            // Analyser les métadonnées
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                destinationApk.absolutePath,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            )

            var appName = "Jeu/App Protégé VIP"
            var packageName = "com.anos.vip.sandbox"
            var versionName = "v4.0.0 VIP"

            if (packageInfo != null) {
                packageName = packageInfo.packageName ?: packageName
                versionName = packageInfo.versionName ?: versionName
                packageInfo.applicationInfo?.let { appInfo ->
                    appInfo.sourceDir = destinationApk.absolutePath
                    appInfo.publicSourceDir = destinationApk.absolutePath
                    val label = appInfo.loadLabel(packageManager).toString()
                    if (label.isNotBlank()) {
                        appName = label
                    }
                }
            }

            val protectedApp = ProtectedAppEntity(
                id = 1,
                appName = appName,
                packageName = packageName,
                versionName = versionName,
                appType = "IN_APP_CONTAINER",
                localApkPath = destinationApk.absolutePath,
                embeddedAppUrl = "",
                apkFileSizeMb = sizeMb,
                description = "Module VIP sécurisé par Anos FF. Exécution directe dans le booster.",
                isLocked = false,
                lastUpdated = System.currentTimeMillis()
            )

            db.protectedAppDao().saveProtectedApp(protectedApp)
            cloudService.publishProtectedAppToCloud(protectedApp)

            Result.success(protectedApp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Définit une configuration d'application exécutée à l'intérieur du Booster.
     */
    suspend fun updateProtectedAppConfig(
        appName: String,
        packageName: String,
        versionName: String,
        embeddedAppUrl: String,
        description: String,
        isLocked: Boolean
    ): ProtectedAppEntity = withContext(Dispatchers.IO) {
        val current = getProtectedApp()
        val updated = current.copy(
            appName = appName.ifBlank { current.appName },
            packageName = packageName.ifBlank { current.packageName },
            versionName = versionName.ifBlank { current.versionName },
            appType = "IN_APP_CONTAINER",
            embeddedAppUrl = embeddedAppUrl.trim(),
            description = description.ifBlank { current.description },
            isLocked = isLocked,
            lastUpdated = System.currentTimeMillis()
        )
        db.protectedAppDao().saveProtectedApp(updated)
        cloudService.publishProtectedAppToCloud(updated)
        updated
    }

    suspend fun setInstalledAppAsProtected(
        packageName: String,
        appName: String,
        versionName: String
    ): ProtectedAppEntity = withContext(Dispatchers.IO) {
        val current = getProtectedApp()
        val updated = current.copy(
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            appType = "IN_APP_CONTAINER",
            description = "Application configurée par Anos FF. Exécution directe et sécurisée dans le booster.",
            lastUpdated = System.currentTimeMillis()
        )
        db.protectedAppDao().saveProtectedApp(updated)
        cloudService.publishProtectedAppToCloud(updated)
        updated
    }

    suspend fun recordLaunch() = withContext(Dispatchers.IO) {
        db.protectedAppDao().recordLaunch()
    }

    /**
     * Récupère la liste des applications pour suggestions rapides.
     */
    fun getInstalledAppsList(): List<InstalledAppItem> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<InstalledAppItem>()

        for (app in installedApps) {
            val isSystem = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystem || app.packageName.contains("freefire") || app.packageName.contains("pubg") || app.packageName.contains("game")) {
                val name = pm.getApplicationLabel(app).toString()
                val pkg = app.packageName
                val pInfo = try {
                    pm.getPackageInfo(pkg, 0)
                } catch (_: Exception) {
                    null
                }
                val vName = pInfo?.versionName ?: "1.0"
                result.add(InstalledAppItem(appName = name, packageName = pkg, versionName = vName))
            }
        }
        return result.sortedBy { it.appName.lowercase() }
    }
}

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String
)
