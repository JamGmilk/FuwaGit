package jamgmilk.fuwagit.ui.screen.credentials

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider

private const val SSH_KEY_LOG_TAG = "SSH_KEY"

fun generateSshKeyPair(type: String, comment: String = ""): Pair<String, String> {
    Log.d(SSH_KEY_LOG_TAG, "Starting SSH key generation, type: $type, comment: $comment")
    return try {
        when (type) {
            "RSA" -> {
                Log.d(SSH_KEY_LOG_TAG, "Generating RSA key pair...")
                generateRsaKeyPair(comment)
            }
            else -> {
                Log.d(SSH_KEY_LOG_TAG, "Generating Ed25519 key pair...")
                generateEd25519KeyPair(comment)
            }
        }.also { result ->
            Log.d(SSH_KEY_LOG_TAG, "Key generation successful. PublicKey length: ${result.first.length}, PrivateKey length: ${result.second.length}")
        }
    } catch (e: Exception) {
        Log.e(SSH_KEY_LOG_TAG, "Key generation failed: ${e.javaClass.simpleName}: ${e.message}", e)
        Pair("", "")
    }
}

private fun generateRsaKeyPair(comment: String = ""): Pair<String, String> {
    Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Starting RSA key generation, comment: $comment")
    return try {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Initializing RSA 4096-bit key pair generator")
        keyPairGenerator.initialize(4096)
        Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Generating RSA key pair...")
        val keyPair = keyPairGenerator.generateKeyPair()
        Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: RSA key pair generated")

        val publicKey = keyPair.public as java.security.interfaces.RSAPublicKey
        val publicKeyEncoded = encodeRsaPublicKey(publicKey, comment)
        Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Public key encoded: ${publicKeyEncoded.take(50)}...")

        val privateKey = encodeRsaPrivateKey(keyPair.private as java.security.interfaces.RSAPrivateKey, comment)
        Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Private key encoded, length: ${privateKey.length}")

        return Pair(publicKeyEncoded, privateKey)
    } catch (e: Exception) {
        Log.e(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Failed - ${e.javaClass.simpleName}: ${e.message}", e)
        throw e
    }
}

private fun encodeRsaPublicKey(publicKey: java.security.interfaces.RSAPublicKey, comment: String = ""): String {
    val byteStream = ByteArrayOutputStream()
    val dos = DataOutputStream(byteStream)

    dos.writeInt(7)
    dos.write("ssh-rsa".toByteArray())

    val exponent = publicKey.publicExponent
    val modulus = publicKey.modulus

    var exponentBytes = exponent.toByteArray()
    if (exponentBytes.isNotEmpty() && exponentBytes[0] == 0x00.toByte()) {
        exponentBytes = exponentBytes.copyOfRange(1, exponentBytes.size)
    }

    var modulusBytes = modulus.toByteArray()
    if (modulusBytes.isNotEmpty() && modulusBytes[0] == 0x00.toByte()) {
        modulusBytes = modulusBytes.copyOfRange(1, modulusBytes.size)
    }

    dos.writeInt(exponentBytes.size)
    dos.write(exponentBytes)

    dos.writeInt(modulusBytes.size)
    dos.write(modulusBytes)

    val base64Key = Base64.getEncoder().encodeToString(byteStream.toByteArray())
    return if (comment.isNotBlank()) "ssh-rsa $base64Key $comment" else "ssh-rsa $base64Key"
}

private fun encodeRsaPrivateKey(privateKey: java.security.interfaces.RSAPrivateKey, comment: String = ""): String {
    val pkcs8Encoded = privateKey.encoded
    val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
    val keyContent = base64.encodeToString(pkcs8Encoded)
    return "-----BEGIN PRIVATE KEY-----\n$keyContent\n-----END PRIVATE KEY-----"
}

private fun generateEd25519KeyPair(comment: String = ""): Pair<String, String> {
    Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Starting Ed25519 key generation with BouncyCastle, comment: $comment")
    return try {
        java.security.Security.addProvider(BouncyCastleProvider())
        Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: BC provider registered")

        val secureRandom = SecureRandom()

        val keyGen = Ed25519KeyPairGenerator()
        keyGen.init(Ed25519KeyGenerationParameters(secureRandom))
        Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: KeyPairGenerator initialized")

        val keyPair = keyGen.generateKeyPair()
        Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Key pair generated")

        val publicKeyParams = keyPair.public as Ed25519PublicKeyParameters
        val privateKeyParams = keyPair.private as Ed25519PrivateKeyParameters

        val publicKeyEncoded = encodeEd25519PublicKey(publicKeyParams, comment)
        Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Public key encoded: ${publicKeyEncoded.take(50)}...")

        val privateKeyEncoded = encodeEd25519PrivateKey(privateKeyParams, comment)
        Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Private key encoded, length: ${privateKeyEncoded.length}")

        Pair(publicKeyEncoded, privateKeyEncoded)
    } catch (e: Exception) {
        Log.e(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Failed - ${e.javaClass.simpleName}: ${e.message}", e)
        throw e
    }
}

private fun encodeEd25519PublicKey(publicKey: Ed25519PublicKeyParameters, comment: String = ""): String {
    val byteStream = ByteArrayOutputStream()
    val dos = DataOutputStream(byteStream)

    dos.writeInt(11)
    dos.write("ssh-ed25519".toByteArray())

    val keyBytes = publicKey.encoded
    dos.writeInt(keyBytes.size)
    dos.write(keyBytes)

    val base64Key = Base64.getEncoder().encodeToString(byteStream.toByteArray())
    return if (comment.isNotBlank()) "ssh-ed25519 $base64Key $comment" else "ssh-ed25519 $base64Key"
}

private fun encodeEd25519PrivateKey(privateKey: Ed25519PrivateKeyParameters, comment: String = ""): String {
    val byteStream = ByteArrayOutputStream()
    val dos = DataOutputStream(byteStream)

    val privateKeyBytes = privateKey.encoded
    dos.writeInt(32)
    dos.write(privateKeyBytes.copyOfRange(0, 32))

    val publicKey = privateKey.generatePublicKey() as Ed25519PublicKeyParameters
    val publicKeyBytes = publicKey.encoded
    dos.writeInt(32)
    dos.write(publicKeyBytes)

    val base64 = Base64.getMimeEncoder(64, "\n".toByteArray())
    val keyContent = base64.encodeToString(byteStream.toByteArray())
    return "-----BEGIN OPENSSH PRIVATE KEY-----\n$keyContent\n-----END OPENSSH PRIVATE KEY-----"
}

fun calculateFingerprint(publicKey: String): String {
    Log.d(SSH_KEY_LOG_TAG, "Calculating fingerprint for publicKey: ${publicKey.take(50)}...")
    return try {
        val keyPart = publicKey.substringAfter(" ").substringBefore(" ")
        Log.d(SSH_KEY_LOG_TAG, "Key part for fingerprint: ${keyPart.take(20)}...")
        val keyBytes = Base64.getDecoder().decode(keyPart)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(keyBytes)
        val fingerprint = "SHA256:${Base64.getEncoder().withoutPadding().encodeToString(digest)}"
        Log.d(SSH_KEY_LOG_TAG, "Fingerprint calculated: $fingerprint")
        fingerprint
    } catch (e: Exception) {
        Log.e(SSH_KEY_LOG_TAG, "Fingerprint calculation failed: ${e.javaClass.simpleName}: ${e.message}", e)
        "unknown"
    }
}

fun detectSshKeyType(privateKey: String): String {
    return when {
        privateKey.contains("BEGIN RSA PRIVATE KEY") -> "RSA"
        privateKey.contains("BEGIN OPENSSH PRIVATE KEY") -> "Ed25519"
        else -> "Ed25519"
    }
}
