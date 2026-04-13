package jamgmilk.fuwagit.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogManager {
    private const val TAG = "CrashLogManager"
    private const val LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10
    private const val LOG_PREFIX = "crash_"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var isInitialized = false
    private lateinit var logDir: File
    private var appVersion: String = "Unknown"

    fun init(context: Context) {
        if (isInitialized) return

        logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleException(thread, throwable)
        }
        isInitialized = true
        Log.i(TAG, "CrashLogManager initialized. Log directory: ${logDir.absolutePath}")
    }

    private fun handleException(thread: Thread?, throwable: Throwable?) {
        if (throwable == null) {
            Log.e(TAG, "handleException called with null throwable")
            defaultHandler?.uncaughtException(thread ?: Thread.currentThread(), NullPointerException("null throwable"))
            return
        }

        val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())
        val logFile = File(logDir, "${LOG_PREFIX}${timestamp}.txt")

        try {
            writeLog(logFile, thread ?: Thread.currentThread(), throwable)
            Log.i(TAG, "Crash log written to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }

        defaultHandler?.uncaughtException(thread ?: Thread.currentThread(), throwable)
    }

    private fun writeLog(logFile: File, thread: Thread?, throwable: Throwable) {
        PrintWriter(FileWriter(logFile)).use { writer ->
            writer.println("=== FuwaGit Crash Log ===")
            writer.println("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            writer.println("App Version: ${getAppVersion()}")
            writer.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            writer.println("Device: ${Build.MANUFACTURER}/${Build.MODEL}")
            writer.println()

            writer.println("Thread: ${thread?.name ?: "Unknown"}")
            writer.println()

            writer.println("Exception: ${throwable.javaClass.name}")
            writer.println("Message: ${throwable.message}")
            writer.println()

            writer.println("Stack Trace:")
            throwable.printStackTrace(writer)
            writer.println()

            var cause = throwable.cause
            var causeCount = 1
            while (cause != null && causeCount <= 5) {
                writer.println("Caused by [$causeCount]: ${cause.javaClass.name}")
                writer.println("Message: ${cause.message}")
                cause.printStackTrace(writer)
                writer.println()
                cause = cause.cause
                causeCount++
            }

            writer.println("=== End of Crash Log ===")
        }

        cleanupOldLogs()
    }

    private fun getAppVersion(): String = appVersion

    private fun cleanupOldLogs() {
        val logFiles = logDir.listFiles { file ->
            file.isFile && file.name.startsWith(LOG_PREFIX) && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: return

        if (logFiles.size > MAX_LOG_FILES) {
            logFiles.drop(MAX_LOG_FILES).forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted old crash log: ${file.name}")
            }
        }
    }

    fun getLogFiles(): List<File> {
        return logDir.listFiles { file ->
            file.isFile && file.name.startsWith(LOG_PREFIX) && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getAllLogsContent(): String {
        val logs = StringBuilder()
        logs.append("=== FuwaGit Crash Logs Export ===\n")
        logs.append("Exported at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
        logs.append("Total crash logs: ${getLogFiles().size}\n")
        logs.append("================================\n\n")

        getLogFiles().forEachIndexed { index, file ->
            if (index > 0) {
                logs.append("\n\n")
                logs.append("========================================\n")
                logs.append("========================================\n\n")
            }
            logs.append(file.readText())
        }

        return logs.toString()
    }

    fun createShareIntent(context: Context): Intent {
        val logsContent = getAllLogsContent()
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FuwaGit Crash Logs")
            putExtra(Intent.EXTRA_TEXT, logsContent)
        }
    }

    fun logManualError(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())
        val logFile = File(logDir, "${LOG_PREFIX}manual_${timestamp}.txt")

        try {
            PrintWriter(FileWriter(logFile)).use { writer ->
                writer.println("=== FuwaGit Manual Error Log ===")
                writer.println("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                writer.println("Tag: $tag")
                writer.println("App Version: ${getAppVersion()}")
                writer.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                writer.println("Device: ${Build.MANUFACTURER}/${Build.MODEL}")
                writer.println()
                writer.println("Message: $message")
                writer.println()
                throwable?.let {
                    writer.println("Stack Trace:")
                    it.printStackTrace(writer)
                }
                writer.println("=== End of Manual Error Log ===")
            }
            Log.d(TAG, "Manual error log written to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write manual error log", e)
        }
    }
}
