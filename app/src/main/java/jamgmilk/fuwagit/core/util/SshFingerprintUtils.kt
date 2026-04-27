package jamgmilk.fuwagit.core.util

import jamgmilk.fuwagit.BuildConfig
import java.security.MessageDigest
import java.util.Base64

object SshFingerprintUtils {
    private const val TAG = "SshFingerprintUtils"

    fun computePublicKeyFingerprint(publicKey: String): String {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, "Computing fingerprint for publicKey: ${publicKey.take(50)}...")
        }

        val keyPart = publicKey.substringAfter(" ").substringBefore(" ")
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, "Key part for fingerprint: ${keyPart.take(20)}...")
        }

        val keyBytes = Base64.getDecoder().decode(keyPart)
        val fingerprint = computeSha256Fingerprint(keyBytes)

        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, "Fingerprint calculated: $fingerprint")
        }
        return fingerprint
    }

    fun computeHostKeyFingerprint(keyBytes: ByteArray): String {
        return computeSha256Fingerprint(keyBytes)
    }

    private fun computeSha256Fingerprint(keyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(keyBytes)
        return "SHA256:${Base64.getEncoder().withoutPadding().encodeToString(digest)}"
    }
}