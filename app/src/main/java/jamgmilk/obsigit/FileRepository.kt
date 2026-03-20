package jamgmilk.obsigit

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileRepository {
    suspend fun getFileOwner(path: String): Result<String> = withContext(Dispatchers.IO) {
        val shellResult = Shell.cmd("stat -c '%U' $path").exec()
        val owner = shellResult.out.firstOrNull()
        if (shellResult.isSuccess && owner != null) {
            Result.success(owner)
        } else {
            Result.failure(Exception("Failed to get owner for $path"))
        }
    }
}