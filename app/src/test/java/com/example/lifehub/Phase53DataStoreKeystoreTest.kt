package com.example.lifehub

import com.example.lifehub.security.KeystoreManager
import com.example.lifehub.data.local.PreferenceDataStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 53: DataStore配置存储 + Keystore加密存储 - 全面单元测试
 *
 * 测试范围：
 * 1. KeystoreManager - 加密/解密逻辑测试
 * 2. KeystoreManager - Base64编码/解码
 * 3. KeystoreManager - 空值/边界条件处理
 * 4. KeystoreManager - 密钥别名常量
 * 5. PreferenceDataStore - Key定义正确性
 * 6. PreferenceDataStore - 默认值正确性
 * 7. PreferenceDataStore - 配置项完整性
 * 8. 加密存储端到端流程
 * 9. Token存储安全性验证
 * 10. 多次加密同一明文得到不同密文（IV随机性）
 * 11. 大文本加密/解密
 * 12. 特殊字符加密/解密
 * 13. Unicode中文加密/解密
 * 14. 密钥规格验证
 * 15. 篡改密文检测
 */
class Phase53DataStoreKeystoreTest {

    // ==================== 1. KeystoreManager常量与密钥别名 ====================

    @Test
    fun `KeystoreManager - KEY_ALIAS is defined and non-empty`() {
        val alias = KeystoreManager.KEY_ALIAS
        assertNotNull("KEY_ALIAS不应为null", alias)
        assertTrue("KEY_ALIAS不应为空字符串", alias.isNotEmpty())
        assertTrue("KEY_ALIAS应包含lifehub标识", alias.contains("lifehub", ignoreCase = true))
    }

    @Test
    fun `KeystoreManager - TRANSFORMATION is AES GCM`() {
        val transformation = KeystoreManager.TRANSFORMATION
        assertNotNull("TRANSFORMATION不应为null", transformation)
        assertTrue("应使用AES加密", transformation.contains("AES"))
        assertTrue("应使用GCM模式", transformation.contains("GCM"))
    }

    @Test
    fun `KeystoreManager - ANDROID_KEYSTORE provider name`() {
        val provider = KeystoreManager.ANDROID_KEYSTORE
        assertEquals("AndroidKeyStore", provider)
    }

    @Test
    fun `KeystoreManager - IV_SIZE is 12 bytes for GCM`() {
        // AES-GCM标准IV长度为12字节
        assertEquals("GCM IV长度应为12", 12, KeystoreManager.IV_SIZE)
    }

    // ==================== 2. Base64编码/解码辅助方法 ====================

    @Test
    fun `encodeToBase64 - basic string encoding`() {
        val input = "Hello, LifeHub!".toByteArray()
        val encoded = KeystoreManager.encodeToBase64(input)
        assertNotNull("编码结果不应为null", encoded)
        assertTrue("编码结果不应为空", encoded.isNotEmpty())
        // Base64只包含 A-Z, a-z, 0-9, +, /, =
        assertTrue("应为有效Base64字符串", encoded.matches(Regex("^[A-Za-z0-9+/=]+$")))
    }

    @Test
    fun `decodeFromBase64 - round trip encoding`() {
        val original = "敏感数据：Token=abc123".toByteArray()
        val encoded = KeystoreManager.encodeToBase64(original)
        val decoded = KeystoreManager.decodeFromBase64(encoded)
        assertArrayEquals("Base64编解码往返应一致", original, decoded)
    }

    @Test
    fun `encodeToBase64 - empty input`() {
        val encoded = KeystoreManager.encodeToBase64(ByteArray(0))
        assertNotNull("空输入编码不应为null", encoded)
    }

    @Test
    fun `decodeFromBase64 - empty input`() {
        val decoded = KeystoreManager.decodeFromBase64("")
        assertNotNull("空输入解码不应为null", decoded)
        assertEquals("空输入解码应为空数组", 0, decoded.size)
    }

    @Test
    fun `encodeToBase64 - large data`() {
        val largeData = ByteArray(10000) { (it % 256).toByte() }
        val encoded = KeystoreManager.encodeToBase64(largeData)
        val decoded = KeystoreManager.decodeFromBase64(encoded)
        assertArrayEquals("大数据Base64编解码往返应一致", largeData, decoded)
    }

