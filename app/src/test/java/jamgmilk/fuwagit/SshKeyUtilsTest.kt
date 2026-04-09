package jamgmilk.fuwagit

import jamgmilk.fuwagit.core.util.calculateFingerprint
import jamgmilk.fuwagit.core.util.detectSshKeyType
import jamgmilk.fuwagit.core.util.generateSshKeyPair
import jamgmilk.fuwagit.core.util.validatePrivateKey
import org.junit.Assert.*
import org.junit.Test

class SshKeyUtilsTest {

    @Test
    fun `generateSshKeyPair generates Ed25519 key pair by default`() {
        val (publicKey, privateKey) = generateSshKeyPair("Ed25519", "test@example.com")

        assertNotEquals("", publicKey)
        assertNotEquals("", privateKey)
        assertTrue(publicKey.startsWith("ssh-ed25519 "))
        assertTrue(privateKey.contains("-----BEGIN OPENSSH PRIVATE KEY-----"))
        assertTrue(privateKey.contains("-----END OPENSSH PRIVATE KEY-----"))
    }

    @Test
    fun `generateSshKeyPair generates RSA key pair`() {
        val (publicKey, privateKey) = generateSshKeyPair("RSA", "test@example.com")

        assertNotEquals("", publicKey)
        assertNotEquals("", privateKey)
        assertTrue(publicKey.startsWith("ssh-rsa "))
        assertTrue(privateKey.contains("-----BEGIN RSA PRIVATE KEY-----"))
        assertTrue(privateKey.contains("-----END RSA PRIVATE KEY-----"))
    }

    @Test
    fun `generateSshKeyPair with comment includes comment in public key`() {
        val comment = "user@hostname"
        val (publicKey, _) = generateSshKeyPair("Ed25519", comment)

        assertTrue(publicKey.endsWith(comment))
    }

    @Test
    fun `generateSshKeyPair with empty comment generates valid key`() {
        val (publicKey, privateKey) = generateSshKeyPair("Ed25519", "")

        assertNotEquals("", publicKey)
        assertNotEquals("", privateKey)
        assertTrue(publicKey.startsWith("ssh-ed25519 "))
    }

    @Test
    fun `calculateFingerprint returns SHA256 format`() {
        val (publicKey, _) = generateSshKeyPair("Ed25519")

        val fingerprint = calculateFingerprint(publicKey)

        assertTrue(fingerprint.startsWith("SHA256:"))
    }

    @Test
    fun `calculateFingerprint is consistent for same key`() {
        val (publicKey, _) = generateSshKeyPair("Ed25519")

        val fingerprint1 = calculateFingerprint(publicKey)
        val fingerprint2 = calculateFingerprint(publicKey)

        assertEquals(fingerprint1, fingerprint2)
    }

    @Test
    fun `calculateFingerprint different for different keys`() {
        val (publicKey1, _) = generateSshKeyPair("Ed25519")
        val (publicKey2, _) = generateSshKeyPair("Ed25519")

        val fingerprint1 = calculateFingerprint(publicKey1)
        val fingerprint2 = calculateFingerprint(publicKey2)

        assertNotEquals(fingerprint1, fingerprint2)
    }

    @Test
    fun `calculateFingerprint returns unknown for invalid key`() {
        val invalidKey = "ssh-ed25519 invalid-base64-content"
        val fingerprint = calculateFingerprint(invalidKey)

        assertEquals("unknown", fingerprint)
    }

    @Test
    fun `calculateFingerprint handles key without comment`() {
        val (publicKey, _) = generateSshKeyPair("Ed25519", "")

        val fingerprint = calculateFingerprint(publicKey)

        assertTrue(fingerprint.startsWith("SHA256:"))
    }

    @Test
    fun `detectSshKeyType detects RSA key`() {
        val (_, privateKey) = generateSshKeyPair("RSA")

        val keyType = detectSshKeyType(privateKey)

        assertEquals("RSA", keyType)
    }

    @Test
    fun `detectSshKeyType detects Ed25519 key`() {
        val (_, privateKey) = generateSshKeyPair("Ed25519")

        val keyType = detectSshKeyType(privateKey)

        assertEquals("Ed25519", keyType)
    }

    @Test
    fun `detectSshKeyType returns Unknown for invalid key`() {
        val invalidKey = "-----BEGIN PRIVATE KEY-----\ninvalid\n-----END PRIVATE KEY-----"

        val keyType = detectSshKeyType(invalidKey)

        assertEquals("Unknown", keyType)
    }

    @Test
    fun `detectSshKeyType handles key without PEM format`() {
        val keyType = detectSshKeyType("not-a-pem-key")

        assertEquals("Unknown", keyType)
    }

    @Test
    fun `validatePrivateKey validates Ed25519 key`() {
        val (_, privateKey) = generateSshKeyPair("Ed25519")

        val (isValid, keyType) = validatePrivateKey(privateKey)

        assertTrue(isValid)
        assertEquals("Ed25519", keyType)
    }

