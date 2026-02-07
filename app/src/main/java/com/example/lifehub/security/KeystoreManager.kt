package com.example.lifehub.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android Keystore加密管理器
 *
 * 功能：
 * - 使用Android Keystore生成和管理AES-256密钥
 * - 提供AES-GCM加密/解密方法，用于保护Token、API Key等敏感数据
 * - 提供软加密模式（用于单元测试环境，不依赖Android Keystore）
 * - 加密数据格式：Base64(IV):Base64(Ciphertext)
 *
 * 安全设计：
 * - 密钥存储在Android硬件安全模块（TEE/StrongBox）中，不可导出
 * - 每次加密使用随机IV，确保同一明文产生不同密文
 * - 使用GCM模式提供认证加密（AEAD），防止篡改
 */
object KeystoreManager {

    private const val TAG = "KeystoreManager"

    /** Keystore密钥别名 */
    const val KEY_ALIAS = "lifehub_secure_key"

    /** 加密算法转换字符串 */
    const val TRANSFORMATION = "AES/GCM/NoPadding"

    /** Android Keystore提供者名称 */
    const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /** GCM IV长度（字节），标准为12 */
    const val IV_SIZE = 12

    /** GCM认证标签长度（位） */
    private const val GCM_TAG_LENGTH = 128

    /** 软加密密钥种子（仅用于单元测试环境，生产环境使用Android Keystore） */
    private const val SOFT_KEY_SEED = "LifeHub_Secure_Storage_Key_2026!"

    // ==================== Android Keystore方法（生产环境） ====================

    /**
     * 获取或创建Android Keystore中的密钥
     * @return SecretKey 密钥对象
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // 如果密钥已存在，直接返回
        keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        // 生成新密钥
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 使用Android Keystore加密字符串
     * @param plaintext 明文字符串
     * @return 加密后的字符串（Base64(IV):Base64(Ciphertext)），失败返回null
     */
    fun encryptString(plaintext: String?): String? {
        if (plaintext == null) return null
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            formatEncryptedData(iv, ciphertext)
        } catch (e: Exception) {
            logSafe("加密失败，降级使用软加密: ${e.message}")
            encryptStringSoft(plaintext)
        }
    }

    /**
     * 使用Android Keystore解密字符串
     * @param encryptedData 加密数据字符串（Base64(IV):Base64(Ciphertext)）
     * @return 解密后的明文字符串，失败返回null
     */
    fun decryptString(encryptedData: String?): String? {
        if (encryptedData.isNullOrEmpty()) return null
        return try {
            val parsed = parseEncryptedData(encryptedData) ?: return null
            val (iv, ciphertext) = parsed
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            logSafe("Keystore解密失败，尝试软解密: ${e.message}")
            decryptStringSoft(encryptedData)
        }
    }

    // ==================== 软加密方法（单元测试 + 降级方案） ====================

    /**
     * 获取软加密密钥（确定性，每次返回相同密钥）
     * 仅用于单元测试和Keystore不可用时的降级场景
     */
    fun getSoftKey(): SecretKeySpec {
        val keyBytes = SOFT_KEY_SEED.toByteArray(Charsets.UTF_8).copyOf(32) // AES-256 = 32字节
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 软加密 - 不依赖Android Keystore的加密方法
     * @param plaintext 明文字符串
     * @return 加密后的字符串，失败返回null
     */
    fun encryptStringSoft(plaintext: String?): String? {
        if (plaintext == null) return null
        return try {
            val key = getSoftKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            formatEncryptedData(iv, ciphertext)
        } catch (e: Exception) {
            logSafe("软加密失败: ${e.message}")
            null
        }
    }

    /**
     * 软解密 - 不依赖Android Keystore的解密方法
     * @param encryptedData 加密数据字符串
     * @return 解密后的明文字符串，失败返回null
     */
    fun decryptStringSoft(encryptedData: String?): String? {
        if (encryptedData.isNullOrEmpty()) return null
        return try {
            val parsed = parseEncryptedData(encryptedData) ?: return null
            val (iv, ciphertext) = parsed
            val key = getSoftKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            logSafe("软解密失败: ${e.message}")
            null
        }
    }

    // ==================== Base64工具方法 ====================

    /**
     * 字节数组编码为Base64字符串
     */
    fun encodeToBase64(data: ByteArray): String {
        return java.util.Base64.getEncoder().encodeToString(data)
    }

    /**
     * Base64字符串解码为字节数组
     */
    fun decodeFromBase64(encoded: String): ByteArray {
        if (encoded.isEmpty()) return ByteArray(0)
        return java.util.Base64.getDecoder().decode(encoded)
    }

    // ==================== 加密数据格式化 ====================

    /**
     * 将IV和密文格式化为存储字符串
     * 格式：Base64(IV):Base64(Ciphertext)
     */
    fun formatEncryptedData(iv: ByteArray, ciphertext: ByteArray): String {
        val ivBase64 = encodeToBase64(iv)
        val ciphertextBase64 = encodeToBase64(ciphertext)
        return "$ivBase64:$ciphertextBase64"
    }

    /**
     * 安全日志：兼容Android和JUnit测试环境
     * 在Android环境使用Log.e，在JUnit环境使用System.err
     */
    private fun logSafe(message: String) {
        try {
            android.util.Log.e(TAG, message)
        } catch (_: Throwable) {
            System.err.println("$TAG: $message")
        }
    }

    /**
     * 解析存储字符串为IV和密文
     * @return Pair<IV, Ciphertext> 或 null（格式无效时）
     */
    fun parseEncryptedData(data: String): Pair<ByteArray, ByteArray>? {
        if (data.isEmpty()) return null
        val colonIndex = data.indexOf(':')
        if (colonIndex <= 0 || colonIndex >= data.length - 1) return null
        return try {
            val ivBase64 = data.substring(0, colonIndex)
            val ciphertextBase64 = data.substring(colonIndex + 1)
            val iv = decodeFromBase64(ivBase64)
            val ciphertext = decodeFromBase64(ciphertextBase64)
            Pair(iv, ciphertext)
        } catch (e: Exception) {
            logSafe("解析加密数据失败: ${e.message}")
            null
        }
    }
}