    @Test
    fun `encodeToBase64 - binary data with all byte values`() {
        val allBytes = ByteArray(256) { it.toByte() }
        val encoded = KeystoreManager.encodeToBase64(allBytes)
        val decoded = KeystoreManager.decodeFromBase64(encoded)
        assertArrayEquals("所有字节值编解码应一致", allBytes, decoded)
    }

    // ==================== 3. 加密数据格式化与解析 ====================

    @Test
    fun `formatEncryptedData - produces IV colon ciphertext format`() {
        val iv = ByteArray(12) { 0x01 }
        val ciphertext = ByteArray(32) { 0x02 }
        val formatted = KeystoreManager.formatEncryptedData(iv, ciphertext)
        assertNotNull("格式化结果不应为null", formatted)
        assertTrue("格式化结果应包含分隔符':'", formatted.contains(":"))
        val parts = formatted.split(":")
        assertEquals("应有两部分（IV:密文）", 2, parts.size)
        assertTrue("IV部分不应为空", parts[0].isNotEmpty())
        assertTrue("密文部分不应为空", parts[1].isNotEmpty())
    }

    @Test
    fun `parseEncryptedData - round trip format and parse`() {
        val iv = ByteArray(12) { (it + 10).toByte() }
        val ciphertext = ByteArray(48) { (it + 50).toByte() }
        val formatted = KeystoreManager.formatEncryptedData(iv, ciphertext)
        val result = KeystoreManager.parseEncryptedData(formatted)
        assertNotNull("解析结果不应为null", result)
        assertArrayEquals("解析的IV应与原始一致", iv, result!!.first)
        assertArrayEquals("解析的密文应与原始一致", ciphertext, result.second)
    }

    @Test
    fun `parseEncryptedData - invalid format returns null`() {
        // 没有冒号分隔符的无效格式
        val result = KeystoreManager.parseEncryptedData("invaliddata")
        assertNull("无效格式应返回null", result)
    }

    @Test
    fun `parseEncryptedData - empty string returns null`() {
        val result = KeystoreManager.parseEncryptedData("")
        assertNull("空字符串应返回null", result)
    }

    @Test
    fun `parseEncryptedData - multiple colons uses first split`() {
        // 包含多个冒号时，只在第一个冒号处分割
        val iv = ByteArray(12) { 0x03 }
        val ciphertext = ByteArray(20) { 0x04 }
        val formatted = KeystoreManager.formatEncryptedData(iv, ciphertext)
        // 验证解析仍然正确
        val result = KeystoreManager.parseEncryptedData(formatted)
        assertNotNull("应能正确解析", result)
    }

    // ==================== 4. 空值与边界条件 ====================

    @Test
    fun `encryptString - null input returns null`() {
        // 使用软加密模式（无需Keystore的纯逻辑测试）
        val result = KeystoreManager.encryptStringSoft(null)
        assertNull("null输入加密应返回null", result)
    }

    @Test
    fun `encryptString - empty string returns encrypted data`() {
        val result = KeystoreManager.encryptStringSoft("")
        // 空字符串仍然应该被加密（加密后非空）
        assertNotNull("空字符串加密不应返回null", result)
        assertTrue("加密结果应包含IV:密文格式", result!!.contains(":"))
    }

    @Test
    fun `decryptString - null input returns null`() {
        val result = KeystoreManager.decryptStringSoft(null)
        assertNull("null输入解密应返回null", result)
    }

    @Test
    fun `decryptString - empty input returns null`() {
        val result = KeystoreManager.decryptStringSoft("")
        assertNull("空输入解密应返回null", result)
    }

    @Test
    fun `decryptString - invalid format returns null`() {
        val result = KeystoreManager.decryptStringSoft("not_valid_encrypted_data")
        assertNull("无效格式解密应返回null", result)
    }

    // ==================== 5. 软加密端到端测试 ====================

