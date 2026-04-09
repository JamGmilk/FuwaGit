package jamgmilk.fuwagit

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import org.junit.Assert.*
import org.junit.Test

class AppResultTest {

    @Test
    fun `Success wraps data correctly`() {
        val result: AppResult<Int> = AppResult.Success(42)

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals(42, result.getOrNull())
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `Error wraps exception correctly`() {
        val exception = AppException.InvalidPassword()
        val result: AppResult<Int> = AppResult.Error(exception)

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `Success getOrNull returns data`() {
        val result = AppResult.Success("test")
        assertEquals("test", result.getOrNull())
    }

    @Test
    fun `Error getOrNull returns null`() {
        val result = AppResult.Error(AppException.DecryptionFailed())
        assertNull(result.getOrNull())
    }

    @Test
    fun `Success message is null`() {
        val result = AppResult.Success(42)
        assertNull(result.message)
    }

    @Test
    fun `Error message returns exception message`() {
        val result = AppResult.Error(AppException.CredentialNotFound("uuid-123"))
        assertEquals("Credential not found: uuid-123", result.message)
    }

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

        assertTrue(mapped.isError)
        assertNull(mapped.getOrNull())
    }

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
    fun `catching wraps successful computation`() {
        val result = AppResult.catching { 100 }

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
    }

    @Test
    fun `catching wraps AppException`() {
        val result = AppResult.catching {
            throw AppException.InvalidPassword()
        }

        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is AppException.InvalidPassword)
    }

    @Test
    fun `catching wraps generic exception as Unknown`() {
        val result = AppResult.catching {
            throw RuntimeException("Something went wrong")
        }

        assertTrue(result.isError)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppException.Unknown)
        assertEquals("Something went wrong", exception?.message)
    }

    @Test
    fun `catching with result transformation`() {
        val result = AppResult.catching {
            listOf(1, 2, 3)
        }

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2, 3), result.getOrNull())
    }
}

class AppExceptionTest {

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
    fun `BiometricNotEnabled has default message`() {
        val exception = AppException.BiometricNotEnabled()
        assertEquals("Biometric authentication not enabled", exception.message)
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
    fun `ImportFailed has default message`() {
        val exception = AppException.ImportFailed()
        assertEquals("Failed to import credentials", exception.message)
    }

    @Test
    fun `ExportFailed has default message`() {
        val exception = AppException.ExportFailed()
        assertEquals("Failed to export credentials", exception.message)
    }

    @Test
    fun `EncryptionFailed has default message`() {
        val exception = AppException.EncryptionFailed()
        assertEquals("Encryption failed", exception.message)
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
    fun `BranchNotFound has correct message`() {
        val exception = AppException.BranchNotFound("feature-x")
        assertEquals("Branch not found: feature-x", exception.message)
    }

    @Test
    fun `MergeConflict has custom message`() {
        val exception = AppException.MergeConflict("Auto-merge failed")
        assertEquals("Auto-merge failed", exception.message)
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
    fun `Unknown has custom message`() {
        val exception = AppException.Unknown("Unexpected error")
        assertEquals("Unexpected error", exception.message)
    }

    @Test
    fun `Validation has custom message`() {
        val exception = AppException.Validation("Invalid input")
        assertEquals("Invalid input", exception.message)
    }
}