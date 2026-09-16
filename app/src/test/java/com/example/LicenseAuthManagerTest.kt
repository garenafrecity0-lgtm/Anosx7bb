package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.LicenseAuthManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LicenseAuthManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: LicenseAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = LicenseAuthManager(context)
    }

    @Test
    fun testMasterAdminKeyAuthentication() = runBlocking {
        val result = authManager.validateKey("com.dts")
        assertTrue(result.isAuthenticated)
        assertTrue(result.isAdmin)
        assertEquals("com.dts", result.activeKey)
        assertEquals("ADMIN", result.keyType)
    }

    @Test
    fun testTemporaryKeyGenerationAndValidation() = runBlocking {
        val generatedKey = authManager.generateLicense(durationHours = 24, label = "Test Pass 24h")
        assertTrue(generatedKey.keyString.startsWith("ANOS-V3-") || generatedKey.keyString.startsWith("ANOS-X7-"))
        assertEquals("TEMPORARY", generatedKey.keyType)

        val validationResult = authManager.validateKey(generatedKey.keyString)
        assertTrue(validationResult.isAuthenticated)
        assertEquals(false, validationResult.isAdmin)
    }

    @Test
    fun testLifetimeKeyGenerationAndValidation() = runBlocking {
        val generatedKey = authManager.generateLicense(durationHours = 0, label = "VIP Lifetime")
        assertEquals("LIFETIME", generatedKey.keyType)

        val validationResult = authManager.validateKey(generatedKey.keyString)
        assertTrue(validationResult.isAuthenticated)
        assertEquals("ACCÈS ILLIMITÉ À VIE", validationResult.remainingTimeFormatted)
    }

    @Test
    fun testCaseInsensitiveKeyValidation() = runBlocking {
        val generatedKey = authManager.generateLicense(durationHours = 24, label = "Test Lowercase")
        val lowerKey = generatedKey.keyString.lowercase()

        val validationResult = authManager.validateKey(lowerKey)
        assertTrue(validationResult.isAuthenticated)
        assertEquals(generatedKey.keyString, validationResult.activeKey)
    }

    @Test
    fun testCustomMasterAdminKey() = runBlocking {
        val customKey = "MON_ADMIN_SECRET_2026"
        val setSuccess = authManager.setMasterAdminKey(customKey)
        assertTrue(setSuccess)
        assertEquals(customKey, authManager.getMasterAdminKey())

        // Custom key validates as admin
        val adminResult = authManager.validateKey(customKey)
        assertTrue(adminResult.isAuthenticated)
        assertTrue(adminResult.isAdmin)

        // Custom key with lowercase also works
        val lowerAdminResult = authManager.validateKey(customKey.lowercase())
        assertTrue(lowerAdminResult.isAuthenticated)
        assertTrue(lowerAdminResult.isAdmin)

        // Old default com.dts is now rejected because custom key is active!
        val oldDefaultResult = authManager.validateKey("com.dts")
        assertEquals(false, oldDefaultResult.isAuthenticated)
    }

    @Test
    fun testRevocation() = runBlocking {
        val generatedKey = authManager.generateLicense(durationHours = 24, label = "To Revoke")
        val validFirst = authManager.validateKey(generatedKey.keyString)
        assertTrue(validFirst.isAuthenticated)

        // Revoke key
        authManager.revokeKey(generatedKey.keyString)

        // Must now be rejected
        val rejectedResult = authManager.validateKey(generatedKey.keyString)
        assertEquals(false, rejectedResult.isAuthenticated)
    }

    @Test
    fun testCrossDeviceFriendKeyValidation() = runBlocking {
        // 1. Admin generates a 24h key on Admin phone
        val generatedOnAdminPhone = authManager.generateLicense(durationHours = 24, label = "Clé pour mon ami")
        val keyString = generatedOnAdminPhone.keyString

        // 2. Simulate friend's phone: key does NOT exist in friend's database initially
        authManager.deleteKey(keyString)

        // 3. Friend enters the key on their phone
        val friendResult = authManager.validateKey(keyString)
        assertTrue("La clé doit être validée sur le téléphone de l'ami", friendResult.isAuthenticated)
        assertEquals(false, friendResult.isAdmin)
        assertEquals("TEMPORARY", friendResult.keyType)
        assertTrue("L'expiration doit être dans le futur", friendResult.expiresAt > System.currentTimeMillis())

        // 4. Test friend entering with lowercase and no dashes
        val cleanPayload = keyString.replace("-", "").lowercase()
        val secondFriendResult = authManager.validateKey(cleanPayload)
        assertTrue(secondFriendResult.isAuthenticated)
    }

    @Test
    fun testInvalidOrTamperedKeyRejected() = runBlocking {
        val fakeKey = "ANOS-X7-FAKE-1234-5678"
        val result = authManager.validateKey(fakeKey)
        assertEquals(false, result.isAuthenticated)
    }
}
