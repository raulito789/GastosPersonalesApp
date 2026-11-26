package com.raulito.gastospersonales.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Abstraction for symmetric encryption so security logic can be unit-tested on JVM
 * as well as executed with hardware-backed Android Keystore on Android devices.
 */
interface TextCipher {
    fun encrypt(plainText: String): String
    fun decrypt(cipherTextBase64: String): String
}

/**
 * AES-256-GCM encryption manager backed by Android KeyStore.
 * Used to encrypt sensitive local expense notes, auth tokens, and the SQLCipher passphrase.
 */
class CryptoManager(
    private val keyAlias: String = "gastos_master_aes_key"
) : TextCipher {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingEntry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(1 + iv.size + encrypted.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(cipherTextBase64: String): String {
        if (cipherTextBase64.isEmpty()) return ""
        return runCatching {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val ivSize = combined[0].toInt() and 0xFF
            val iv = combined.copyOfRange(1, 1 + ivSize)
            val encrypted = combined.copyOfRange(1 + ivSize, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }

    fun generateRandomPassphraseBytes(length: Int = 32): ByteArray {
        return ByteArray(length).also { SecureRandom().nextBytes(it) }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
