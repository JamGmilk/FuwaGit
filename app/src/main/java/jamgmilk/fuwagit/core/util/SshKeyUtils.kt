package jamgmilk.fuwagit.core.util

import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.util.Base64
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter

private const val SSH_KEY_LOG_TAG = "SSH_KEY"

fun generateSshKeyPair(type: String, comment: String = ""): Pair<String, String> {
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "Starting SSH key generation, type: $type, comment: $comment")

    val result = when (type) {
        "RSA" -> {
            if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "Generating RSA key pair...")
            generateRsaKeyPair(comment)
        }
        else -> {
            if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "Generating Ed25519 key pair...")
            generateEd25519KeyPair(comment)
        }
    }

    if (BuildConfig.DEBUG) {
        Log.d(SSH_KEY_LOG_TAG, "Key generation successful. PublicKey length: ${result.first.length}, PrivateKey length: ${result.second.length}")
    }
    return result
}

private fun generateRsaKeyPair(comment: String = ""): Pair<String, String> {
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Starting RSA key generation, comment: $comment")

    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Initializing RSA 4096-bit key pair generator")
    keyPairGenerator.initialize(4096)

    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Generating RSA key pair...")
    val keyPair = keyPairGenerator.generateKeyPair()
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: RSA key pair generated")

    val publicKey = keyPair.public as java.security.interfaces.RSAPublicKey
    val publicKeyEncoded = encodeRsaPublicKey(publicKey, comment)
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Public key encoded: ${publicKeyEncoded.take(50)}...")

    val privateKey = encodeRsaPrivateKey(keyPair.private as RSAPrivateCrtKey)
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateRsaKeyPair: Private key encoded (PKCS#1), length: ${privateKey.length}")

    return Pair(publicKeyEncoded, privateKey)
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

private fun encodeRsaPrivateKey(privateKey: RSAPrivateCrtKey): String {
    val vector = ASN1EncodableVector()
    vector.add(ASN1Integer(BigInteger.ZERO))
    vector.add(ASN1Integer(privateKey.modulus))
    vector.add(ASN1Integer(privateKey.publicExponent))
    vector.add(ASN1Integer(privateKey.privateExponent))
    vector.add(ASN1Integer(privateKey.primeP))
    vector.add(ASN1Integer(privateKey.primeQ))
    vector.add(ASN1Integer(privateKey.primeExponentP))
    vector.add(ASN1Integer(privateKey.primeExponentQ))
    vector.add(ASN1Integer(privateKey.crtCoefficient))

    val sequence = DERSequence(vector)
    val derBytes = sequence.encoded

    val writer = StringWriter()
    PemWriter(writer).use { pw ->
        pw.writeObject(PemObject("RSA PRIVATE KEY", derBytes))
    }
    return writer.toString()
}

private fun generateEd25519KeyPair(comment: String = ""): Pair<String, String> {
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Starting Ed25519 key generation with OpenSSH format, comment: $comment")

    val keyPairGenerator = Ed25519KeyPairGenerator()
    keyPairGenerator.init(Ed25519KeyGenerationParameters(SecureRandom()))
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Ed25519KeyPairGenerator initialized")

    val keyPair = keyPairGenerator.generateKeyPair()
    val publicKey = keyPair.public as Ed25519PublicKeyParameters
    val privateKey = keyPair.private as Ed25519PrivateKeyParameters
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Key pair generated (lightweight API)")

    val publicKeyEncoded = encodeEd25519PublicKey(publicKey, comment)
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Public key encoded: ${publicKeyEncoded.take(50)}...")

    val privateKeyEncoded = encodeEd25519PrivateKey(privateKey, publicKey, comment)
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "generateEd25519KeyPair: Private key encoded (OpenSSH format), length: ${privateKeyEncoded.length}")

    return Pair(publicKeyEncoded, privateKeyEncoded)
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

private fun encodeEd25519PrivateKey(
    privateKey: Ed25519PrivateKeyParameters,
    publicKey: Ed25519PublicKeyParameters,
    comment: String = ""
): String {
    val byteStream = ByteArrayOutputStream()
    val dos = DataOutputStream(byteStream)

    dos.write("openssh-key-v1".toByteArray())
    dos.writeByte(0)

    writeOpenSshString(dos, "none")
    writeOpenSshString(dos, "none")
    writeOpenSshString(dos, ByteArray(0))

    dos.writeInt(1)

    val pubKeyBlob = ByteArrayOutputStream()
    val pubDos = DataOutputStream(pubKeyBlob)
    pubDos.writeInt(11)
    pubDos.write("ssh-ed25519".toByteArray())
    val pubKeyBytes = publicKey.encoded
    pubDos.writeInt(pubKeyBytes.size)
    pubDos.write(pubKeyBytes)
    writeOpenSshString(dos, pubKeyBlob.toByteArray())

    val privKeyBlob = ByteArrayOutputStream()
    val privDos = DataOutputStream(privKeyBlob)

    val checkInt = SecureRandom().nextInt()
    privDos.writeInt(checkInt)
    privDos.writeInt(checkInt)

    writeOpenSshString(privDos, "ssh-ed25519")
    writeOpenSshString(privDos, pubKeyBytes)

    val seed = privateKey.encoded
    val combined = seed + pubKeyBytes
    writeOpenSshString(privDos, combined)

    writeOpenSshString(privDos, comment.toByteArray())

    val paddingLength = (8 - (privKeyBlob.size() % 8)) % 8
    for (i in 1..paddingLength) {
        privDos.writeByte(i.toByte().toInt())
    }

    writeOpenSshString(dos, privKeyBlob.toByteArray())

    val base64 = Base64.getMimeEncoder(70, "\n".toByteArray())
    val keyContent = base64.encodeToString(byteStream.toByteArray())
    return "-----BEGIN OPENSSH PRIVATE KEY-----\n$keyContent\n-----END OPENSSH PRIVATE KEY-----\n"
}

private fun writeOpenSshString(dos: DataOutputStream, str: String) {
    val bytes = str.toByteArray()
    dos.writeInt(bytes.size)
    dos.write(bytes)
}

private fun writeOpenSshString(dos: DataOutputStream, bytes: ByteArray) {
    dos.writeInt(bytes.size)
    dos.write(bytes)
}

fun calculateFingerprint(publicKey: String): String {
    return SshFingerprintUtils.computePublicKeyFingerprint(publicKey)
}

fun detectSshKeyType(privateKey: String): String {
    return try {
        StringReader(privateKey).use { reader ->
            PemReader(reader).use { pemReader ->
                val pemObject = pemReader.readPemObject()
                    ?: return "Unknown"
                when {
                    pemObject.type.contains("RSA PRIVATE KEY") -> "RSA"
                    pemObject.type.contains("OPENSSH PRIVATE KEY") -> detectOpenSshKeyType(pemObject.content)
                    pemObject.type == "PRIVATE KEY" -> {
                        val info = PrivateKeyInfo.getInstance(pemObject.content)
                        val algOid = info.privateKeyAlgorithm.algorithm
                        when {
                            algOid.equals(PKCSObjectIdentifiers.rsaEncryption) -> "RSA"
                            algOid.equals(EdECObjectIdentifiers.id_Ed25519) -> "Ed25519"
                            else -> "Unknown"
                        }
                    }
                    else -> "Unknown"
                }
            }
        }
    } catch (_: Exception) {
        "Unknown"
    }
}

private fun detectOpenSshKeyType(keyContent: ByteArray): String {
    return try {
        val inputStream = java.io.ByteArrayInputStream(keyContent)
        val dis = java.io.DataInputStream(inputStream)

        val authMagic = ByteArray(15)
        dis.readFully(authMagic)
        if (String(authMagic) != "openssh-key-v1") {
            return "Unknown"
        }

        dis.readByte()

        val cipherName = readString(dis)
        val cipherNameLower = cipherName.lowercase()
        if (cipherNameLower != "none" && cipherNameLower.isNotEmpty()) {
            return "Unknown"
        }

        readString(dis) // kdfName
        readString(dis) // kdfOptions

        dis.readInt()

        val publicKeyBlob = readString(dis)
        if (publicKeyBlob.isEmpty()) {
            return "Unknown"
        }

        val pubKeyInput = java.io.ByteArrayInputStream(publicKeyBlob.toByteArray())
        val pubKeyDis = java.io.DataInputStream(pubKeyInput)

        val keyTypeLength = pubKeyDis.readInt()
        val keyTypeBytes = ByteArray(keyTypeLength)
        pubKeyDis.readFully(keyTypeBytes)
        val keyType = String(keyTypeBytes)

        when {
            keyType.equals("ssh-rsa", ignoreCase = true) -> "RSA"
            keyType.equals("ssh-ed25519", ignoreCase = true) -> "Ed25519"
            keyType.equals("ecdsa-sha2-nistp256", ignoreCase = true) -> "ECDSA256"
            keyType.equals("ecdsa-sha2-nistp384", ignoreCase = true) -> "ECDSA384"
            keyType.equals("ecdsa-sha2-nistp521", ignoreCase = true) -> "ECDSA521"
            else -> "Unknown"
        }
    } catch (_: Exception) {
        "Unknown"
    }
}

private fun readString(dis: java.io.DataInputStream): String {
    val length = dis.readInt()
    if (length !in 1..262144) {
        return ""
    }
    val bytes = ByteArray(length)
    dis.readFully(bytes)
    return String(bytes)
}

fun validatePrivateKey(privateKey: String): Pair<Boolean, String> {
    if (BuildConfig.DEBUG) Log.d(SSH_KEY_LOG_TAG, "Validating private key...")
    try {
        if (!isValidPemFormat(privateKey)) {
            throw IllegalArgumentException("Invalid PEM format. Key must contain BEGIN and END markers")
        }
        val keyContent = privateKey
            .replace("-----BEGIN[\\s\\S]*?-----".toRegex(), "")
            .replace("-----END[\\s\\S]*?-----".toRegex(), "")
            .replace("\\s".toRegex(), "")
        if (keyContent.isBlank()) {
            throw IllegalArgumentException("Empty key content")
        }
        try {
            Base64.getDecoder().decode(keyContent)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid private key: ${e.message}")
        }
        val keyType = detectSshKeyType(privateKey)
        if (keyType == "Unknown") {
            throw IllegalArgumentException("Unsupported key algorithm. Only RSA and Ed25519 are supported")
        }
        return Pair(true, keyType)
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid private key: ${e.message}")
    }
}

fun isValidPemFormat(key: String): Boolean {
    val trimmedKey = key.trim()
    return trimmedKey.startsWith("-----BEGIN") && trimmedKey.contains("PRIVATE KEY-----")
}
