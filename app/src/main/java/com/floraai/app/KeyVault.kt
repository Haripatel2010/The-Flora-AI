package com.floraai.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeyVault {
    private const val STORE = "flora_secure_keys"
    private const val KEY_ALIAS = "flora_master_key"

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )

        return generator.generateKey()
    }

    fun save(context: Context, name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val data = Base64.encodeToString(encrypted, Base64.NO_WRAP)

        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit()
            .putString("${name}_iv", iv)
            .putString(name, data)
            .apply()
    }

    fun get(context: Context, name: String): String? {
        val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val iv = prefs.getString("${name}_iv", null) ?: return null
        val data = prefs.getString(name, null) ?: return null

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )

            String(
                cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)),
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            null
        }
    }

    fun delete(context: Context, name: String) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit()
            .remove(name)
            .remove("${name}_iv")
            .apply()
    }
}