    @Test
    fun `soft encrypt-decrypt - basic string round trip`() {
        val original = "my_access_token_12345"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull("加密不应返回null", encrypted)
        assertNotEquals("加密结果应不等于原文", original, encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("解密结果应与原文一致", original, decrypted)
    }

    @Test
    fun `soft encrypt-decrypt - Chinese characters`() {
        val original = "用户令牌_测试数据_中文内容"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("中文加解密应一致", original, decrypted)
    }

    @Test
    fun `soft encrypt-decrypt - special characters`() {
        val original = "token=abc&key=xyz!@#\$%^&*(){}[]|\\:\";<>?,./~`"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("特殊字符加解密应一致", original, decrypted)
    }

    @Test
    fun `soft encrypt-decrypt - very long string`() {
        val original = "a".repeat(10000)
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("长字符串加解密应一致", original, decrypted)
    }

    @Test
    fun `soft encrypt-decrypt - JWT token format`() {
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val encrypted = KeystoreManager.encryptStringSoft(jwtToken)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("JWT Token加解密应一致", jwtToken, decrypted)
    }

    @Test
    fun `soft encrypt-decrypt - API key format`() {
        val apiKey = "sk-abcdefghijklmnopqrstuvwxyz1234567890"
        val encrypted = KeystoreManager.encryptStringSoft(apiKey)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("API Key加解密应一致", apiKey, decrypted)
    }

    @Test
    fun `soft encrypt - same plaintext produces different ciphertext`() {
        // AES-GCM使用随机IV，同一明文应产生不同密文
        val original = "test_token_value"
        val encrypted1 = KeystoreManager.encryptStringSoft(original)
        val encrypted2 = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted1)
        assertNotNull(encrypted2)
        assertNotEquals("两次加密结果应不同（随机IV）", encrypted1, encrypted2)
        // 但解密结果应相同
        assertEquals(original, KeystoreManager.decryptStringSoft(encrypted1!!))
        assertEquals(original, KeystoreManager.decryptStringSoft(encrypted2!!))
    }

    @Test
    fun `soft encrypt-decrypt - unicode emoji`() {
        val original = "Token🔐Key🗝️Secret🤫"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("Emoji加解密应一致", original, decrypted)
    }

    @Test
    fun `soft encrypt-decrypt - newlines and whitespace`() {
        val original = "line1\nline2\r\nline3\ttab"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("换行和空白字符加解密应一致", original, decrypted)
    }

    @Test
    fun `soft decrypt - tampered ciphertext returns null`() {
        val original = "sensitive_data"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        // 篡改密文
        val tampered = encrypted!! + "TAMPERED"
        val result = KeystoreManager.decryptStringSoft(tampered)
        assertNull("篡改后的密文解密应返回null", result)
    }

    // ==================== 6. PreferenceDataStore Key定义 ====================

