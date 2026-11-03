package com.company.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps values in SharedPreferences. A key of Android Keystore encrypts each value with AES-GCM.
 *
 * The backup rules of the app leave out the file. The Keystore key does not go to a backup, so a restored file cannot
 * be read anyway.
 *
 * @param context Context of the app.
 */
internal class AndroidSecureStore(context: Context) : SecureStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching { decrypt(stored) }.getOrElse {
            // 키가 사라졌거나 값이 깨졌으면 지운다. 사용자는 다시 로그인한다.
            remove(key)
            null
        }
    }

    override fun write(key: String, value: String) {
        prefs.edit().putString(key, encrypt(value)).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        /** File name of the values. The backup rules of androidApp use this name. */
        const val PREFS_NAME = "magic_session"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "magic_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128

        /** Length of the IV that Android Keystore makes for AES-GCM. */
        const val IV_BYTES = 12
    }
}
