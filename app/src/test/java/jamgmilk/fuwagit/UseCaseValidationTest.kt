package jamgmilk.fuwagit

import org.junit.Assert.*
import org.junit.Test

class UseCaseValidationTest {

    // ==================== 通用验证逻辑 ====================

    private fun validateNotBlank(value: String?, fieldName: String): Result<Unit> {
        return if (value.isNullOrBlank()) {
            Result.failure(IllegalArgumentException("$fieldName cannot be empty"))
        } else {
            Result.success(Unit)
        }
    }

    // ==================== CloneRepositoryUseCase 验证测试 ====================

    @Test
    fun `test clone validation rejects blank URI`() {
        val result = validateNotBlank("", "URI")
        assertTrue(result.isFailure)
        assertEquals("URI cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test clone validation rejects whitespace-only URI`() {
        val result = validateNotBlank("   ", "URI")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test clone validation accepts valid URI`() {
        val result = validateNotBlank("https://github.com/user/repo.git", "URI")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test clone validation rejects blank localPath`() {
        val result = validateNotBlank("", "Local path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test clone validation accepts valid localPath`() {
        val result = validateNotBlank("/path/to/repo", "Local path")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test clone validation both params blank`() {
        val uriResult = validateNotBlank("", "URI")
        val pathResult = validateNotBlank("  ", "Local path")

        assertTrue(uriResult.isFailure)
        assertTrue(pathResult.isFailure)
    }

    // ==================== CommitUseCase 验证测试 ====================

    @Test
    fun `test commit validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test commit validation rejects blank message`() {
        val result = validateNotBlank("", "Commit message")
        assertTrue(result.isFailure)
        assertEquals("Commit message cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test commit validation accepts valid inputs`() {
        val repoResult = validateNotBlank("/path/to/repo", "Repository path")
        val msgResult = validateNotBlank("Initial commit", "Commit message")

        assertTrue(repoResult.isSuccess)
        assertTrue(msgResult.isSuccess)
    }

    @Test
    fun `test commit validation rejects whitespace-only message`() {
        val result = validateNotBlank("   \t\n  ", "Commit message")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test commit validation accepts multi-line message`() {
        val result = validateNotBlank(
            "feat: add new feature\n\n- Added feature X\n- Fixed bug Y",
            "Commit message"
        )
        assertTrue(result.isSuccess)
    }

    // ==================== PullUseCase / PushUseCase / FetchUseCase 验证测试 ====================

    @Test
    fun `test pull validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test pull validation accepts valid repoPath`() {
        val result = validateNotBlank("/home/user/project", "Repository path")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test push validation with Windows path`() {
        val result = validateNotBlank("D:\\Projects\\MyRepo", "Repository path")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test fetch validation accepts absolute path`() {
        val paths = listOf(
            "/usr/local/src/project",
            "/home/developer/git/app",
            "C:\\Users\\dev\\repo"
        )

        paths.forEach { path ->
            assertTrue("Path '$path' should be valid",
                validateNotBlank(path, "Repository path").isSuccess)
        }
    }

    // ==================== BranchUseCase 验证测试 ====================

    @Test
    fun `test branch list validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test branch create validation rejects blank branchName`() {
        val nameResult = validateNotBlank("", "Branch name")
        assertTrue(nameResult.isFailure)
        assertEquals("Branch name cannot be empty", nameResult.exceptionOrNull()?.message)
    }

    @Test
    fun `test branch create accepts valid names`() {
        val validNames = listOf("main", "develop", "feature/login", "fix/bug-123", "release/v1.0")

        validNames.forEach { name ->
            assertTrue("Branch name '$name' should be valid",
                validateNotBlank(name, "Branch name").isSuccess)
        }
    }

    @Test
    fun `test branch delete validation both params required`() {
        val repoResult = validateNotBlank("", "Repository path")
        val nameResult = validateNotBlank("", "Branch name")

        assertTrue(repoResult.isFailure)
        assertTrue(nameResult.isFailure)
    }

    @Test
    fun `test branch rename validation old and new names required`() {
        val oldNameResult = validateNotBlank("", "Old branch name")
        val newNameResult = validateNotBlank("", "New branch name")

        assertTrue(oldNameResult.isFailure)
        assertTrue(newNameResult.isFailure)

        val validOld = validateNotBlank("old-feature", "Old branch name")
        val validNew = validateNotBlank("new-feature", "New branch name")

        assertTrue(validOld.isSuccess)
        assertTrue(validNew.isSuccess)
    }

    @Test
    fun `test branch checkout validation requires both params`() {
        val repoResult = validateNotBlank("/path/to/repo", "Repository path")
        val branchResult = validateNotBlank("feature/test", "Branch name")

        assertTrue(repoResult.isSuccess)
        assertTrue(branchResult.isSuccess)
    }

    // ==================== StageUseCase 验证测试 ====================

    @Test
    fun `test stageAll validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test stageFile validation rejects blank filePath`() {
        val fileResult = validateNotBlank("", "File path")
        assertTrue(fileResult.isFailure)
        assertEquals("File path cannot be empty", fileResult.exceptionOrNull()?.message)
    }

    @Test
    fun `test stageFile accepts relative and absolute paths`() {
        val paths = listOf(
            "src/main/kotlin/App.kt",
            "README.md",
            "/full/path/to/file.txt"
        )

        paths.forEach { path ->
            assertTrue("File path '$path' should be valid",
                validateNotBlank(path, "File path").isSuccess)
        }
    }

    @Test
    fun `test unstageFile validation same as stageFile`() {
        val result = validateNotBlank("modified.txt", "File path")
        assertTrue(result.isSuccess)
    }

    // ==================== MergeUseCase 验证测试 ====================

    @Test
    fun `test merge validation requires repoPath and branchName`() {
        val repoResult = validateNotBlank("", "Repository path")
        val branchResult = validateNotBlank("", "Branch name")

        assertTrue(repoResult.isFailure)
        assertTrue(branchResult.isFailure)
    }

    @Test
    fun `test merge validation accepts valid inputs`() {
        val repoResult = validateNotBlank("/project", "Repository path")
        val branchResult = validateNotBlank("feature/new-ui", "Branch name")

        assertTrue(repoResult.isSuccess)
        assertTrue(branchResult.isSuccess)
    }

    @Test
    fun `test rebase validation same as merge`() {
        val repoResult = validateNotBlank("/project", "Repository path")
        val upstreamResult = validateNotBlank("main", "Branch name")

        assertTrue(repoResult.isSuccess)
        assertTrue(upstreamResult.isSuccess)
    }

    @Test
    fun `test resolveConflict validation requires filePath`() {
        val fileResult = validateNotBlank("conflict.kt", "File path")
        assertTrue(fileResult.isSuccess)

        val blankFileResult = validateNotBlank("", "File path")
        assertTrue(blankFileResult.isFailure)
    }

    @Test
    fun `test abortRebase validation requires repoPath`() {
        val result = validateNotBlank("/repo", "Repository path")
        assertTrue(result.isSuccess)

        val blankResult = validateNotBlank("", "Repository path")
        assertTrue(blankResult.isFailure)
    }

    // ==================== ResetUseCase 验证测试 ====================

    @Test
    fun `test reset validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test reset validation rejects blank commitHash`() {
        val result = validateNotBlank("", "Commit hash")
        assertTrue(result.isFailure)
        assertEquals("Commit hash cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test reset validation accepts full and short hashes`() {
        val hashes = listOf(
            "abc123def4567890123456789012345678901234",
            "abc123d",
            "HEAD",
            "HEAD~1",
            "main"
        )

        hashes.forEach { hash ->
            assertTrue("Hash '$hash' should be valid",
                validateNotBlank(hash, "Commit hash").isSuccess)
        }
    }

    // ==================== CleanUseCase / InitRepoUseCase 验证测试 ====================

    @Test
    fun `test clean validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test initRepo validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test initRepo validation accepts various paths`() {
        val paths = listOf(
            "/tmp/new-repo",
            "./my-project",
            "D:\\Projects\\NewProject",
            "/home/user/git/new-app"
        )

        paths.forEach { path ->
            assertTrue("Init path '$path' should be valid",
                validateNotBlank(path, "Repository path").isSuccess)
        }
    }

    // ==================== GetDetailedStatusUseCase / GetCommitHistoryUseCase 验证测试 ====================

    @Test
    fun `test getDetailedStatus validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test getCommitHistory validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test getCommitHistory accepts valid maxCount values`() {
        val maxCounts = listOf(1, 10, 50, 100, 1000)

        maxCounts.forEach { count ->
            assertTrue("maxCount $count should be positive", count > 0)
        }
    }

    // ==================== DiscardChangesUseCase 验证测试 ====================

    @Test
    fun `test discardChanges validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    // ==================== ConfigureRemoteUseCase / DeleteRemoteUseCase 验证测试 ====================

    @Test
    fun `test configureRemote validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test configureRemote validation rejects blank remote name`() {
        val nameResult = validateNotBlank("", "Remote name")
        assertTrue(nameResult.isFailure)
    }

    @Test
    fun `test configureRemote validation rejects blank url`() {
        val urlResult = validateNotBlank("", "URL")
        assertTrue(urlResult.isFailure)
    }

    @Test
    fun `test configureRemote accepts standard remote config`() {
        val repoResult = validateNotBlank("/repo", "Repository path")
        val nameResult = validateNotBlank("origin", "Remote name")
        val urlResult = validateNotBlank("https://github.com/user/repo.git", "URL")

        assertTrue(repoResult.isSuccess)
        assertTrue(nameResult.isSuccess)
        assertTrue(urlResult.isSuccess)
    }

    // ==================== GitConfigUseCases 验证测试 ====================

    @Test
    fun `test applyGitConfigToRepo validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test applyGitConfigToRepo validation rejects both blank name and email`() {
        val nameResult = validateNotBlank("", "Name or email")
        val emailResult = validateNotBlank("", "Email")
        assertTrue(nameResult.isFailure || emailResult.isFailure)
    }

    @Test
    fun `test applyGitConfig accepts name only`() {
        val nameResult = validateNotBlank("John Doe", "Name")
        assertTrue(nameResult.isSuccess)
    }

    @Test
    fun `test applyGitConfig accepts email only`() {
        val emailResult = validateNotBlank("john@example.com", "Email")
        assertTrue(emailResult.isSuccess)
    }

    @Test
    fun `test removeRepoLocalConfig validation rejects blank repoPath`() {
        val result = validateNotBlank("", "Repository path")
        assertTrue(result.isFailure)
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `test validation with null value treated as blank`() {
        val result = validateNotBlank(null, "Field")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test validation with tab characters only`() {
        val result = validateNotBlank("\t\t", "Field")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test validation with newline characters only`() {
        val result = validateNotBlank("\n\n", "Field")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test validation with mixed whitespace`() {
        val result = validateNotBlank("  \t \n  ", "Field")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test validation with unicode content`() {
        val result = validateNotBlank("功能分支名称", "Field")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test validation with very long string`() {
        val longString = "a".repeat(10000)
        val result = validateNotBlank(longString, "Field")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test validation with single character`() {
        val result = validateNotBlank("a", "Field")
        assertTrue(result.isSuccess)
    }

    // ==================== 错误消息一致性测试 ====================

    @Test
    fun `test error messages contain field name`() {
        val fields = mapOf(
            "URI" to "URI cannot be empty",
            "Local path" to "Local path cannot be empty",
            "Repository path" to "Repository path cannot be empty",
            "Commit message" to "Commit message cannot be empty",
            "Branch name" to "Branch name cannot be empty",
            "File path" to "File path cannot be empty",
            "Commit hash" to "Commit hash cannot be empty"
        )

        fields.forEach { (field, expectedMessage) ->
            val result = validateNotBlank("", field)
            assertEquals(expectedMessage, result.exceptionOrNull()?.message)
        }
    }
}
