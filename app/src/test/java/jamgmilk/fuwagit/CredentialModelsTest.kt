package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.credential.*
import org.junit.Assert.*
import org.junit.Test

class CredentialModelsTest {

    // ==================== HttpsCredential 测试 ====================

    @Test
    fun `test HttpsCredential creation with all fields`() {
        val credential = HttpsCredential(
            uuid = "uuid-1234",
            host = "github.com",
            username = "testuser",
            password = "secret123",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        assertEquals("uuid-1234", credential.uuid)
        assertEquals("github.com", credential.host)
        assertEquals("testuser", credential.username)
        assertEquals("secret123", credential.password)
        assertEquals(1000L, credential.createdAt)
        assertEquals(2000L, credential.updatedAt)
    }

    @Test
    fun `test HttpsCredential default timestamps`() {
        val beforeCreate = System.currentTimeMillis()
        val credential = HttpsCredential(
            uuid = "uuid-1",
            host = "gitlab.com",
            username = "user",
            password = "pass"
        )
        val afterCreate = System.currentTimeMillis()

        assertTrue("createdAt should be >= beforeCreate", credential.createdAt >= beforeCreate)
        assertTrue("createdAt should be <= afterCreate", credential.createdAt <= afterCreate)
        assertTrue("updatedAt should be >= beforeCreate", credential.updatedAt >= beforeCreate)
        assertTrue("updatedAt should be <= afterCreate", credential.updatedAt <= afterCreate)
    }

    @Test
    fun `test HttpsCredential data class equality`() {
        val cred1 = HttpsCredential("uuid1", "github.com", "user1", "pass1")
        val cred2 = HttpsCredential("uuid1", "github.com", "user1", "pass1")
        val cred3 = HttpsCredential("uuid2", "github.com", "user1", "pass1")

        assertEquals("Same fields should be equal", cred1, cred2)
        assertNotEquals("Different uuid should not be equal", cred1, cred3)
    }

    @Test
    fun `test HttpsCredential for different hosts`() {
        val githubCred = HttpsCredential("u1", "github.com", "gh_user", "gh_pass")
        val gitlabCred = HttpsCredential("u2", "gitlab.com", "gl_user", "gl_pass")
        val bitbucketCred = HttpsCredential("u3", "bitbucket.org", "bb_user", "bb_pass")

        assertEquals("github.com", githubCred.host)
        assertEquals("gitlab.com", gitlabCred.host)
        assertEquals("bitbucket.org", bitbucketCred.host)
    }

    // ==================== SshKey 测试 ====================

    @Test
    fun `test SshKey creation with passphrase`() {
        val sshKey = SshKey(
            uuid = "ssh-uuid-1",
            name = "Work Key",
            type = "RSA",
            publicKey = "ssh-rsa AAAAB3Nza... user@host",
            privateKey = "-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----",
            passphrase = "mypassphrase",
            fingerprint = "SHA256:abc123def456",
            createdAt = 3000L
        )

        assertEquals("ssh-uuid-1", sshKey.uuid)
        assertEquals("Work Key", sshKey.name)
        assertEquals("RSA", sshKey.type)
        assertNotNull("Should have passphrase", sshKey.passphrase)
        assertEquals("mypassphrase", sshKey.passphrase)
        assertEquals("SHA256:abc123def456", sshKey.fingerprint)
    }

    @Test
    fun `test SshKey creation without passphrase`() {
        val sshKey = SshKey(
            uuid = "ssh-uuid-2",
            name = "Personal Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAAA...",
            privateKey = "-----BEGIN PRIVATE KEY-----\n...",
            passphrase = null,
            fingerprint = "SHA256:noPassKey"
        )

        assertNull("No passphrase should be null", sshKey.passphrase)
        assertEquals("Ed25519", sshKey.type)
    }

    @Test
    fun `test SshKey comment extraction from public key`() {
        val keyWithComment = SshKey(
            uuid = "u1",
            name = "Test Key",
            type = "RSA",
            publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC user@host",
            privateKey = "private-key-data",
            fingerprint = "SHA256:fp1"
        )

        assertEquals("user@host", keyWithComment.comment)
    }

    @Test
    fun `test SshKey no comment in public key`() {
        val keyWithoutComment = SshKey(
            uuid = "u2",
            name = "Minimal Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI",
            privateKey = "private",
            fingerprint = "SHA256:fp2"
        )

        assertEquals("", keyWithoutComment.comment)
    }

    @Test
    fun `test SshKey data class equality`() {
        val key1 = SshKey("u1", "Key1", "RSA", "pub", "priv", "pass", "fp1")
        val key2 = SshKey("u1", "Key1", "RSA", "pub", "priv", "pass", "fp1")
        val key3 = SshKey("u2", "Key2", "RSA", "pub", "priv", "pass", "fp2")

        assertEquals(key1, key2)
        assertNotEquals(key1, key3)
    }

    // ==================== CloneCredential 密封类测试 ====================

    @Test
    fun `test CloneCredential Https subtype`() {
        val httpsCred = CloneCredential.Https(
            username = "myuser",
            password = "mypassword"
        )

        assertTrue("Should be CloneCredential", httpsCred is CloneCredential)
        assertTrue("Should be Https subtype", httpsCred is CloneCredential.Https)
        assertEquals("myuser", httpsCred.username)
        assertEquals("mypassword", httpsCred.password)
    }

    @Test
    fun `test CloneCredential Ssh subtype`() {
        val sshCred = CloneCredential.Ssh(
            privateKey = "-----BEGIN RSA PRIVATE KEY-----\nkeydata\n-----END RSA PRIVATE KEY-----",
            passphrase = "secret"
        )

        assertTrue(sshCred is CloneCredential)
        assertTrue(sshCred is CloneCredential.Ssh)
        assertNotNull(sshCred.privateKey)
        assertEquals("secret", sshCred.passphrase)
    }

    @Test
    fun `test CloneCredential Ssh without passphrase`() {
        val sshCred = CloneCredential.Ssh(
            privateKey = "private-key-no-pass",
            passphrase = null
        )

        assertNull(sshCred.passphrase)
    }

    @Test
    fun `test CloneCredential when expression matching`() {
        val credentials: List<CloneCredential> = listOf(
            CloneCredential.Https("user1", "pass1"),
            CloneCredential.Ssh("key1", null),
            CloneCredential.Https("user2", "pass2"),
            CloneCredential.Ssh("key2", "passphrase")
        )

        var httpsCount = 0
        var sshCount = 0

        credentials.forEach { cred ->
            when (cred) {
                is CloneCredential.Https -> httpsCount++
                is CloneCredential.Ssh -> sshCount++
            }
        }

        assertEquals("Should have 2 HTTPS credentials", 2, httpsCount)
        assertEquals("Should have 2 SSH credentials", 2, sshCount)
    }

    // ==================== 凭据匹配和选择逻辑测试 ====================

    @Test
    fun `test match HTTPS credential by host for GitHub URL`() {
        val credentials = listOf(
            HttpsCredential("u1", "github.com", "gh_user", "gh_pass"),
            HttpsCredential("u2", "gitlab.com", "gl_user", "gl_pass"),
            HttpsCredential("u3", "bitbucket.org", "bb_user", "bb_pass")
        )

        val remoteUrl = "https://github.com/owner/repo.git"
        val host = remoteUrl.removePrefix("https://").substringBefore('/')
        val matched = credentials.find { it.host == host }

        assertNotNull("Should match github.com credential", matched)
        assertEquals("github.com", matched?.host)
        assertEquals("gh_user", matched?.username)
    }

    @Test
    fun `test match HTTPS credential by host for GitLab URL`() {
        val credentials = listOf(
            HttpsCredential("u1", "github.com", "gh_user", "gh_pass"),
            HttpsCredential("u2", "gitlab.com", "gl_user", "gl_pass")
        )

        val remoteUrl = "https://gitlab.com/group/project.git"
        val host = remoteUrl.removePrefix("https://").substringBefore('/')
        val matched = credentials.find { it.host == host }

        assertNotNull(matched)
        assertEquals("gitlab.com", matched?.host)
    }

    @Test
    fun `test no match when credential host not in list`() {
        val credentials = listOf(
            HttpsCredential("u1", "github.com", "user", "pass")
        )

        val remoteUrl = "https://unknown-host.com/repo.git"
        val host = remoteUrl.removePrefix("https://").substringBefore('/')
        val matched = credentials.find { it.host == host }

        assertNull("Should not find match for unknown host", matched)
    }

    @Test
    fun `test auto-select clone credential prefers HTTPS over SSH`() {
        val httpsCredentials = listOf(
            HttpsCredential("u1", "github.com", "user", "pass")
        )
        val sshKeys = listOf(
            SshKey("sk1", "My Key", "RSA", "pub", "priv", null, "fp")
        )

        val selected = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(httpsCredentials.first().username, httpsCredentials.first().password)
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNotNull(selected)
        assertTrue("Should prefer HTTPS", selected is CloneCredential.Https)
    }

    @Test
    fun `test auto-select falls back to SSH when no HTTPS available`() {
        val httpsCredentials = emptyList<HttpsCredential>()
        val sshKeys = listOf(
            SshKey("sk1", "Fallback Key", "Ed25519", "pub", "priv", null, "fp")
        )

        val selected = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(httpsCredentials.first().username, httpsCredentials.first().password)
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNotNull(selected)
        assertTrue("Should fall back to SSH", selected is CloneCredential.Ssh)
    }

    @Test
    fun `test auto-select returns null when both lists empty`() {
        val selected = runCatching {
            val httpsCredentials = emptyList<HttpsCredential>()
            val sshKeys = emptyList<SshKey>()
            if (httpsCredentials.isNotEmpty()) {
                CloneCredential.Https(httpsCredentials.first().username, httpsCredentials.first().password)
            } else if (sshKeys.isNotEmpty()) {
                CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
            } else {
                null
            }
        }.getOrNull()

        assertNull(selected)
    }

    // ==================== SSH URL 解析测试 ====================

    @Test
    fun `test identify SSH URL format git@`() {
        val sshUrls = listOf(
            "git@github.com:user/repo.git",
            "git@gitlab.com:group/project.git",
            "git@bitbucket.org:team/repo.git"
        )

        sshUrls.forEach { url ->
            assertTrue("'$url' should be SSH format",
                url.contains("@") && url.startsWith("git@"))
        }
    }

    @Test
    fun `test identify SSH URL format ssh protocol`() {
        val urls = listOf(
            "ssh://git@github.com/user/repo.git",
            "ssh://git@gitlab.com/group/project.git"
        )

        urls.forEach { url ->
            assertTrue("'$url' should be SSH format",
                url.startsWith("ssh://"))
        }
    }

    @Test
    fun `test identify HTTPS URL format`() {
        val httpsUrls = listOf(
            "https://github.com/user/repo.git",
            "https://gitlab.com/group/project",
            "https://bitbucket.org/team/repo.git"
        )

        httpsUrls.forEach { url ->
            assertTrue("'$url' should be HTTPS format",
                url.startsWith("https://"))
        }
    }

    @Test
    fun `test extract username from SSH URL`() {
        val url = "git@github.com:user/repo.git"
        val atIndex = url.indexOf('@')
        val username = url.substring(0, atIndex)

        assertEquals("git", username)
    }

    // ==================== 凭据与 URL 类型对应关系测试 ====================

    @Test
    fun `test HTTPS URLs should use HttpsCredential`() {
        val url = "https://github.com/user/repo.git"
        val isHttps = url.startsWith("https://") && !url.contains("@") || 
                      (url.startsWith("https://") && !url.startsWith("ssh://"))

        assertTrue(isHttps)
    }

    @Test
    fun `test SSH URLs should use SshCredential`() {
        val url = "git@github.com:user/repo.git"
        val isSsh = url.startsWith("git@") || url.startsWith("ssh://")

        assertTrue(isSsh)
    }

    @Test
    fun `test determine credential type from URL`() {
        fun getCredentialType(url: String): String {
            return when {
                url.startsWith("git@") || url.startsWith("ssh://") -> "SSH"
                url.startsWith("https://") -> "HTTPS"
                else -> "UNKNOWN"
            }
        }

        assertEquals("SSH", getCredentialType("git@github.com:user/repo"))
        assertEquals("SSH", getCredentialType("ssh://git@github.com/user/repo"))
        assertEquals("HTTPS", getCredentialType("https://github.com/user/repo"))
        assertEquals("UNKNOWN", getCredentialType("ftp://server.com/repo"))
    }
}
