package jamgmilk.obsigit.credential

import android.content.Context
import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair as JschKeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class SshKeyType(val algorithm: String, val displayName: String) {
    RSA("rsa", "RSA"),
    ED25519("ed25519", "Ed25519")
}

data class SshKeyInfo(
    val id: String,
    val name: String,
    val type: SshKeyType,
    val publicKey: String,
    val fingerprint: String,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val hasPrivateKey: Boolean = true
)

class SshKeyManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SshKeyManager"
        private const val SSH_KEYS_DIR = "ssh_keys"
        private const val PRIVATE_KEY_SUFFIX = "_private"
        private const val PUBLIC_KEY_SUFFIX = "_public.pub"
        private const val KEY_INFO_SUFFIX = "_info"
        private const val KEY_INDEX_FILE = "key_index"
        
        private const val DEFAULT_RSA_KEY_SIZE = 4096
    }
    
    private val keysDir: File by lazy {
        File(context.filesDir, SSH_KEYS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    suspend fun generateKeyPair(
        name: String,
        type: SshKeyType,
        comment: String = "",
        keySize: Int? = null
    ): Result<SshKeyInfo> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Generating ${type.displayName} key pair")
            
            when (type) {
                SshKeyType.RSA -> generateRsaKeyPair(name, comment, keySize ?: DEFAULT_RSA_KEY_SIZE)
                SshKeyType.ED25519 -> generateEd25519KeyPair(name, comment)
            }
        }
    }
    
    private fun generateRsaKeyPair(name: String, comment: String, keySize: Int): SshKeyInfo {
        val jsch = JSch()
        val keyPair = JschKeyPair.genKeyPair(jsch, JschKeyPair.RSA, keySize)
        
        val privateKeyStream = ByteArrayOutputStream()
        val publicKeyStream = ByteArrayOutputStream()
        keyPair.writePrivateKey(privateKeyStream)
        keyPair.writePublicKey(publicKeyStream, comment)
        
        val privateKeyBytes = privateKeyStream.toByteArray()
        val publicKeyString = publicKeyStream.toString("UTF-8")
        
        val fingerprint = calculateFingerprint(publicKeyString)
        val id = java.util.UUID.randomUUID().toString()
        
        saveKeyFiles(id, name, privateKeyBytes, publicKeyString, SshKeyType.RSA, fingerprint, comment)
        
        return SshKeyInfo(
            id = id,
            name = name,
            type = SshKeyType.RSA,
            publicKey = publicKeyString,
            fingerprint = fingerprint,
            comment = comment,
            createdAt = System.currentTimeMillis(),
            hasPrivateKey = true
        )
    }
    
    private fun generateEd25519KeyPair(name: String, comment: String): SshKeyInfo {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        
        val publicKeyString = formatEd25519PublicKey(publicKey, comment)
        val privateKeyBytes = formatEd25519PrivateKey(privateKey, publicKey)
        
        val fingerprint = calculateFingerprint(publicKeyString)
        val id = java.util.UUID.randomUUID().toString()
        
        saveKeyFiles(id, name, privateKeyBytes, publicKeyString, SshKeyType.ED25519, fingerprint, comment)
        
        return SshKeyInfo(
            id = id,
            name = name,
            type = SshKeyType.ED25519,
            publicKey = publicKeyString,
            fingerprint = fingerprint,
            comment = comment,
            createdAt = System.currentTimeMillis(),
            hasPrivateKey = true
        )
    }
    
    private fun formatEd25519PublicKey(publicKey: Ed25519PublicKeyParameters, comment: String): String {
        val keyBytes = publicKey.encoded
        val buffer = ByteBuffer.allocate(4 + 11 + 4 + keyBytes.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        val keyType = "ssh-ed25519"
        buffer.putInt(keyType.length)
        buffer.put(keyType.toByteArray(Charsets.US_ASCII))
        buffer.putInt(keyBytes.size)
        buffer.put(keyBytes)
        
        val base64Key = Base64.getEncoder().encodeToString(buffer.array())
        return if (comment.isNotEmpty()) "ssh-ed25519 $base64Key $comment" else "ssh-ed25519 $base64Key"
    }
    
    private fun formatEd25519PrivateKey(privateKey: Ed25519PrivateKeyParameters, publicKey: Ed25519PublicKeyParameters): ByteArray {
        val publicKeyBytes = publicKey.encoded
        val privateKeyBytes = privateKey.encoded
        
        val authMagic = byteArrayOf('o'.code.toByte(), 'p'.code.toByte(), 'e'.code.toByte(), 'n'.code.toByte(), 's'.code.toByte(), 's'.code.toByte(), 'h'.code.toByte(), '-'.code.toByte(), 'k'.code.toByte(), 'e'.code.toByte(), 'y'.code.toByte(), '-'.code.toByte(), 'v'.code.toByte(), '1'.code.toByte(), 0)
        val cipherName = "none".toByteArray(Charsets.US_ASCII)
        val kdfName = "none".toByteArray(Charsets.US_ASCII)
        val kdfOptions = ByteArray(0)
        val keyType = "ssh-ed25519".toByteArray(Charsets.US_ASCII)
        val checkInt = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(SecureRandom().nextInt()).array()
        
        val comment = "".toByteArray(Charsets.UTF_8)
        
        val privateSection = ByteBuffer.allocate(
            4 + checkInt.size +
            4 + checkInt.size +
            4 + keyType.size +
            4 + publicKeyBytes.size +
            4 + privateKeyBytes.size +
            4 + comment.size
        )
        privateSection.order(ByteOrder.LITTLE_ENDIAN)
        
        privateSection.putInt(checkInt.size)
        privateSection.put(checkInt)
        privateSection.putInt(checkInt.size)
        privateSection.put(checkInt)
        privateSection.putInt(keyType.size)
        privateSection.put(keyType)
        privateSection.putInt(publicKeyBytes.size)
        privateSection.put(publicKeyBytes)
        privateSection.putInt(privateKeyBytes.size)
        privateSection.put(privateKeyBytes)
        privateSection.putInt(comment.size)
        privateSection.put(comment)
        
        val padding = ByteArray(8 - (privateSection.position() % 8))
        for (i in padding.indices) {
            padding[i] = (i + 1).toByte()
        }
        
        val totalSize = authMagic.size +
            4 + cipherName.size +
            4 + kdfName.size +
            4 + kdfOptions.size +
            4 +
            4 + publicKeyBytes.size +
            4 + privateSection.position() + padding.size
        
        val result = ByteBuffer.allocate(totalSize)
        result.order(ByteOrder.LITTLE_ENDIAN)
        
        result.put(authMagic)
        result.putInt(cipherName.size)
        result.put(cipherName)
        result.putInt(kdfName.size)
        result.put(kdfName)
        result.putInt(kdfOptions.size)
        result.put(kdfOptions)
        result.putInt(1)
        result.putInt(publicKeyBytes.size)
        result.put(publicKeyBytes)
        result.putInt(privateSection.position() + padding.size)
        result.put(privateSection.array(), 0, privateSection.position())
        result.put(padding)
        
        return result.array()
    }
    
    suspend fun importKeyPair(
        name: String,
        privateKeyContent: String,
        publicKeyContent: String? = null,
        passphrase: String? = null
    ): Result<SshKeyInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val jsch = JSch()
            
            val privateKeyBytes = privateKeyContent.toByteArray(Charsets.UTF_8)
            val passphraseBytes = passphrase?.toByteArray(Charsets.UTF_8)
            
            val keyPair = JschKeyPair.load(jsch, privateKeyBytes, passphraseBytes)
            
            val type = when (keyPair.keyType) {
                JschKeyPair.RSA -> SshKeyType.RSA
                JschKeyPair.ED25519 -> SshKeyType.ED25519
                else -> throw IllegalArgumentException("Unsupported key type: ${keyPair.keyType}")
            }
            
            val publicKeyStream = ByteArrayOutputStream()
            keyPair.writePublicKey(publicKeyStream, "")
            val publicKeyString = publicKeyContent ?: publicKeyStream.toString("UTF-8")
            
            val fingerprint = calculateFingerprint(publicKeyString)
            val id = java.util.UUID.randomUUID().toString()
            
            val privateKeyStream = ByteArrayOutputStream()
            keyPair.writePrivateKey(privateKeyStream)
            val privateKeyBytesToSave = privateKeyStream.toByteArray()
            
            saveKeyFiles(id, name, privateKeyBytesToSave, publicKeyString, type, fingerprint, "")
            
            SshKeyInfo(
                id = id,
                name = name,
                type = type,
                publicKey = publicKeyString,
                fingerprint = fingerprint,
                comment = "",
                createdAt = System.currentTimeMillis(),
                hasPrivateKey = true
            )
        }
    }
    
    suspend fun importPublicKey(
        name: String,
        publicKeyContent: String
    ): Result<SshKeyInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val type = detectKeyType(publicKeyContent)
            val fingerprint = calculateFingerprint(publicKeyContent)
            val id = java.util.UUID.randomUUID().toString()
            
            val infoFile = File(keysDir, "$id$KEY_INFO_SUFFIX")
            val publicKeyFile = File(keysDir, "$id$PUBLIC_KEY_SUFFIX")
            publicKeyFile.writeText(publicKeyContent)
            
            saveKeyInfo(infoFile, SshKeyInfo(
                id = id,
                name = name,
                type = type,
                publicKey = publicKeyContent,
                fingerprint = fingerprint,
                comment = extractComment(publicKeyContent),
                createdAt = System.currentTimeMillis(),
                hasPrivateKey = false
            ))
            
            updateKeyIndex()
            
            SshKeyInfo(
                id = id,
                name = name,
                type = type,
                publicKey = publicKeyContent,
                fingerprint = fingerprint,
                comment = extractComment(publicKeyContent),
                createdAt = System.currentTimeMillis(),
                hasPrivateKey = false
            )
        }
    }
    
    suspend fun getAllKeys(): Result<List<SshKeyInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val indexFile = File(keysDir, KEY_INDEX_FILE)
            if (!indexFile.exists()) return@runCatching emptyList()
            
            indexFile.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { id ->
                    loadKeyInfo(id)
                }
        }
    }
    
    suspend fun getKey(id: String): Result<SshKeyInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            loadKeyInfo(id)
        }
    }
    
    suspend fun getPrivateKey(id: String): Result<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            val privateKeyFile = File(keysDir, "$id$PRIVATE_KEY_SUFFIX")
            if (privateKeyFile.exists()) {
                privateKeyFile.readBytes()
            } else {
                null
            }
        }
    }
    
    suspend fun deleteKey(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            File(keysDir, "$id$PRIVATE_KEY_SUFFIX").delete()
            File(keysDir, "$id$PUBLIC_KEY_SUFFIX").delete()
            File(keysDir, "$id$KEY_INFO_SUFFIX").delete()
            updateKeyIndex()
        }
    }
    
    suspend fun exportPublicKey(id: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val publicKeyFile = File(keysDir, "$id$PUBLIC_KEY_SUFFIX")
            if (!publicKeyFile.exists()) {
                throw IllegalArgumentException("Public key not found: $id")
            }
            publicKeyFile.readText()
        }
    }
    
    suspend fun exportPrivateKey(id: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val privateKeyFile = File(keysDir, "$id$PRIVATE_KEY_SUFFIX")
            if (!privateKeyFile.exists()) {
                throw IllegalArgumentException("Private key not found: $id")
            }
            privateKeyFile.readBytes()
        }
    }
    
    private fun saveKeyFiles(
        id: String,
        name: String,
        privateKey: ByteArray,
        publicKey: String,
        type: SshKeyType,
        fingerprint: String,
        comment: String
    ) {
        val privateKeyFile = File(keysDir, "$id$PRIVATE_KEY_SUFFIX")
        val publicKeyFile = File(keysDir, "$id$PUBLIC_KEY_SUFFIX")
        val infoFile = File(keysDir, "$id$KEY_INFO_SUFFIX")
        
        privateKeyFile.writeBytes(privateKey)
        publicKeyFile.writeText(publicKey)
        
        saveKeyInfo(infoFile, SshKeyInfo(
            id = id,
            name = name,
            type = type,
            publicKey = publicKey,
            fingerprint = fingerprint,
            comment = comment,
            createdAt = System.currentTimeMillis(),
            hasPrivateKey = true
        ))
        
        updateKeyIndex()
    }
    
    private fun saveKeyInfo(file: File, info: SshKeyInfo) {
        file.writeText(buildString {
            appendLine("id=${info.id}")
            appendLine("name=${info.name}")
            appendLine("type=${info.type.name}")
            appendLine("fingerprint=${info.fingerprint}")
            appendLine("comment=${info.comment}")
            appendLine("createdAt=${info.createdAt}")
            appendLine("hasPrivateKey=${info.hasPrivateKey}")
        })
    }
    
    private fun loadKeyInfo(id: String): SshKeyInfo? {
        val infoFile = File(keysDir, "$id$KEY_INFO_SUFFIX")
        if (!infoFile.exists()) return null
        
        val lines = infoFile.readLines()
        val map = lines.associate { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }
        
        return SshKeyInfo(
            id = map["id"] ?: id,
            name = map["name"] ?: "Unknown",
            type = try { SshKeyType.valueOf(map["type"] ?: "RSA") } catch (e: Exception) { SshKeyType.RSA },
            publicKey = File(keysDir, "$id$PUBLIC_KEY_SUFFIX").takeIf { it.exists() }?.readText() ?: "",
            fingerprint = map["fingerprint"] ?: "",
            comment = map["comment"] ?: "",
            createdAt = map["createdAt"]?.toLongOrNull() ?: 0L,
            hasPrivateKey = map["hasPrivateKey"]?.toBoolean() ?: false
        )
    }
    
    private fun updateKeyIndex() {
        val indexFile = File(keysDir, KEY_INDEX_FILE)
        val ids = keysDir.listFiles()
            ?.filter { it.name.endsWith(KEY_INFO_SUFFIX) }
            ?.map { it.name.removeSuffix(KEY_INFO_SUFFIX) }
            ?: emptyList()
        indexFile.writeText(ids.joinToString("\n"))
    }
    
    private fun calculateFingerprint(publicKey: String): String {
        val keyParts = publicKey.trim().split(" ")
        if (keyParts.size < 2) return ""
        
        return try {
            val keyBytes = Base64.getDecoder().decode(keyParts[1])
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(keyBytes)
            hash.joinToString(":") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun detectKeyType(publicKey: String): SshKeyType {
        return when {
            publicKey.startsWith("ssh-rsa") -> SshKeyType.RSA
            publicKey.startsWith("ssh-ed25519") -> SshKeyType.ED25519
            else -> throw IllegalArgumentException("Unsupported key type. Only RSA and Ed25519 are supported.")
        }
    }
    
    private fun extractComment(publicKey: String): String {
        val parts = publicKey.trim().split(" ")
        return if (parts.size >= 3) parts[2] else ""
    }
    
    fun getKeyStoragePath(): String = keysDir.absolutePath
}