    @Test
    fun `validatePrivateKey validates RSA key`() {
        val (_, privateKey) = generateSshKeyPair("RSA")

        val (isValid, keyType) = validatePrivateKey(privateKey)

        assertTrue(isValid)
        assertEquals("RSA", keyType)
    }

    @Test
    fun `validatePrivateKey rejects key without BEGIN marker`() {
        val invalidKey = """
            -----END OPENSSH PRIVATE KEY-----
            some-content
        """.trimIndent()

        try {
            validatePrivateKey(invalidKey)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid PEM format. Key must contain BEGIN and END markers", e.message)
        }
    }

    @Test
    fun `validatePrivateKey rejects key without END marker`() {
        val invalidKey = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            some-content
        """.trimIndent()

        try {
            validatePrivateKey(invalidKey)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid PEM format. Key must contain BEGIN and END markers", e.message)
        }
    }

    @Test
    fun `validatePrivateKey rejects empty content`() {
        val invalidKey = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()

        try {
            validatePrivateKey(invalidKey)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Empty key content", e.message)
        }
    }

    @Test
    fun `validatePrivateKey rejects whitespace-only content`() {
        val invalidKey = "-----BEGIN OPENSSH PRIVATE KEY-----\n   \t  \n-----END OPENSSH PRIVATE KEY-----"

        try {
            validatePrivateKey(invalidKey)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Empty key content", e.message)
        }
    }

    @Test
    fun `validatePrivateKey rejects invalid base64 content`() {
        val invalidKey = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            not-valid-base64!!!
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()

        try {
            validatePrivateKey(invalidKey)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.startsWith("Invalid private key:"))
        }
    }

    @Test
    fun `fingerprint can be extracted from generated public key`() {
        val (publicKey, _) = generateSshKeyPair("Ed25519", "test@example.com")

        val keyPart = publicKey.substringAfter(" ").substringBefore(" ")

        assertNotEquals("", keyPart)
        assertFalse(keyPart.contains(" "))
    }

    @Test
    fun `RSA public key has correct format with comment`() {
        val comment = "developer@machine"
        val (publicKey, _) = generateSshKeyPair("RSA", comment)

        val parts = publicKey.split(" ")
        assertEquals(3, parts.size)
        assertEquals("ssh-rsa", parts[0])
        assertEquals(comment, parts[2])
    }

    @Test
    fun `RSA public key has correct format without comment`() {
        val (publicKey, _) = generateSshKeyPair("RSA", "")

        val parts = publicKey.split(" ")
        assertEquals(2, parts.size)
        assertEquals("ssh-rsa", parts[0])
    }

    @Test
    fun `Ed25519 public key has correct format with comment`() {
        val comment = "developer@machine"
        val (publicKey, _) = generateSshKeyPair("Ed25519", comment)

        val parts = publicKey.split(" ")
        assertEquals(3, parts.size)
        assertEquals("ssh-ed25519", parts[0])
        assertEquals(comment, parts[2])
    }

    @Test
    fun `Ed25519 public key has correct format without comment`() {
        val (publicKey, _) = generateSshKeyPair("Ed25519", "")

        val parts = publicKey.split(" ")
        assertEquals(2, parts.size)
        assertEquals("ssh-ed25519", parts[0])
    }

    @Test
    fun `multiple key generations produce unique keys`() {
        val keys = (1..5).map { generateSshKeyPair("Ed25519") }

        val publicKeys = keys.map { it.first }.toSet()
        val privateKeys = keys.map { it.second }.toSet()

        assertEquals(5, publicKeys.size)
        assertEquals(5, privateKeys.size)
    }

    @Test
    fun `fingerprint length is consistent for Ed25519`() {
        val (_, _) = generateSshKeyPair("Ed25519")

        val fingerprints = (1..3).map {
            val (publicKey, _) = generateSshKeyPair("Ed25519")
            calculateFingerprint(publicKey)
        }

        assertTrue(fingerprints.all { it.startsWith("SHA256:") })
        val base64Length = fingerprints.first().removePrefix("SHA256:").length
        assertTrue(fingerprints.all { it.removePrefix("SHA256:").length == base64Length })
    }

    @Test
    fun `fingerprint format matches OpenSSH format`() {
        val (publicKey, _) = generateSshKeyPair("Ed25519")

        val keyPart = publicKey.substringAfter(" ").substringBefore(" ")
        val keyBytes = java.util.Base64.getDecoder().decode(keyPart)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(keyBytes)
        val expectedFingerprint = "SHA256:${java.util.Base64.getEncoder().withoutPadding().encodeToString(digest)}"

        val actualFingerprint = calculateFingerprint(publicKey)

        assertEquals(expectedFingerprint, actualFingerprint)
    }
}