package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import org.junit.Assert.*
import org.junit.Test

class CredentialModelsTest {

    // ==================== HttpsCredential 测试 ====================

    @Test
    fun `HttpsCredential creation with all fields`() {
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
    fun `HttpsCredential default timestamps are recent`() {
        val beforeCreate = System.currentTimeMillis() - 1000
        val credential = HttpsCredential(
            uuid = "uuid-1",
            host = "gitlab.com",
            username = "user",
            password = "pass"
        )
        val afterCreate = System.currentTimeMillis() + 1000

        assertTrue(credential.createdAt >= beforeCreate)
        assertTrue(credential.createdAt <= afterCreate)
        assertTrue(credential.updatedAt >= beforeCreate)
        assertTrue(credential.updatedAt <= afterCreate)
    }

    @Test
    fun `HttpsCredential data class equality`() {
        val cred1 = HttpsCredential("uuid1", "github.com", "user1", "pass1")
        val cred2 = HttpsCredential("uuid1", "github.com", "user1", "pass1")
        val cred3 = HttpsCredential("uuid2", "github.com", "user1", "pass1")

        assertEquals(cred1, cred2)
        assertNotEquals(cred1, cred3)
    }

    @Test
    fun `HttpsCredential for different hosts`() {
        val githubCred = HttpsCredential("u1", "github.com", "gh_user", "gh_pass")
        val gitlabCred = HttpsCredential("u2", "gitlab.com", "gl_user", "gl_pass")
        val bitbucketCred = HttpsCredential("u3", "bitbucket.org", "bb_user", "bb_pass")

        assertEquals("github.com", githubCred.host)
        assertEquals("gitlab.com", gitlabCred.host)
        assertEquals("bitbucket.org", bitbucketCred.host)
    }

    // ==================== SshKey 测试 ====================

    @Test
    fun `SshKey creation with passphrase`() {
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
        assertNotNull(sshKey.passphrase)
        assertEquals("mypassphrase", sshKey.passphrase)
        assertEquals("SHA256:abc123def456", sshKey.fingerprint)
    }

    @Test
    fun `SshKey creation without passphrase`() {
        val sshKey = SshKey(
            uuid = "ssh-uuid-2",
            name = "Personal Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAAA...",
            privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----\n...",
            passphrase = null,
            fingerprint = "SHA256:noPassKey"
        )

        assertNull(sshKey.passphrase)
        assertEquals("Ed25519", sshKey.type)
    }

    @Test
    fun `SshKey comment extraction from public key with comment`() {
        val sshKey = SshKey(
            uuid = "u1",
            name = "Test Key",
            type = "RSA",
            publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC user@host",
            privateKey = "private-key-data",
            fingerprint = "SHA256:fp1"
        )

        assertEquals("user@host", sshKey.comment)
    }

    @Test
    fun `SshKey comment extraction from public key without comment`() {
        val sshKey = SshKey(
            uuid = "u2",
            name = "Minimal Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI",
            privateKey = "private",
            fingerprint = "SHA256:fp2"
        )

        assertEquals("", sshKey.comment)
    }

    @Test
    fun `SshKey comment extraction with email-style comment`() {
        val sshKey = SshKey(
            uuid = "u3",
            name = "Key With Email",
            type = "RSA",
            publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC developer@example.com",
            privateKey = "private",
            fingerprint = "SHA256:fp3"
        )

        assertEquals("developer@example.com", sshKey.comment)
    }

    @Test
    fun `SshKey comment extraction handles malformed public key`() {
        val sshKey = SshKey(
            uuid = "u4",
            name = "Bad Key",
            type = "RSA",
            publicKey = "invalid-public-key-format",
            privateKey = "private",
            fingerprint = "SHA256:fp4"
        )

        assertEquals("", sshKey.comment)
    }

    @Test
    fun `SshKey comment extraction handles empty public key`() {
        val sshKey = SshKey(
            uuid = "u5",
            name = "Empty Key",
            type = "RSA",
            publicKey = "",
            privateKey = "private",
            fingerprint = "SHA256:fp5"
        )

        assertEquals("", sshKey.comment)
    }

    @Test
    fun `SshKey data class equality`() {
        val key1 = SshKey("u1", "Key1", "RSA", "pub", "priv", "pass", "fp1")
        val key2 = SshKey("u1", "Key1", "RSA", "pub", "priv", "pass", "fp1")
        val key3 = SshKey("u2", "Key2", "RSA", "pub", "priv", "pass", "fp2")

        assertEquals(key1, key2)
        assertNotEquals(key1, key3)
    }

    @Test
    fun `SshKey default passphrase is null`() {
        val sshKey = SshKey(
            uuid = "u6",
            name = "Default Passphrase Key",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAA...",
            privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----...",
            fingerprint = "SHA256:default"
        )

        assertNull(sshKey.passphrase)
    }

    @Test
    fun `SshKey default createdAt is recent`() {
        val beforeCreate = System.currentTimeMillis() - 1000
        val sshKey = SshKey(
            uuid = "u7",
            name = "Key With Default Time",
            type = "Ed25519",
            publicKey = "ssh-ed25519 AAA...",
            privateKey = "private",
            fingerprint = "SHA256:time"
        )
        val afterCreate = System.currentTimeMillis() + 1000

        assertTrue(sshKey.createdAt >= beforeCreate)
        assertTrue(sshKey.createdAt <= afterCreate)
    }

    // ==================== CloneCredential 密封类测试 ====================

    @Test
    fun `CloneCredential Https subtype`() {
        val httpsCred = CloneCredential.Https(
            username = "myuser",
            password = "mypassword"
        )

        assertTrue(httpsCred is CloneCredential)
        assertTrue(httpsCred is CloneCredential.Https)
        assertEquals("myuser", httpsCred.username)
        assertEquals("mypassword", httpsCred.password)
    }

    @Test
    fun `CloneCredential Ssh subtype`() {
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
    fun `CloneCredential Ssh without passphrase`() {
        val sshCred = CloneCredential.Ssh(
            privateKey = "private-key-no-pass",
            passphrase = null
        )

        assertNull(sshCred.passphrase)
    }

    @Test
    fun `CloneCredential when expression matching`() {
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

        assertEquals(2, httpsCount)
        assertEquals(2, sshCount)
    }

    @Test
    fun `CloneCredential Https equals with same values`() {
        val cred1 = CloneCredential.Https("user", "pass")
        val cred2 = CloneCredential.Https("user", "pass")
        assertEquals(cred1, cred2)
    }

    @Test
    fun `CloneCredential Ssh equals with same values`() {
        val cred1 = CloneCredential.Ssh("key", "pass")
        val cred2 = CloneCredential.Ssh("key", "pass")
        assertEquals(cred1, cred2)
    }

    @Test
    fun `CloneCredential Https and Ssh are different types`() {
        val httpsCred = CloneCredential.Https("user", "pass")
        val sshCred = CloneCredential.Ssh("key", null)
        assertNotEquals(httpsCred, sshCred)
    }
}