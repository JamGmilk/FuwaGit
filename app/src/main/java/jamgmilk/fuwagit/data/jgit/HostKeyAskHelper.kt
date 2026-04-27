package jamgmilk.fuwagit.data.jgit

import android.util.Base64
import android.util.Log
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import jamgmilk.fuwagit.core.util.SshFingerprintUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object HostKeyAskHelper {
    private const val TAG = "HostKeyAskHelper"
    internal const val HOST_KEY_ASK_TIMEOUT_MS = 30000L

    private val KEY_TYPE_TO_STRING = mapOf(
        0 to "ssh-rsa",
        1 to "ssh-dss",
        19 to "ecdsa-sha2-nistp256",
        20 to "ecdsa-sha2-nistp384",
        21 to "ecdsa-sha2-nistp521",
        3 to "ssh-ed25519"
    )

    private val KEY_STRING_TO_TYPE = KEY_TYPE_TO_STRING.entries.associate { it.value to it.key }

    fun keyStringToType(typeStr: String): Int = KEY_STRING_TO_TYPE[typeStr.lowercase()] ?: 0

    data class HostKeyRequest(
        val host: String,
        val keyType: String,
        val fingerprint: String,
        val future: CompletableFuture<Boolean>
    )

    private val _requests = MutableSharedFlow<HostKeyRequest>(extraBufferCapacity = 8)
    val requests: SharedFlow<HostKeyRequest> = _requests.asSharedFlow()

    fun createRepository(context: android.content.Context, skipHostKeyCheck: Boolean = false): HostKeyRepository {
        val khFile = java.io.File(context.filesDir, "ssh_known_hosts")
        return FuwaHostKeyRepositoryImpl(khFile, skipHostKeyCheck, ::emitRequest)
    }

    internal fun emitRequest(request: HostKeyRequest) {
        _requests.tryEmit(request)
    }

    data class KeyTypeInfo(val typeString: String, val typeCode: Int)

    fun inferKeyTypeInfo(key: ByteArray): KeyTypeInfo {
        if (key.size < 8) return KeyTypeInfo("ssh-rsa", 0)

        val typeStr = extractString(key) ?: return KeyTypeInfo("ssh-rsa", 0)
        val typeCode = keyStringToType(typeStr)
        val canonicalStr = KEY_TYPE_TO_STRING[typeCode] ?: "ssh-rsa"

        return KeyTypeInfo(canonicalStr, typeCode)
    }

    private fun extractString(data: ByteArray): String? {
        if (4 > data.size) return null
        val length = ((data[0].toInt() and 0xFF) shl 24) or
                ((data[1].toInt() and 0xFF) shl 16) or
                ((data[2].toInt() and 0xFF) shl 8) or
                (data[3].toInt() and 0xFF)
        if (4 + length > data.size) return null
        return String(data, 4, length, Charsets.UTF_8)
    }

    fun computeFingerprint(key: ByteArray): String {
        return try {
            SshFingerprintUtils.computeHostKeyFingerprint(key)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute fingerprint", e)
            "unknown"
        }
    }

}

