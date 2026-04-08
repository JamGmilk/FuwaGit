package jamgmilk.fuwagit.domain.usecase.git

import java.io.File
import javax.inject.Inject

/**
 * Use case to calculate the size of a repository folder.
 * Extracted from ViewModel to follow Clean Architecture principles.
 */
class CalculateRepoSizeUseCase @Inject constructor() {

    /**
     * Calculate the total size of a directory in bytes.
     * 
     * @param path The path to the repository folder
     * @return Total size in bytes, or 0L if path doesn't exist or error occurs
     */
    suspend operator fun invoke(path: String): Long {
        return try {
            val file = File(path)
            if (!file.exists()) return 0L
            if (!file.isDirectory) return file.length()

            var size = 0L
            val queue = ArrayDeque<File>()
            queue.add(file)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                current.listFiles()?.forEach { child ->
                    if (child.isDirectory) {
                        queue.add(child)
                    } else {
                        size += child.length()
                    }
                }
            }
            size
        } catch (e: Exception) {
            0L
        }
    }
}