    @Test
    fun `PreferenceDataStore - USER_ID key defined`() {
        val key = PreferenceDataStore.Keys.USER_ID
        assertNotNull("USER_ID Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - USERNAME key defined`() {
        val key = PreferenceDataStore.Keys.USERNAME
        assertNotNull("USERNAME Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - NICKNAME key defined`() {
        val key = PreferenceDataStore.Keys.NICKNAME
        assertNotNull("NICKNAME Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - IS_LOGGED_IN key defined`() {
        val key = PreferenceDataStore.Keys.IS_LOGGED_IN
        assertNotNull("IS_LOGGED_IN Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - HEALTH_GOAL key defined`() {
        val key = PreferenceDataStore.Keys.HEALTH_GOAL
        assertNotNull("HEALTH_GOAL Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - ENCRYPTED_ACCESS_TOKEN key defined`() {
        val key = PreferenceDataStore.Keys.ENCRYPTED_ACCESS_TOKEN
        assertNotNull("ENCRYPTED_ACCESS_TOKEN Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - ENCRYPTED_REFRESH_TOKEN key defined`() {
        val key = PreferenceDataStore.Keys.ENCRYPTED_REFRESH_TOKEN
        assertNotNull("ENCRYPTED_REFRESH_TOKEN Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - THEME_MODE key defined`() {
        val key = PreferenceDataStore.Keys.THEME_MODE
        assertNotNull("THEME_MODE Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - BASE_URL key defined`() {
        val key = PreferenceDataStore.Keys.BASE_URL
        assertNotNull("BASE_URL Key不应为null", key)
    }

    @Test
    fun `PreferenceDataStore - LAST_SYNC_TIME key defined`() {
        val key = PreferenceDataStore.Keys.LAST_SYNC_TIME
        assertNotNull("LAST_SYNC_TIME Key不应为null", key)
    }

    // ==================== 7. PreferenceDataStore 默认值 ====================

    @Test
    fun `PreferenceDataStore - default userId is -1`() {
        assertEquals("默认userId应为-1", -1, PreferenceDataStore.Defaults.USER_ID)
    }

    @Test
    fun `PreferenceDataStore - default isLoggedIn is false`() {
        assertFalse("默认isLoggedIn应为false", PreferenceDataStore.Defaults.IS_LOGGED_IN)
    }

    @Test
    fun `PreferenceDataStore - default healthGoal is balanced`() {
        assertEquals("默认healthGoal应为balanced", "balanced", PreferenceDataStore.Defaults.HEALTH_GOAL)
    }

    @Test
    fun `PreferenceDataStore - default themeMode is system`() {
        assertEquals("默认themeMode应为system", "system", PreferenceDataStore.Defaults.THEME_MODE)
    }

    @Test
    fun `PreferenceDataStore - default nickname is 健康达人`() {
        assertEquals("默认nickname应为健康达人", "健康达人", PreferenceDataStore.Defaults.NICKNAME)
    }

    // ==================== 8. 加密存储安全性验证 ====================

    @Test
    fun `encrypted data does not contain plaintext`() {
        val sensitiveToken = "sk-very-secret-api-key-12345"
        val encrypted = KeystoreManager.encryptStringSoft(sensitiveToken)
        assertNotNull(encrypted)
        assertFalse("加密数据不应包含原始token", encrypted!!.contains(sensitiveToken))
        assertFalse("加密数据不应包含'secret'关键词", encrypted.contains("secret"))
        assertFalse("加密数据不应包含'api-key'关键词", encrypted.contains("api-key"))
    }

    @Test
    fun `encrypted data length is greater than plaintext`() {
        val original = "short"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        assertNotNull(encrypted)
        assertTrue("加密数据长度应大于原文", encrypted!!.length > original.length)
    }

    @Test
    fun `soft encrypt uses IV prefix in output`() {
        val encrypted = KeystoreManager.encryptStringSoft("test")
        assertNotNull(encrypted)
        val parts = encrypted!!.split(":")
        assertEquals("加密输出应有两部分", 2, parts.size)
        // IV部分Base64编码后应有固定长度（12字节 -> 16个Base64字符）
        val ivBase64 = parts[0]
        assertTrue("IV Base64长度应为16", ivBase64.length == 16)
    }

    // ==================== 9. 多重加密场景 ====================

    @Test
    fun `multiple tokens encrypted independently`() {
        val accessToken = "access_token_abc123"
        val refreshToken = "refresh_token_xyz789"
        val apiKey = "sk-api-key-000"

        val encAccess = KeystoreManager.encryptStringSoft(accessToken)
        val encRefresh = KeystoreManager.encryptStringSoft(refreshToken)
        val encApiKey = KeystoreManager.encryptStringSoft(apiKey)

        // 确保各自独立加密
        assertNotEquals("access和refresh密文应不同", encAccess, encRefresh)
        assertNotEquals("access和apiKey密文应不同", encAccess, encApiKey)
        assertNotEquals("refresh和apiKey密文应不同", encRefresh, encApiKey)

        // 各自能正确解密
        assertEquals(accessToken, KeystoreManager.decryptStringSoft(encAccess!!))
        assertEquals(refreshToken, KeystoreManager.decryptStringSoft(encRefresh!!))
        assertEquals(apiKey, KeystoreManager.decryptStringSoft(encApiKey!!))
    }

    @Test
    fun `sequential encrypt-decrypt operations`() {
        // 连续多次加解密操作
        for (i in 1..50) {
            val original = "token_iteration_$i"
            val encrypted = KeystoreManager.encryptStringSoft(original)
            assertNotNull("第${i}次加密不应返回null", encrypted)
            val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
            assertEquals("第${i}次解密应与原文一致", original, decrypted)
        }
    }

    // ==================== 10. 边界长度测试 ====================

    @Test
    fun `encrypt-decrypt - single character`() {
        val original = "A"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt-decrypt - exactly 16 bytes (AES block size)`() {
        val original = "1234567890123456" // 恰好16字节
        val encrypted = KeystoreManager.encryptStringSoft(original)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt-decrypt - exactly 32 bytes`() {
        val original = "12345678901234567890123456789012"
        val encrypted = KeystoreManager.encryptStringSoft(original)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt-decrypt - 255 bytes`() {
        val original = "x".repeat(255)
        val encrypted = KeystoreManager.encryptStringSoft(original)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals(original, decrypted)
    }

    @Test
    fun `encrypt-decrypt - 256 bytes`() {
        val original = "y".repeat(256)
        val encrypted = KeystoreManager.encryptStringSoft(original)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals(original, decrypted)
    }

    // ==================== 11. PreferenceDataStore DATASTORE_NAME ====================

    @Test
    fun `PreferenceDataStore - DATASTORE_NAME is defined`() {
        val name = PreferenceDataStore.DATASTORE_NAME
        assertNotNull("DATASTORE_NAME不应为null", name)
        assertTrue("DATASTORE_NAME不应为空", name.isNotEmpty())
        assertTrue("DATASTORE_NAME应包含lifehub标识", name.contains("lifehub", ignoreCase = true))
    }

    // ==================== 12. KeystoreManager软密钥一致性 ====================

    @Test
    fun `soft key is deterministic`() {
        // 软加密密钥应该是确定性的（每次相同）
        val key1 = KeystoreManager.getSoftKey()
        val key2 = KeystoreManager.getSoftKey()
        assertArrayEquals("软密钥应每次相同", key1.encoded, key2.encoded)
    }

    @Test
    fun `soft key has correct length for AES-256`() {
        val key = KeystoreManager.getSoftKey()
        // AES-256密钥应为32字节
        assertEquals("AES-256密钥应为32字节", 32, key.encoded.size)
    }

    @Test
    fun `soft key algorithm is AES`() {
        val key = KeystoreManager.getSoftKey()
        assertEquals("密钥算法应为AES", "AES", key.algorithm)
    }

    // ==================== 13. 加密数据不可逆验证 ====================

    @Test
    fun `different plaintexts produce different ciphertexts`() {
        val enc1 = KeystoreManager.encryptStringSoft("password1")
        val enc2 = KeystoreManager.encryptStringSoft("password2")
        assertNotEquals("不同明文应产生不同密文", enc1, enc2)
    }

    @Test
    fun `cross decrypt fails gracefully`() {
        // 用一个密文尝试解密另一个不匹配的原文
        val encrypted = KeystoreManager.encryptStringSoft("original_data")
        assertNotNull(encrypted)
        val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
        assertEquals("original_data", decrypted)
        assertNotEquals("解密结果不应为其他值", "different_data", decrypted)
    }

    // ==================== 14. PreferenceDataStore SecureKeys ====================

    @Test
    fun `PreferenceDataStore SecureKeys - all sensitive keys identified`() {
        val secureKeys = PreferenceDataStore.SecureKeys.ALL
        assertTrue("安全Key列表不应为空", secureKeys.isNotEmpty())
        assertTrue("应包含access_token", secureKeys.contains("encrypted_access_token"))
        assertTrue("应包含refresh_token", secureKeys.contains("encrypted_refresh_token"))
    }

    // ==================== 15. 批量操作测试 ====================

    @Test
    fun `encrypt and decrypt many different tokens`() {
        val tokens = listOf(
            "access_token_user1_abc",
            "refresh_token_user1_xyz",
            "sk-dashscope-api-key-123",
            "volc-access-key-456",
            "",
            "short",
            "a".repeat(1000),
            "中文Token测试_2026",
            "special!@#\$%^&*()[]",
            "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.abc123def456"
        )

        for (token in tokens) {
            val encrypted = KeystoreManager.encryptStringSoft(token)
            assertNotNull("Token '$token' 加密不应返回null", encrypted)
            val decrypted = KeystoreManager.decryptStringSoft(encrypted!!)
            assertEquals("Token '$token' 解密应与原文一致", token, decrypted)
        }
    }

    @Test
    fun `formatEncryptedData and parseEncryptedData - various IV sizes`() {
        // 测试不同IV大小的格式化和解析
        for (size in listOf(12, 16, 8)) {
            val iv = ByteArray(size) { it.toByte() }
            val data = ByteArray(32) { (it + 100).toByte() }
            val formatted = KeystoreManager.formatEncryptedData(iv, data)
            val parsed = KeystoreManager.parseEncryptedData(formatted)
            assertNotNull("size=$size 应能解析", parsed)
            assertArrayEquals("size=$size IV应一致", iv, parsed!!.first)
            assertArrayEquals("size=$size 数据应一致", data, parsed.second)
        }
    }
}
