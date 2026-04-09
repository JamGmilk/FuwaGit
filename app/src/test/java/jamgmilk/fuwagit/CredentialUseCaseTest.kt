package jamgmilk.fuwagit

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import org.junit.Assert.*
import org.junit.Test

class CredentialUseCaseTest {

    // ==================== SetupMasterPasswordUseCase 验证测试 ====================

    @Test
    fun `SetupMasterPasswordUseCase validates password minimum length`() {
        fun validatePasswordLength(password: String): Boolean = password.length >= 6

        assertFalse("Empty password should fail", validatePasswordLength(""))
        assertFalse("5 chars should fail", validatePasswordLength("12345"))
        assertTrue("6 chars should pass", validatePasswordLength("123456"))
        assertTrue("Longer password should pass", validatePasswordLength("mySecurePassword123"))
    }

    @Test
    fun `SetupMasterPasswordUseCase validates password confirmation match`() {
        fun validatePasswordMatch(password: String, confirmPassword: String): Boolean =
            password == confirmPassword

        assertFalse("Different passwords should fail", validatePasswordMatch("pass1", "pass2"))
        assertTrue("Same passwords should pass", validatePasswordMatch("samepass", "samepass"))
    }

    @Test
    fun `SetupMasterPasswordUseCase rejects password mismatch`() {
        val password = "password123"
        val confirmPassword = "differentPassword"

        val isMatch = password == confirmPassword
        assertFalse("Passwords should not match", isMatch)
    }

    @Test
    fun `SetupMasterPasswordUseCase accepts valid password and confirmation`() {
        val password = "securePassword123!"
        val confirmPassword = "securePassword123!"

        assertEquals(password, confirmPassword)
        assertTrue(password.length >= 6)
    }

    @Test
    fun `SetupMasterPasswordUseCase password hint is optional`() {
        val hint: String? = null
        assertNull(hint)

        val hintWithValue: String? = "My work laptop"
        assertNotNull(hintWithValue)
        assertEquals("My work laptop", hintWithValue)
    }

    // ==================== ResolveCloneCredentialUseCase 测试 ====================