class FuwaHostKeyRepositoryImpl(
    private val khFile: java.io.File?,
    private val skipHostKeyCheck: Boolean = false,
    private val requestEmitter: (HostKeyAskHelper.HostKeyRequest) -> Unit
) : HostKeyRepository {
    private val hostKeys = CopyOnWriteArrayList<HostKey>()
    private val regexCache = ConcurrentHashMap<String, Regex>()
    private val decodedKeyCache: MutableMap<HostKey, ByteArray> =
        Collections.synchronizedMap(WeakHashMap())

    init {
        loadFromFile()
    }

    private fun getRegex(pattern: String): Regex {
        return regexCache.getOrPut(pattern) {
            val escaped = Regex.fromLiteral(pattern)
                .replace("\\*", ".*")
                .replace("\\?", ".")
            Regex(escaped)
        }
    }

    private fun getDecodedKeyBytes(hostKey: HostKey): ByteArray? {
        return synchronized(decodedKeyCache) {
            decodedKeyCache[hostKey] ?: run {
                try {
                    Base64.decode(hostKey.key, Base64.NO_WRAP).also { decodedKeyCache[hostKey] = it }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun loadFromFile() {
        if (khFile == null || !khFile.exists()) return

        try {
            khFile.forEachLine { line ->
                parseKnownHostsLine(line)?.let { hostKeys.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load known_hosts: ${e.message}")
        }
    }

    private fun parseKnownHostsLine(line: String?): HostKey? {
        if (line.isNullOrBlank() || line.startsWith("#")) return null

        val parts = line.trim().split(WHITESPACE_REGEX)
        if (parts.size < 3) return null

        val hostPattern = parts[0]
        val typeStr = parts[1]
        val keyBase64 = parts[2]

        val keyType = HostKeyAskHelper.keyStringToType(typeStr)

        val keyBytes = try {
            Base64.decode(keyBase64, Base64.NO_WRAP)
        } catch (_: Exception) {
            return null
        }

        return HostKey(hostPattern, keyType, keyBytes)
    }

    override fun getKnownHostsRepositoryID(): String = "fuwa-git-known-hosts"

    override fun getHostKey(): Array<out HostKey> = hostKeys.toTypedArray()

    override fun getHostKey(host: String?, type: String?): Array<out HostKey>? {
        if (host == null) return null

        return hostKeys.filter { key ->
            val hostMatches = key.host == host ||
                    hostWildcardMatch(host, key.host)
            val typeMatches = type == null || key.type == type
            hostMatches && typeMatches
        }.toTypedArray()
    }

    private fun hostWildcardMatch(host: String, pattern: String): Boolean {
        if (!pattern.contains("*") && !pattern.contains("?")) return false
        return getRegex(pattern).matches(host)
    }

    override fun add(hostKey: HostKey?, ui: com.jcraft.jsch.UserInfo?) {
        if (hostKey == null) return
        hostKeys.removeAll { it.host == hostKey.host && it.type == hostKey.type }
        hostKeys.add(hostKey)
        saveToFile()
    }

    override fun remove(host: String?, type: String?) {
        if (host == null) return
        hostKeys.removeAll {
            it.host == host && (type == null || it.type == type)
        }
        saveToFile()
    }

    override fun remove(host: String?, type: String?, key: ByteArray?) {
        if (host == null || key == null) return
        hostKeys.removeAll {
            it.host == host &&
                    (type == null || it.type == type) &&
                    getDecodedKeyBytes(it)?.contentEquals(key) ?: false
        }
        saveToFile()
    }

    override fun check(host: String?, key: ByteArray?): Int {
        if (host == null || key == null) return HOST_KEY_NOT_FOUND

        Log.i(TAG, "check() called for host=$host, skipHostKeyCheck=$skipHostKeyCheck, keySize=${key.size}")

        if (skipHostKeyCheck) {
            Log.i(TAG, "SSH host key verification skipped for $host")
            return HOST_KEY_OK
        }

        val existingKeys = getHostKey(host, null)
        if (existingKeys.isNullOrEmpty()) {
            return handleUnknownHost(host, key)
        }

        return if (keyMatchesAny(key, existingKeys)) {
            Log.i(TAG, "Key matched for host '$host'")
            HOST_KEY_OK
        } else {
            Log.w(TAG, "SECURITY: Host '$host' has a CHANGED key — possible MITM attack! Rejecting.")
            HOST_KEY_CHANGED
        }
    }

    private fun handleUnknownHost(host: String, key: ByteArray): Int {
        val keyTypeInfo = HostKeyAskHelper.inferKeyTypeInfo(key)
        val fingerprint = HostKeyAskHelper.computeFingerprint(key)

        Log.d(TAG, "Emitting host key ask request for $host")
        val future = CompletableFuture<Boolean>()
        requestEmitter(HostKeyAskHelper.HostKeyRequest(host, keyTypeInfo.typeString, fingerprint, future))

        try {
            val accepted = future.get(HostKeyAskHelper.HOST_KEY_ASK_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            Log.d(TAG, "User responded: accepted=$accepted for $host")

            if (accepted) {
                Log.i(TAG, "User accepted new host key for $host")
                hostKeys.add(HostKey(host, keyTypeInfo.typeCode, key))
                saveToFile()
                return HOST_KEY_OK
            } else {
                Log.i(TAG, "User rejected new host key for $host")
                return HOST_KEY_NOT_FOUND
            }
        } catch (_: java.util.concurrent.TimeoutException) {
            Log.w(TAG, "Host key ask timed out for $host")
            future.complete(false)
            return HOST_KEY_NOT_FOUND
        } catch (e: Exception) {
            Log.e(TAG, "Host key ask error: ${e.message}")
            return HOST_KEY_NOT_FOUND
        }
    }

    private fun keyMatchesAny(key: ByteArray, existingKeys: Array<out HostKey>): Boolean {
        for (existing in existingKeys) {
            val existingKeyBytes = getDecodedKeyBytes(existing) ?: continue
            if (existingKeyBytes.contentEquals(key)) {
                return true
            }
        }
        return false
    }

    private fun saveToFile() {
        if (khFile == null) return

        try {
            khFile.bufferedWriter().use { writer ->
                for (key in hostKeys) {
                    writer.write("${key.host} ${key.type} ${key.key}\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save known_hosts: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FuwaHostKeyRepository"
        private const val HOST_KEY_CHANGED = -1
        private const val HOST_KEY_NOT_FOUND = 2
        private const val HOST_KEY_OK = 0
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}