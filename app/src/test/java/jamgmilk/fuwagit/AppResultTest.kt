package jamgmilk.fuwagit

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import org.junit.Assert.*
import org.junit.Test

class AppResultTest {

    // ==================== AppResult.Success 基本测试 ====================

    @Test
    fun `Success wraps data correctly`() {
        val result: AppResult<Int> = AppResult.Success(42)

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals(42, result.getOrNull())
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `Success with String data`() {
        val result: AppResult<String> = AppResult.Success("test")
        assertEquals("test", result.getOrNull())
    }

    @Test
    fun `Success with List data`() {
        val result: AppResult<List<Int>> = AppResult.Success(listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), result.getOrNull())
    }

    @Test
    fun `Success with null data`() {
        val result: AppResult<String?> = AppResult.Success(null)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `Success message is null`() {
        val result = AppResult.Success(42)
        assertNull(result.message)
    }

    // ==================== AppResult.Error 基本测试 ====================

    @Test
    fun `Error wraps exception correctly`() {
        val exception = AppException.InvalidPassword()
        val result: AppResult<Int> = AppResult.Error(exception)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `Error message returns exception message`() {
        val result = AppResult.Error(AppException.CredentialNotFound("uuid-123"))
        assertEquals("Credential not found: uuid-123", result.message)
    }

    @Test
    fun `Error getOrNull returns null`() {
        val result = AppResult.Error(AppException.DecryptionFailed())
        assertNull(result.getOrNull())
    }

    // ==================== AppResult.map 测试 ====================

    @Test
    fun `map transforms success data`() {
        val result = AppResult.Success(5)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `map preserves error`() {
        val result: AppResult<Int> = AppResult.Error(AppException.InvalidPassword())
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isFailure)
        assertNull(mapped.getOrNull())
    }

    @Test
    fun `map with type change`() {
        val result = AppResult.Success(5)
        val mapped = result.map { "Number: $it" }

        assertTrue(mapped.isSuccess)
        assertEquals("Number: 5", mapped.getOrNull())
    }

    @Test
    fun `map error to success with different type`() {
        val result: AppResult<Int> = AppResult.Error(AppException.InvalidPassword())
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isFailure)
    }

    // ==================== AppResult.onSuccess 测试 ====================

    @Test
    fun `onSuccess executes action for success`() {
        val result = AppResult.Success(10)
        var captured = 0

        result.onSuccess { captured = it }

        assertEquals(10, captured)
    }

    @Test
    fun `onSuccess does not execute for error`() {
        val result: AppResult<Int> = AppResult.Error(AppException.InvalidPassword())
        var executed = false

        result.onSuccess { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onSuccess returns same result`() {
        val result = AppResult.Success(10)
        val returned = result.onSuccess { it * 2 }

        assertSame(result, returned)
    }

    // ==================== AppResult.onError 测试 ====================

    @Test
    fun `onError executes action for error`() {
        val exception = AppException.InvalidPassword()
        val result: AppResult<Int> = AppResult.Error(exception)
        var captured: AppException? = null

        result.onError { captured = it }

        assertEquals(exception, captured)
    }

    @Test
    fun `onError does not execute for success`() {
        val result = AppResult.Success(42)
        var executed = false

        result.onError { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onError returns same result`() {
        val result: AppResult<Int> = AppResult.Error(AppException.InvalidPassword())
        val returned = result.onError { }

        assertSame(result, returned)
    }

    // ==================== AppResult.catching 测试 ====================

    @Test
    fun `catching wraps successful computation`() {
        val result = AppResult.catching { 100 }

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
    }

    @Test
    fun `catching wraps successful list computation`() {
        val result = AppResult.catching {
            listOf(1, 2, 3)
        }

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2, 3), result.getOrNull())
    }

    @Test
    fun `catching wraps AppException`() {
        val result = AppResult.catching {
            throw AppException.InvalidPassword()
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException.InvalidPassword)
    }

    @Test
    fun `catching wraps generic exception as Unknown`() {
        val result = AppResult.catching {
            throw RuntimeException("Something went wrong")
        }

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppException.Unknown)
        assertEquals("Something went wrong", exception?.message)
    }

    @Test
    fun `catching with null return`() {
        val result = AppResult.catching { null }

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `catching with throwable subclass of AppException`() {
        val result = AppResult.catching {
            throw AppException.DecryptionFailed("Custom error")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException.DecryptionFailed)
    }

    // ==================== toAppResult 扩展函数测试 ====================

    @Test
    fun `toAppResult converts success Result`() {
        val result: Result<Int> = Result.success(42)
        val appResult = result.toAppResult()

        assertTrue(appResult.isSuccess)
        assertEquals(42, appResult.getOrNull())
    }

    @Test
    fun `toAppResult converts failure Result with AppException`() {
        val cause = AppException.InvalidPassword()
        val result: Result<Int> = Result.failure(cause)
        val appResult = result.toAppResult()

        assertTrue(appResult.isFailure)
        assertEquals(cause, appResult.exceptionOrNull())
    }

    @Test
    fun `toAppResult converts failure Result with generic exception`() {
        val cause = RuntimeException("Network error")
        val result: Result<Int> = Result.failure(cause)
        val appResult = result.toAppResult()

        assertTrue(appResult.isFailure)
        assertTrue(appResult.exceptionOrNull() is AppException.Unknown)
    }

    @Test
    fun `toAppResult converts failure Result with unknown exception`() {
        val cause = IllegalStateException("Unknown state")
        val result: Result<Int> = Result.failure(cause)
        val appResult = result.toAppResult()

        assertTrue(appResult.isFailure)
        assertTrue(appResult.exceptionOrNull() is AppException.Unknown)
        assertEquals("Unknown state", appResult.exceptionOrNull()?.message)
    }
}

class AppExceptionTest {

