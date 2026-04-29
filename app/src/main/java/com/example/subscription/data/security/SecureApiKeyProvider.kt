package com.example.subscription.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.subscription.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * Provider для безопасного хранения API ключей.
 *
 * Использует Android Keystore для генерации AES-ключа и шифрования API ключа.
 * Зашифрованный ключ хранится в SharedPreferences.
 *
 * ⚠️ Важно: это защита от casual inspection (decompiling, strings.xml).
 * Полностью защитить ключ в клиентском приложении невозможно.
 * Для production используйте серверный прокси.
 */
class SecureApiKeyProvider(context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "api_key_encryption_alias"
        private const val PREFS_NAME = "secure_prefs"
        private const val ENCRYPTED_KEY = "encrypted_api_key"
        private const val IV_KEY = "encryption_iv"
        private const val GCM_TAG_LENGTH = 128
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Возвращает API key, расшифрованный из Keystore.
     * При первом запуске берет из BuildConfig, шифрует и сохраняет.
     */
    fun getApiKey(): String {
        val encrypted = prefs.getString(ENCRYPTED_KEY, null)
        val iv = prefs.getString(IV_KEY, null)

        return if (encrypted != null && iv != null) {
            decrypt(encrypted, iv)
        } else {
            val plainKey = BuildConfig.GEMINI_API_KEY
            if (plainKey.isNotBlank() && plainKey != "YOUR_GEMINI_API_KEY_HERE") {
                encryptAndSave(plainKey)
            }
            plainKey
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encryptAndSave(plainText: String) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val encrypted = Base64.encodeToString(cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)

        prefs.edit()
            .putString(ENCRYPTED_KEY, encrypted)
            .putString(IV_KEY, iv)
            .apply()
    }

    private fun decrypt(encryptedBase64: String, ivBase64: String): String {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            // Если расшифровка не удалась (например, ключ был удален из Keystore),
            // возвращаем из BuildConfig
            BuildConfig.GEMINI_API_KEY
        }
    }
}
