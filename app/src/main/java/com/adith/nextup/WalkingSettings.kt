package com.adith.nextup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The user's credential never enters app state, logs, backups, or the APK. */
class WalkingSettings(context: Context) {
    private val prefs = context.getSharedPreferences("walking", Context.MODE_PRIVATE)
    private val alias = "${context.packageName}.walking-key"
    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun key(): String? = try {
        val encoded = prefs.getString("key", null)
        if (encoded == null) null else {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        }
    } catch (_: Exception) { null }
    fun save(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        val encrypted = cipher.iv + cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("key", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putLong("last_attempt", 0).putLong("retry_after", 0).commit())
    }
    fun clear() { check(prefs.edit().clear().commit()) }
    var lastAttempt: Long
        get() = prefs.getLong("last_attempt", 0)
        set(value) { prefs.edit().putLong("last_attempt", value).apply() }
    var retryAfter: Long
        get() = prefs.getLong("retry_after", 0)
        set(value) { prefs.edit().putLong("retry_after", value).apply() }
}
