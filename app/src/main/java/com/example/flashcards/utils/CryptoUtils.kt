package com.example.flashcards.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val SECRET_KEY = "1234567890123456" // 16 characters for AES-128
    private const val IV = "1234567890123456"         // 16 characters IV

    private val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
    private val ivSpec = IvParameterSpec(IV.toByteArray())

    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            plainText ?: ""
        }
    }

    fun decrypt(cipherText: String?): String {
        if (cipherText.isNullOrEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails, it might be unencrypted data, return as is
            cipherText ?: ""
        }
    }
}
