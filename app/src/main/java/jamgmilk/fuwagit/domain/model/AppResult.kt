package jamgmilk.fuwagit.domain.model

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val exception: AppException) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun exceptionOrNull(): AppException? = when (this) {
        is Success -> null
        is Error -> exception
    }

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppException) -> Unit): AppResult<T> {
        if (this is Error) action(exception)
        return this
    }

    companion object {
        inline fun <T> catching(block: () -> T): AppResult<T> = try {
            Success(block())
        } catch (e: AppException) {
            Error(e)
        } catch (e: Exception) {
            Error(AppException.Unknown(e.message ?: "Unknown error"))
        }
    }
}

sealed class AppException : Exception() {
    data class CredentialNotFound(val uuid: String) : AppException() {
        override val message: String = "Credential not found: $uuid"
    }

    data class MasterKeyNotUnlocked(override val message: String = "Master key not unlocked") : AppException()

    data class InvalidPassword(override val message: String = "Invalid password") : AppException()

    data class PasswordMismatch(override val message: String = "Passwords do not match") : AppException()

    data class BiometricNotEnabled(override val message: String = "Biometric authentication not enabled") : AppException()

    data class ImportFailed(override val message: String = "Failed to import credentials") : AppException()

    data class ExportFailed(override val message: String = "Failed to export credentials") : AppException()

    data class GitOperationFailed(val operation: String, override val message: String) : AppException()

    data class RepositoryNotFound(val path: String) : AppException() {
        override val message: String = "Repository not found: $path"
    }

    data class EncryptionFailed(override val message: String = "Encryption failed") : AppException()

    data class DecryptionFailed(override val message: String = "Decryption failed") : AppException()

    data class Unknown(override val message: String) : AppException()
}

fun <T> Result<T>.toAppResult(): AppResult<T> = fold(
    onSuccess = { AppResult.Success(it) },
    onFailure = { 
        AppResult.Error(
            when (it) {
                is AppException -> it
                else -> AppException.Unknown(it.message ?: "Unknown error")
            }
        )
    }
)
