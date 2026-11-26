package com.raulito.gastospersonales.security

import android.content.Context
import android.util.Base64

/**
 * Stores authentication tokens, user session metadata, and the SQLCipher database passphrase
 * encrypted with AES-256-GCM via [CryptoManager] (backed by Android Keystore).
 */
class SecureTokenStorage(
    context: Context,
    private val crypto: CryptoManager
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        expiresAtEpochSec: Long
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, crypto.encrypt(accessToken))
            .putString(KEY_REFRESH_TOKEN, crypto.encrypt(refreshToken))
            .putString(KEY_USER_ID, crypto.encrypt(userId))
            .putString(KEY_USER_EMAIL, crypto.encrypt(email))
            .putLong(KEY_EXPIRES_AT, expiresAtEpochSec)
            .apply()
    }

    fun getAccessToken(): String? = decryptOrNull(KEY_ACCESS_TOKEN)

    fun getRefreshToken(): String? = decryptOrNull(KEY_REFRESH_TOKEN)

    fun getUserId(): String? = decryptOrNull(KEY_USER_ID)

    fun getUserEmail(): String? = decryptOrNull(KEY_USER_EMAIL)

    fun isSessionValid(): Boolean {
        val token = getAccessToken()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val nowSec = System.currentTimeMillis() / 1000L
        return !token.isNullOrBlank() && (expiresAt == 0L || expiresAt > nowSec)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun saveCloudConfig(url: String, anonKey: String) {
        prefs.edit()
            .putString(KEY_SUPABASE_URL, url.trim())
            .putString(KEY_SUPABASE_KEY, crypto.encrypt(anonKey.trim()))
            .apply()
    }

    fun getCustomSupabaseUrl(): String = prefs.getString(KEY_SUPABASE_URL, "").orEmpty()

    fun getCustomSupabaseKey(): String = decryptOrNull(KEY_SUPABASE_KEY).orEmpty()

    /**
     * Retrieves or generates a persistent 32-byte passphrase for SQLCipher,
     * stored encrypted using the Android Keystore master key.
     */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val encrypted = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (!encrypted.isNullOrBlank()) {
            val decoded = crypto.decrypt(encrypted)
            if (decoded.isNotBlank()) {
                return Base64.decode(decoded, Base64.NO_WRAP)
            }
        }
        val rawBytes = crypto.generateRandomPassphraseBytes(32)
        val encoded = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, crypto.encrypt(encoded)).apply()
        return rawBytes
    }

    private fun decryptOrNull(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        val decrypted = crypto.decrypt(encrypted)
        return decrypted.ifBlank { null }
    }

    companion object {
        private const val PREFS_NAME = "gastos_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_USER_ID = "enc_user_id"
        private const val KEY_USER_EMAIL = "enc_user_email"
        private const val KEY_EXPIRES_AT = "expires_at_epoch_sec"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_DB_PASSPHRASE = "enc_db_passphrase"
        private const val KEY_SUPABASE_URL = "custom_supabase_url"
        private const val KEY_SUPABASE_KEY = "enc_custom_supabase_key"
    }
}