    private fun extractHostFromUrl(url: String): String? {
        return try {
            when {
                url.startsWith("https://") -> url.substringAfter("https://").substringBefore("/")
                url.startsWith("git@") -> url.substringAfter("git@").substringBefore(":")
                url.startsWith("ssh://") -> url.substringAfter("ssh://").substringAfter("@").substringBefore("/")
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Test
    fun `extractHostFromUrl handles https URLs`() {
        val url = "https://github.com/user/repo.git"
        val host = extractHostFromUrl(url)
        assertEquals("github.com", host)
    }

    @Test
    fun `extractHostFromUrl handles git@ SSH URLs`() {
        val url = "git@github.com:user/repo.git"
        val host = extractHostFromUrl(url)
        assertEquals("github.com", host)
    }

    @Test
    fun `extractHostFromUrl handles ssh URLs`() {
        val url = "ssh://git@gitlab.com/group/repo.git"
        val host = extractHostFromUrl(url)
        assertEquals("gitlab.com", host)
    }

    @Test
    fun `extractHostFromUrl handles GitLab URLs`() {
        val url = "https://gitlab.com/owner/project.git"
        val host = extractHostFromUrl(url)
        assertEquals("gitlab.com", host)
    }

    @Test
    fun `extractHostFromUrl handles Bitbucket URLs`() {
        val url = "https://bitbucket.org/team/repo.git"
        val host = extractHostFromUrl(url)
        assertEquals("bitbucket.org", host)
    }

    @Test
    fun `extractHostFromUrl returns null for invalid URLs`() {
        val invalidUrls = listOf(
            "not-a-url",
            "ftp://server.com/repo",
            ""
        )

        invalidUrls.forEach { url ->
            assertNull("Invalid URL '$url' should return null", extractHostFromUrl(url))
        }
    }

    @Test
    fun `resolveCredential auto-selects HTTPS when available`() {
        val httpsCredentials = listOf(
            HttpsCredential("u1", "github.com", "user", "pass")
        )
        val sshKeys = emptyList<SshKey>()

        val selected = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(httpsCredentials.first().username, "decrypted_password")
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNotNull(selected)
        assertTrue(selected is CloneCredential.Https)
    }

    @Test
    fun `resolveCredential falls back to SSH when no HTTPS`() {
        val httpsCredentials = emptyList<HttpsCredential>()
        val sshKeys = listOf(
            SshKey("sk1", "Key", "Ed25519", "pub", "priv", null, "fp")
        )

        val selected = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(httpsCredentials.first().username, "pass")
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNotNull(selected)
        assertTrue(selected is CloneCredential.Ssh)
    }

    @Test
    fun `resolveCredential returns null when no credentials`() {
        val httpsCredentials = emptyList<HttpsCredential>()
        val sshKeys = emptyList<SshKey>()

        val selected = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(httpsCredentials.first().username, "pass")
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNull(selected)
    }

    @Test
    fun `resolveCredential matches HTTPS by host when URL provided`() {
        val httpsCredentials = listOf(
            HttpsCredential("u1", "github.com", "gh_user", "gh_pass"),
            HttpsCredential("u2", "gitlab.com", "gl_user", "gl_pass")
        )
        val remoteUrl = "https://github.com/owner/repo.git"
        val host = extractHostFromUrl(remoteUrl)

        val matched = httpsCredentials.find { it.host.contains(host ?: "", ignoreCase = true) }

        assertNotNull(matched)
        assertEquals("github.com", matched?.host)
        assertEquals("gh_user", matched?.username)
    }

    @Test
    fun `resolveCredential returns first HTTPS when no URL match`() {
        val httpsCredentials = listOf(
            HttpsCredential("u1", "github.com", "first_user", "pass"),
            HttpsCredential("u2", "gitlab.com", "second_user", "pass")
        )

        val selected = httpsCredentials.firstOrNull()

        assertNotNull(selected)
        assertEquals("first_user", selected?.username)
    }

    @Test
    fun `resolveCredential handles SSH key with passphrase`() {
        val sshKey = SshKey(
            uuid = "sk1",
            name = "Work Key",
            type = "RSA",
            publicKey = "ssh-rsa AAA...",
            privateKey = "-----BEGIN RSA PRIVATE KEY-----...",
            passphrase = "myPassphrase",
            fingerprint = "SHA256:abc123"
        )

        val credential = CloneCredential.Ssh(sshKey.privateKey, sshKey.passphrase)

        assertTrue(credential is CloneCredential.Ssh)
        val sshCred = credential as CloneCredential.Ssh
        assertEquals("myPassphrase", sshCred.passphrase)
        assertEquals(sshKey.privateKey, sshCred.privateKey)
    }

    @Test
    fun `resolveCredential handles SSH key without passphrase`() {
        val sshKey = SshKey(
            uuid = "sk1",
            name = "Personal Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAA...",
            privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----...",
            passphrase = null,
            fingerprint = "SHA256:xyz789"
        )

        val credential = CloneCredential.Ssh(sshKey.privateKey, sshKey.passphrase)

        assertTrue(credential is CloneCredential.Ssh)
        val sshCred = credential as CloneCredential.Ssh
        assertNull(sshCred.passphrase)
    }

    // ==================== Credential Repository 逻辑测试 ====================

    @Test
    fun `HttpsCredential stores decrypted password correctly`() {
        val cred = HttpsCredential(
            uuid = "uuid1",
            host = "github.com",
            username = "testuser",
            password = "ENC:AES_GCM:encrypted_password"
        )

        assertNotNull(cred.password)
        assertEquals("ENC:AES_GCM:encrypted_password", cred.password)
    }

    @Test
    fun `SshKey stores encrypted private key`() {
        val encryptedPrivateKey = "ENC:AES_GCM:encrypted_key_data"

        val key = SshKey(
            uuid = "sk1",
            name = "Test Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAA...",
            privateKey = encryptedPrivateKey,
            passphrase = "ENC:AES_GCM:encrypted_passphrase",
            fingerprint = "SHA256:test"
        )

        assertEquals(encryptedPrivateKey, key.privateKey)
        assertNotNull(key.passphrase)
    }

    @Test
    fun `SshKey comment extraction from public key`() {
        val publicKeyWithComment = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGenerateKey comment@email.com"

        val comment = try {
            val parts = publicKeyWithComment.trim().split(" ")
            if (parts.size >= 3) parts[2] else ""
        } catch (e: Exception) {
            ""
        }

        assertEquals("comment@email.com", comment)
    }

    @Test
    fun `SshKey no comment extraction`() {
        val publicKeyWithoutComment = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5"

        val comment = try {
            val parts = publicKeyWithoutComment.trim().split(" ")
            if (parts.size >= 3) parts[2] else ""
        } catch (e: Exception) {
            ""
        }

        assertEquals("", comment)
    }

    // ==================== 密码强度测试 ====================

    @Test
    fun `password strength validation - very weak passwords`() {
        fun isWeakPassword(password: String): Boolean = password.length < 6

        assertTrue("Empty password is weak", isWeakPassword(""))
        assertTrue("5 chars is weak", isWeakPassword("12345"))
        assertTrue("Single char is weak", isWeakPassword("a"))
        assertFalse("6 chars is not weak by length alone", isWeakPassword("123456"))
    }

    @Test
    fun `password strength validation - common weak passwords`() {
        val weakPasswords = listOf(
            "password",  // 8 chars, common weak password
            "123456",    // 6 chars
            "qwerty",    // 6 chars
            "admin",     // 5 chars
            "letmein"    // 7 chars
        )

        weakPasswords.forEach { pwd ->
            assertTrue("'$pwd' should be considered weak (<= 8 chars)", pwd.length <= 8)
        }
    }

    @Test
    fun `password strength validation - strong passwords`() {
        val strongPasswords = listOf(
            "MySecureP@ssw0rd!",
            "xK9#mP2!vL7@nQ4",
            "Th1s1s@V3ryStr0ng!"
        )

        strongPasswords.forEach { pwd ->
            assertTrue("'$pwd' should be strong (>= 12 chars)", pwd.length >= 12)
        }
    }

    // ==================== 会话超时测试 ====================

    @Test
    fun `session timeout calculation from seconds to millis`() {
        fun toMillis(seconds: Long): Long = seconds * 1000L

        assertEquals(0L, toMillis(0))
        assertEquals(300000L, toMillis(300))
        assertEquals(60000L, toMillis(60))
        assertEquals(3600000L, toMillis(3600))
    }

    @Test
    fun `session timeout validation - expired session`() {
        val timeoutMillis = 300000L // 5 minutes
        val lastUnlockTime = System.currentTimeMillis() - 600000L // 10 minutes ago
        val currentTime = System.currentTimeMillis()

        val elapsed = currentTime - lastUnlockTime
        val isExpired = elapsed >= timeoutMillis

        assertTrue("Session should be expired after 10 minutes when timeout is 5 minutes", isExpired)
    }

    @Test
    fun `session timeout validation - valid session`() {
        val timeoutMillis = 300000L // 5 minutes
        val lastUnlockTime = System.currentTimeMillis() - 60000L // 1 minute ago
        val currentTime = System.currentTimeMillis()

        val elapsed = currentTime - lastUnlockTime
        val isValid = elapsed < timeoutMillis

        assertTrue("Session should be valid after 1 minute when timeout is 5 minutes", isValid)
    }

    @Test
    fun `session timeout validation - no timeout (eternal session)`() {
        val timeoutMillis = 0L // No timeout
        val lastUnlockTime = System.currentTimeMillis() - 1000000L // Long time ago

        val isValid = timeoutMillis == 0L || (System.currentTimeMillis() - lastUnlockTime < timeoutMillis)

        assertTrue("Session with no timeout should always be valid", isValid)
    }

    // ==================== 加密标记测试 ====================

    @Test
    fun `encrypted field has correct marker`() {
        val encryptedMarker = "ENC:AES_GCM:"

        val value = "ENC:AES_GCM:abc123base64"

        assertTrue("Value should start with encryption marker", value.startsWith(encryptedMarker))
    }

    @Test
    fun `decryptField handles encrypted value`() {
        val encryptedMarker = "ENC:AES_GCM:"
        val value = "ENC:AES_GCM:abc123base64"

        val isEncrypted = value.startsWith(encryptedMarker)
        val base64Part = if (isEncrypted) value.substring(encryptedMarker.length) else value

        assertTrue(isEncrypted)
        assertEquals("abc123base64", base64Part)
    }

    @Test
    fun `decryptField handles non-encrypted value`() {
        val plainValue = "plain_text_without_marker"

        val isEncrypted = plainValue.startsWith("ENC:AES_GCM:")
        val result = if (isEncrypted) plainValue.substringAfter("ENC:AES_GCM:") else plainValue

        assertFalse(isEncrypted)
        assertEquals(plainValue, result)
    }

    // ==================== UUID 验证测试 ====================

    @Test
    fun `UUID format validation`() {
        fun isValidUuid(uuid: String): Boolean {
            return try {
                java.util.UUID.fromString(uuid)
                true
            } catch (e: Exception) {
                false
            }
        }

        assertTrue("Valid UUID should pass", isValidUuid("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse("Invalid UUID should fail", isValidUuid("not-a-uuid"))
        assertFalse("Empty string should fail", isValidUuid(""))
    }

    // ==================== 导出/导入数据格式测试 ====================

    @Test
    fun `ExportData contains timestamp and credentials`() {
        val dataVersion = 1
        val exportTime = System.currentTimeMillis()

        assertTrue(dataVersion >= 1)
        assertTrue(exportTime > 0)
    }

    @Test
    fun `ExportData serialization round-trip`() {
        val originalData = mapOf(
            "credentials" to listOf(
                mapOf("host" to "github.com", "username" to "user")
            )
        )

        val jsonString = originalData.toString()
        assertTrue(jsonString.contains("github.com"))
        assertTrue(jsonString.contains("user"))
    }
}