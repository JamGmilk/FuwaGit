package jamgmilk.fuwagit.core.util

import java.util.Arrays

object SecurityUtils {
    fun zeroBytes(bytes: ByteArray?) {
        bytes?.let { Arrays.fill(it, 0.toByte()) }
    }

    fun zeroBytesIfNotNull(bytes: ByteArray?) {
        bytes?.let { Arrays.fill(it, 0.toByte()) }
    }
}