package jamgmilk.fuwagit.data.jgit

import android.util.Base64
import android.util.Log
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture

object HostKeyAskHelper {
    private const val TAG = "HostKeyAskHelper"
    internal const val HOST_KEY_ASK_TIMEOUT_MS = 30000L

    data class HostKeyRequest(
        val host: String,
        val keyType: String,
        val fingerprint: String,
        val future: CompletableFuture<Boolean>
    )

    private val _requests = MutableSharedFlow<HostKeyRequest>(extraBufferCapacity = 1)
    val requests: SharedFlow<HostKeyRequest> = _requests.asSharedFlow()

    fun createRepository(context: android.content.Context, skipHostKeyCheck: Boolean = false): HostKeyRepository {
        val khFile = java.io.File(context.filesDir, "ssh_known_hosts")
        return FuwaHostKeyRepositoryImpl(khFile, skipHostKeyCheck)
    }

    data class KeyTypeInfo(val typeString: String, val typeCode: Int)

    fun inferKeyTypeInfo(key: ByteArray): KeyTypeInfo {
        return when {
            key.size == 32 -> KeyTypeInfo("ssh-ed25519", 3)
            key.size > 256 -> KeyTypeInfo("ssh-rsa", 0)
            else -> KeyTypeInfo("ssh-rsa", 0)
        }
    }

    fun computeFingerprint(key: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(key)
            hash.joinToString(":") { "%02x".format(it) }
        } catch (e: Exception) {
            "unknown"
        }
    }

    class FuwaHostKeyRepositoryImpl(
        private val khFile: java.io.File?,
        private val skipHostKeyCheck: Boolean = false
    ) : HostKeyRepository {
        private val hostKeys = mutableListOf<HostKey>()

        init {
            loadFromFile()
        }

        private fun loadFromFile() {
            if (khFile == null || !khFile.exists()) return

            try {
                khFile.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        parseKnownHostsLine(line)?.let { hostKeys.add(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load known_hosts: ${e.message}")
            }
        }

        private fun parseKnownHostsLine(line: String?): HostKey? {
            if (line.isNullOrBlank() || line.startsWith("#")) return null

            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 3) return null

            val hostPattern = parts[0]
            val typeStr = parts[1]
            val keyBase64 = parts[2]

            val keyType = when (typeStr.lowercase()) {
                "ssh-rsa" -> 0
                "ssh-dss" -> 1
                "ecdsa-sha2-nistp256" -> 19
                "ecdsa-sha2-nistp384" -> 20
                "ecdsa-sha2-nistp521" -> 21
                "ssh-ed25519" -> 3
                else -> 0
            }

            val keyBytes = try {
                Base64.decode(keyBase64, Base64.NO_WRAP)
            } catch (e: Exception) {
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
                val typeMatches = type == null || key.type.toString() == type
                hostMatches && typeMatches
            }.toTypedArray()
        }

        private fun hostWildcardMatch(host: String, pattern: String): Boolean {
            if (!pattern.contains("*") && !pattern.contains("?")) return false

            val regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            return host.matches(Regex(regex))
        }

        override fun add(hostKey: HostKey?, ui: com.jcraft.jsch.UserInfo?) {
            if (hostKey == null) return
            val existingIndex = hostKeys.indexOfFirst {
                it.host == hostKey.host && it.type == hostKey.type
            }
            if (existingIndex >= 0) {
                hostKeys[existingIndex] = hostKey
            } else {
                hostKeys.add(hostKey)
            }
        }

        override fun remove(host: String?, type: String?) {
            if (host == null) return
            hostKeys.removeAll {
                it.host == host && (type == null || it.type.toString() == type)
            }
        }

        override fun remove(host: String?, type: String?, key: ByteArray?) {
            if (host == null || key == null) return
            hostKeys.removeAll {
                it.host == host &&
                        (type == null || it.type.toString() == type) &&
                        try {
                            Base64.decode(it.key, Base64.NO_WRAP).contentEquals(key)
                        } catch (e: Exception) {
                            false
                        }
            }
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
            val keyTypeInfo = inferKeyTypeInfo(key)
            val fingerprint = computeFingerprint(key)

            Log.d(TAG, "Emitting host key ask request for $host")
            val future = CompletableFuture<Boolean>()
            _requests.tryEmit(HostKeyRequest(host, keyTypeInfo.typeString, fingerprint, future))

            try {
                val accepted = future.get(HOST_KEY_ASK_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
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
            } catch (e: java.util.concurrent.TimeoutException) {
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
                val existingKeyBytes = try {
                    Base64.decode(existing.key, Base64.NO_WRAP)
                } catch (e: Exception) {
                    continue
                }
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

        fun addHostKeyDirectly(host: String, keyType: Int, key: ByteArray) {
            hostKeys.add(HostKey(host, keyType, key))
            saveToFile()
        }

        companion object {
            private const val HOST_KEY_CHANGED = -1
            private const val HOST_KEY_NOT_FOUND = 2
            private const val HOST_KEY_OK = 0
        }
    }
}