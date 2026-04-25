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

    private val timestampFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    }
    private val displayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    @Volatile
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
            val versionCode = packageInfo.versionCode
            val versionName = packageInfo.versionName ?: "Unknown"
            "$versionName ($versionCode)"
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

        val timestampDateFormat = timestampFormat.get()!!
        val timestamp = timestampDateFormat.format(Date())
        val logFile = File(logDir, "${LOG_PREFIX}${timestamp}.txt")

        try {
            writeCrashLog(logFile, thread, throwable)
            Log.i(TAG, "Crash log written to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }

        defaultHandler?.uncaughtException(thread ?: Thread.currentThread(), throwable)
    }

    private fun PrintWriter.writeLogHeader(title: String) {
        println("=== $title ===")
        println("Time: ${displayFormat.get()!!.format(Date())}")
        println("App Version: $appVersion")
        println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        println("Device: ${Build.MANUFACTURER}/${Build.MODEL}")
        println()
    }

    private fun PrintWriter.writeCauseChain(throwable: Throwable) {
        var cause = throwable.cause
        var causeCount = 1
        while (cause != null && causeCount <= 5) {
            println("Caused by [$causeCount]: ${cause.javaClass.name}")
            println("Message: ${cause.message}")
            cause.printStackTrace(this)
            println()
            cause = cause.cause
            causeCount++
        }
    }

    private fun writeCrashLog(logFile: File, thread: Thread?, throwable: Throwable) {
        PrintWriter(FileWriter(logFile)).use { writer ->
            writer.writeLogHeader("FuwaGit Crash Log")
            writer.println("Thread: ${thread?.name ?: "Unknown"}")
            writer.println()
            writer.println("Exception: ${throwable.javaClass.name}")
            writer.println("Message: ${throwable.message}")
            writer.println()
            writer.println("Stack Trace:")
            throwable.printStackTrace(writer)
            writer.println()
            writer.writeCauseChain(throwable)
            writer.println("=== End of Crash Log ===")
        }
    }

    private fun cleanupOldLogsSync() {
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
        val logFiles = getLogFiles()
        val logs = logFiles.mapIndexed { index, file ->
            buildString {
                if (index > 0) append("\n\n========================================\n========================================\n\n")
                append(file.readText())
            }
        }
        return buildString {
            append("=== FuwaGit Crash Logs Export ===\n")
            append("Exported at: ${displayFormat.get()!!.format(Date())}\n")
            append("Total crash logs: ${logFiles.size}\n")
            append("================================\n\n")
            append(logs.joinToString(""))
        }
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
        val timestampDateFormat = timestampFormat.get()!!
        val timestamp = timestampDateFormat.format(Date())
        val logFile = File(logDir, "${LOG_PREFIX}manual_${timestamp}.txt")

        try {
            PrintWriter(FileWriter(logFile)).use { writer ->
                writer.writeLogHeader("FuwaGit Manual Error Log")
                writer.println("Tag: $tag")
                writer.println()
                writer.println("Message: $message")
                writer.println()
                throwable?.let {
                    writer.println("Stack Trace:")
                    it.printStackTrace(writer)
                    writer.println()
                    writer.writeCauseChain(it)
                }
                writer.println("=== End of Manual Error Log ===")
            }
            Log.d(TAG, "Manual error log written to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write manual error log", e)
        }
    }
}