    // ==================== Credential Exceptions ====================

    @Test
    fun `CredentialNotFound has correct message`() {
        val exception = AppException.CredentialNotFound("uuid-456")
        assertEquals("Credential not found: uuid-456", exception.message)
    }

    @Test
    fun `MasterKeyNotUnlocked has default message`() {
        val exception = AppException.MasterKeyNotUnlocked()
        assertEquals("Master key not unlocked", exception.message)
    }

    @Test
    fun `MasterKeyNotUnlocked can override message`() {
        val exception = AppException.MasterKeyNotUnlocked("Custom message")
        assertEquals("Custom message", exception.message)
    }

    @Test
    fun `InvalidPassword has default message`() {
        val exception = AppException.InvalidPassword()
        assertEquals("Invalid password", exception.message)
    }

    @Test
    fun `PasswordMismatch has default message`() {
        val exception = AppException.PasswordMismatch()
        assertEquals("Passwords do not match", exception.message)
    }

    @Test
    fun `BiometricError has custom message`() {
        val exception = AppException.BiometricError("Hardware unavailable")
        assertEquals("Hardware unavailable", exception.message)
    }

    @Test
    fun `DecryptionFailed has default message`() {
        val exception = AppException.DecryptionFailed()
        assertEquals("Decryption failed", exception.message)
    }

    @Test
    fun `DecryptionFailed can have custom message`() {
        val exception = AppException.DecryptionFailed("Custom decryption error")
        assertEquals("Custom decryption error", exception.message)
    }

    @Test
    fun `GitOperationFailed has operation and message`() {
        val exception = AppException.GitOperationFailed("clone", "Connection refused")
        assertEquals("Connection refused", exception.message)
    }

    @Test
    fun `RepositoryNotFound has correct message`() {
        val exception = AppException.RepositoryNotFound("/path/to/repo")
        assertEquals("Repository not found: /path/to/repo", exception.message)
    }

    @Test
    fun `RepositoryAlreadyExists has correct message`() {
        val exception = AppException.RepositoryAlreadyExists("/path/to/repo")
        assertEquals("Repository already exists: /path/to/repo", exception.message)
    }

    @Test
    fun `BranchNotFound has correct message`() {
        val exception = AppException.BranchNotFound("feature-x")
        assertEquals("Branch not found: feature-x", exception.message)
    }

    @Test
    fun `BranchAlreadyExists has correct message`() {
        val exception = AppException.BranchAlreadyExists("feature-x")
        assertEquals("Branch already exists: feature-x", exception.message)
    }

    @Test
    fun `MergeConflict has custom message`() {
        val exception = AppException.MergeConflict("Auto-merge failed")
        assertEquals("Auto-merge failed", exception.message)
    }

    @Test
    fun `RebaseConflict has custom message`() {
        val exception = AppException.RebaseConflict("Rebase failed")
        assertEquals("Rebase failed", exception.message)
    }

    @Test
    fun `CheckoutConflict has custom message`() {
        val exception = AppException.CheckoutConflict("Cannot checkout")
        assertEquals("Cannot checkout", exception.message)
    }

    @Test
    fun `RemoteNotFound has correct message`() {
        val exception = AppException.RemoteNotFound("origin")
        assertEquals("Remote not found: origin", exception.message)
    }

    @Test
    fun `PushRejected has custom message`() {
        val exception = AppException.PushRejected("Rejected by remote")
        assertEquals("Rejected by remote", exception.message)
    }

    @Test
    fun `PullRejected has custom message`() {
        val exception = AppException.PullRejected("Rejected locally")
        assertEquals("Rejected locally", exception.message)
    }

    @Test
    fun `InvalidRepository has path and message`() {
        val exception = AppException.InvalidRepository("/path", "Not a git repo")
        assertEquals("Not a git repo", exception.message)
    }

    @Test
    fun `NoRemoteConfigured has default message`() {
        val exception = AppException.NoRemoteConfigured()
        assertEquals("No remote repository configured", exception.message)
    }

    @Test
    fun `CommitFailed has custom message`() {
        val exception = AppException.CommitFailed("Nothing to commit")
        assertEquals("Nothing to commit", exception.message)
    }

    @Test
    fun `ResetFailed has custom message`() {
        val exception = AppException.ResetFailed("Cannot reset")
        assertEquals("Cannot reset", exception.message)
    }

    @Test
    fun `CloneFailed has custom message`() {
        val exception = AppException.CloneFailed("Network error")
        assertEquals("Network error", exception.message)
    }

    @Test
    fun `AuthenticationFailed has default message`() {
        val exception = AppException.AuthenticationFailed()
        assertEquals("Authentication failed", exception.message)
    }

    @Test
    fun `AuthenticationFailed with custom message`() {
        val exception = AppException.AuthenticationFailed("Token expired")
        assertEquals("Token expired", exception.message)
    }

    @Test
    fun `NetworkError has custom message`() {
        val exception = AppException.NetworkError("Connection timeout")
        assertEquals("Connection timeout", exception.message)
    }

    @Test
    fun `Validation has custom message`() {
        val exception = AppException.Validation("Invalid input")
        assertEquals("Invalid input", exception.message)
    }

    @Test
    fun `Unknown has custom message`() {
        val exception = AppException.Unknown("Unexpected error")
        assertEquals("Unexpected error", exception.message)
    }
}
