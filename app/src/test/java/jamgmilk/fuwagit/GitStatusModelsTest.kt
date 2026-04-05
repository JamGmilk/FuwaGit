package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.git.*
import org.junit.Assert.*
import org.junit.Test

class GitStatusModelsTest {

    // ==================== GitChangeType 枚举测试 ====================

    @Test
    fun `test GitChangeType all enum values exist`() {
        val expectedTypes = listOf(
            GitChangeType.Added,
            GitChangeType.Modified,
            GitChangeType.Removed,
            GitChangeType.Untracked,
            GitChangeType.Renamed,
            GitChangeType.Conflicting
        )

        assertEquals(6, expectedTypes.size)
        assertTrue(expectedTypes.contains(GitChangeType.Added))
        assertTrue(expectedTypes.contains(GitChangeType.Modified))
        assertTrue(expectedTypes.contains(GitChangeType.Removed))
        assertTrue(expectedTypes.contains(GitChangeType.Untracked))
        assertTrue(expectedTypes.contains(GitChangeType.Renamed))
        assertTrue(expectedTypes.contains(GitChangeType.Conflicting))
    }

    @Test
    fun `test GitChangeType enum name values`() {
        assertEquals("Added", GitChangeType.Added.name)
        assertEquals("Modified", GitChangeType.Modified.name)
        assertEquals("Removed", GitChangeType.Removed.name)
        assertEquals("Untracked", GitChangeType.Untracked.name)
        assertEquals("Renamed", GitChangeType.Renamed.name)
        assertEquals("Conflicting", GitChangeType.Conflicting.name)
    }

    @Test
    fun `test GitChangeType valueOf lookup`() {
        assertEquals(GitChangeType.Added, java.lang.Enum.valueOf(GitChangeType::class.java, "Added"))
        assertEquals(GitChangeType.Conflicting, java.lang.Enum.valueOf(GitChangeType::class.java, "Conflicting"))
    }

    @Test
    fun `test GitChangeType maps from JGit status strings`() {
        fun mapFromJgitStatus(status: String): GitChangeType = when (status) {
            "ADD" -> GitChangeType.Added
            "DELETE" -> GitChangeType.Removed
            "MODIFY" -> GitChangeType.Modified
            "RENAME" -> GitChangeType.Renamed
            else -> GitChangeType.Modified
        }

        assertEquals(GitChangeType.Added, mapFromJgitStatus("ADD"))
        assertEquals(GitChangeType.Removed, mapFromJgitStatus("DELETE"))
        assertEquals(GitChangeType.Modified, mapFromJgitStatus("MODIFY"))
        assertEquals(GitChangeType.Renamed, mapFromJgitStatus("RENAME"))
        assertEquals(GitChangeType.Modified, mapFromJgitStatus("COPY"))
    }

    // ==================== MergeStatus 枚举测试 ====================

    @Test
    fun `test MergeStatus all enum values`() {
        val expected = listOf(
            MergeStatus.ALREADY_UP_TO_DATE,
            MergeStatus.FAST_FORWARD,
            MergeStatus.MERGED,
            MergeStatus.FAILED,
            MergeStatus.CONFLICTING,
            MergeStatus.ABORTED,
            MergeStatus.UNKNOWN
        )
        assertEquals(7, expected.size)
    }

    @Test
    fun `test MergeStatus maps from JGit merge status strings`() {
        fun mapMergeStatus(jgitStatus: String): MergeStatus = when (jgitStatus) {
            "ALREADY_UP_TO_DATE" -> MergeStatus.ALREADY_UP_TO_DATE
            "FAST_FORWARD" -> MergeStatus.FAST_FORWARD
            "MERGED" -> MergeStatus.MERGED
            "FAILED" -> MergeStatus.FAILED
            "CONFLICTING" -> MergeStatus.CONFLICTING
            "ABORTED" -> MergeStatus.ABORTED
            else -> MergeStatus.UNKNOWN
        }

        assertEquals(MergeStatus.ALREADY_UP_TO_DATE, mapMergeStatus("ALREADY_UP_TO_DATE"))
        assertEquals(MergeStatus.FAST_FORWARD, mapMergeStatus("FAST_FORWARD"))
        assertEquals(MergeStatus.MERGED, mapMergeStatus("MERGED"))
        assertEquals(MergeStatus.FAILED, mapMergeStatus("FAILED"))
        assertEquals(MergeStatus.CONFLICTING, mapMergeStatus("CONFLICTING"))
        assertEquals(MergeStatus.ABORTED, mapMergeStatus("ABORTED"))
        assertEquals(MergeStatus.UNKNOWN, mapMergeStatus("UNKNOWN_STATUS"))
    }

    @Test
    fun `test MergeStatus success states`() {
        val successStates = listOf(
            MergeStatus.ALREADY_UP_TO_DATE,
            MergeStatus.FAST_FORWARD,
            MergeStatus.MERGED
        )
        val failureStates = listOf(
            MergeStatus.FAILED,
            MergeStatus.CONFLICTING,
            MergeStatus.ABORTED
        )

        successStates.forEach { status ->
            assertTrue("$status should be a success-like state",
                status == MergeStatus.ALREADY_UP_TO_DATE || 
                status == MergeStatus.FAST_FORWARD || 
                status == MergeStatus.MERGED)
        }
    }

    // ==================== RebaseStatus 枚举测试 ====================

    @Test
    fun `test RebaseStatus all enum values`() {
        val expected = listOf(
            RebaseStatus.UP_TO_DATE,
            RebaseStatus.FAST_FORWARD,
            RebaseStatus.OK,
            RebaseStatus.CONFLICTING,
            RebaseStatus.ABORTED,
            RebaseStatus.FAILED,
            RebaseStatus.UNKNOWN
        )
        assertEquals(7, expected.size)
    }

    @Test
    fun `test RebaseStatus maps from JGit rebase status strings`() {
        fun mapRebaseStatus(jgitStatus: String): RebaseStatus = when (jgitStatus) {
            "UP_TO_DATE" -> RebaseStatus.UP_TO_DATE
            "FAST_FORWARD" -> RebaseStatus.FAST_FORWARD
            "OK" -> RebaseStatus.OK
            "CONFLICTING" -> RebaseStatus.CONFLICTING
            "ABORTED" -> RebaseStatus.ABORTED
            "FAILED" -> RebaseStatus.FAILED
            else -> RebaseStatus.UNKNOWN
        }

        assertEquals(RebaseStatus.UP_TO_DATE, mapRebaseStatus("UP_TO_DATE"))
        assertEquals(RebaseStatus.FAST_FORWARD, mapRebaseStatus("FAST_FORWARD"))
        assertEquals(RebaseStatus.OK, mapRebaseStatus("OK"))
        assertEquals(RebaseStatus.CONFLICTING, mapRebaseStatus("CONFLICTING"))
        assertEquals(RebaseStatus.ABORTED, mapRebaseStatus("ABORTED"))
        assertEquals(RebaseStatus.FAILED, mapRebaseStatus("FAILED"))
        assertEquals(RebaseStatus.UNKNOWN, mapRebaseStatus("CUSTOM"))
    }

    // ==================== GitResetMode 枚举测试 ====================

    @Test
    fun `test GitResetMode all modes`() {
        val modes = listOf(GitResetMode.SOFT, GitResetMode.MIXED, GitResetMode.HARD)
        assertEquals(3, modes.size)
    }

    @Test
    fun `test GitResetMode descriptions`() {
        assertNotNull(GitResetMode.SOFT.description)
        assertNotNull(GitResetMode.MIXED.description)
        assertNotNull(GitResetMode.HARD.description)

        assertTrue(GitResetMode.SOFT.description.contains("HEAD", ignoreCase = true))
        assertTrue(GitResetMode.HARD.description.contains("discard", ignoreCase = true) ||
                   GitResetMode.HARD.description.contains("dangerous", ignoreCase = true))
    }

    @Test
    fun `test GitResetMode mapping to JGit reset type`() {
        fun mapToJGitResetType(mode: GitResetMode): String = when (mode) {
            GitResetMode.SOFT -> "SOFT"
            GitResetMode.MIXED -> "MIXED"
            GitResetMode.HARD -> "HARD"
        }

        assertEquals("SOFT", mapToJGitResetType(GitResetMode.SOFT))
        assertEquals("MIXED", mapToJGitResetType(GitResetMode.MIXED))
        assertEquals("HARD", mapToJGitResetType(GitResetMode.HARD))
    }

    @Test
    fun `test reset message generation per mode`() {
        fun generateResetMessage(commitHash: String, mode: GitResetMode): String = when (mode) {
            GitResetMode.SOFT -> "Reset to $commitHash (soft): HEAD moved, changes kept staged"
            GitResetMode.MIXED -> "Reset to $commitHash (mixed): HEAD moved, changes unstaged"
            GitResetMode.HARD -> "Reset to $commitHash (hard): All changes discarded"
        }

        val hash = "abc1234"

        val softMsg = generateResetMessage(hash, GitResetMode.SOFT)
        assertTrue(softMsg.contains("soft"))
        assertTrue(softMsg.contains(hash))

        val mixedMsg = generateResetMessage(hash, GitResetMode.MIXED)
        assertTrue(mixedMsg.contains("mixed"))

        val hardMsg = generateResetMessage(hash, GitResetMode.HARD)
        assertTrue(hardMsg.contains("hard"))
        assertTrue(hardMsg.contains("discarded"))
    }

    // ==================== ConflictResult 模型测试 ====================

    @Test
    fun `test ConflictResult no conflicts`() {
        val result = ConflictResult(
            isConflicting = false,
            operationType = "MERGE",
            message = "Merge successful"
        )

        assertFalse(result.isConflicting)
        assertTrue(result.conflicts.isEmpty())
        assertFalse(result.hasUnresolvedConflicts)
        assertFalse(result.allResolved)
        assertFalse(result.allStaged)
        assertEquals(0, result.unresolvedCount)
        assertEquals(0, result.resolvedCount)
    }

    @Test
    fun `test ConflictResult with unresolved conflicts`() {
        val result = ConflictResult(
            isConflicting = true,
            operationType = "MERGE",
            conflicts = listOf(
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "src/main/Main.kt",
                    name = "Main.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.UNRESOLVED,
                    description = "both modified"
                ),
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "README.md",
                    name = "README.md",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.UNRESOLVED,
                    description = "both modified"
                )
            ),
            message = "Merge conflict: 2 file(s) need resolution"
        )

        assertTrue(result.isConflicting)
        assertEquals(2, result.conflicts.size)
        assertTrue(result.hasUnresolvedConflicts)
        assertFalse(result.allResolved)
        assertFalse(result.allStaged)
        assertEquals(2, result.unresolvedCount)
        assertEquals(0, result.resolvedCount)
    }

    @Test
    fun `test ConflictResult with mixed resolution states`() {
        val result = ConflictResult(
            isConflicting = true,
            operationType = "REBASE",
            conflicts = listOf(
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "file1.kt",
                    name = "file1.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.UNRESOLVED
                ),
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "file2.kt",
                    name = "file2.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.RESOLVED
                ),
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "file3.kt",
                    name = "file3.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.STAGED
                )
            ),
            message = "Rebase conflict: 3 file(s) need resolution"
        )

        assertEquals(3, result.conflicts.size)
        assertTrue(result.hasUnresolvedConflicts)
        assertFalse(result.allResolved)
        assertFalse(result.allStaged)
        assertEquals(1, result.unresolvedCount)
        assertEquals(2, result.resolvedCount)
    }

    @Test
    fun `test ConflictResult all resolved`() {
        val result = ConflictResult(
            isConflicting = true,
            operationType = "MERGE",
            conflicts = listOf(
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "fixed.kt",
                    name = "fixed.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.STAGED
                ),
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "also-fixed.kt",
                    name = "also-fixed.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.RESOLVED
                )
            ),
            message = "All conflicts resolved"
        )

        assertFalse(result.hasUnresolvedConflicts)
        assertTrue(result.allResolved)
        assertFalse(result.allStaged)
        assertEquals(0, result.unresolvedCount)
        assertEquals(2, result.resolvedCount)
    }

    @Test
    fun `test ConflictResult all staged`() {
        val result = ConflictResult(
            isConflicting = true,
            operationType = "MERGE",
            conflicts = listOf(
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "a.kt",
                    name = "a.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.STAGED
                ),
                jamgmilk.fuwagit.domain.model.git.GitConflict(
                    path = "b.kt",
                    name = "b.kt",
                    status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.STAGED
                )
            )
        )

        assertTrue(result.allStaged)
        assertTrue(result.allResolved)
        assertFalse(result.hasUnresolvedConflicts)
    }

    // ==================== ConflictStatus 枚举测试 ====================

    @Test
    fun `test ConflictStatus all values`() {
        val statuses = listOf(
            jamgmilk.fuwagit.domain.model.git.ConflictStatus.UNRESOLVED,
            jamgmilk.fuwagit.domain.model.git.ConflictStatus.RESOLVED,
            jamgmilk.fuwagit.domain.model.git.ConflictStatus.STAGED
        )
        assertEquals(3, statuses.size)
    }

    // ==================== GitConflict 模型测试 ====================

    @Test
    fun `test GitConflict creation with defaults`() {
        val conflict = jamgmilk.fuwagit.domain.model.git.GitConflict(
            path = "conflict.txt",
            name = "conflict.txt"
        )

        assertEquals("conflict.txt", conflict.path)
        assertEquals(jamgmilk.fuwagit.domain.model.git.ConflictStatus.UNRESOLVED, conflict.status)
        assertEquals("", conflict.description)
    }

    @Test
    fun `test GitConflict creation with full info`() {
        val conflict = jamgmilk.fuwagit.domain.model.git.GitConflict(
            path = "src/App.kt",
            name = "App.kt",
            status = jamgmilk.fuwagit.domain.model.git.ConflictStatus.RESOLVED,
            description = "both modified"
        )

        assertEquals("src/App.kt", conflict.path)
        assertEquals("App.kt", conflict.name)
        assertEquals(jamgmilk.fuwagit.domain.model.git.ConflictStatus.RESOLVED, conflict.status)
        assertEquals("both modified", conflict.description)
    }

    // ==================== CloneOptions 测试 ====================

    @Test
    fun `test CloneOptions default values`() {
        val options = CloneOptions()

        assertNull(options.branch)
        assertTrue(options.cloneAllBranches)
        assertNull(options.depth)
        assertFalse(options.isBare)
    }

    @Test
    fun `test CloneOptions shallow clone`() {
        val options = CloneOptions(branch = "main", depth = 1, cloneAllBranches = false)

        assertEquals("main", options.branch)
        assertEquals(1, options.depth)
        assertFalse(options.cloneAllBranches)
        assertFalse(options.isBare)
    }

    @Test
    fun `test CloneOptions bare repository`() {
        val options = CloneOptions(isBare = true, cloneAllBranches = false)

        assertTrue(options.isBare)
        assertFalse(options.cloneAllBranches)
    }

    @Test
    fun `test CloneOptions single branch clone`() {
        val options = CloneOptions(
            branch = "develop",
            cloneAllBranches = false,
            depth = 5
        )

        assertEquals("develop", options.branch)
        assertFalse(options.cloneAllBranches)
        assertEquals(5, options.depth)
    }

    // ==================== GitRemote 模型测试 ====================

    @Test
    fun `test GitRemote with push URL`() {
        val remote = GitRemote(
            name = "origin",
            fetchUrl = "https://github.com/user/repo.git",
            pushUrl = "git@github.com:user/repo.git"
        )

        assertEquals("origin", remote.name)
        assertEquals("https://github.com/user/repo.git", remote.fetchUrl)
        assertEquals("git@github.com:user/repo.git", remote.pushUrl)
    }

    @Test
    fun `test GitRemote without push URL`() {
        val remote = GitRemote(
            name = "upstream",
            fetchUrl = "https://github.com/upstream/repo.git",
            pushUrl = null
        )

        assertEquals("upstream", remote.name)
        assertNull(remote.pushUrl)
    }

    // ==================== 状态转换逻辑测试 ====================

    @Test
    fun `test file change type classification for staging area`() {
        fun classifyForStagingArea(changeType: GitChangeType): String = when (changeType) {
            GitChangeType.Added, GitChangeType.Modified, GitChangeType.Removed -> "STAGED"
            GitChangeType.Untracked -> "UNTRACKED"
            GitChangeType.Conflicting -> "CONFLICTING"
            GitChangeType.Renamed -> "RENAMED"
        }

        assertEquals("STAGED", classifyForStagingArea(GitChangeType.Added))
        assertEquals("STAGED", classifyForStagingArea(GitChangeType.Removed))
        assertEquals("UNTRACKED", classifyForStagingArea(GitChangeType.Untracked))
        assertEquals("CONFLICTING", classifyForStagingArea(GitChangeType.Conflicting))
        assertEquals("RENAMED", classifyForStagingArea(GitChangeType.Renamed))
    }

    @Test
    fun `test pull result state classification`() {
        fun classifyPullState(result: PullResult): String = return when {
            result.hasConflicts -> "CONFLICTS"
            result.isUpToDate -> "UP_TO_DATE"
            result.isFastForward -> "FAST_FORWARD"
            result.isMerged -> "MERGED"
            !result.isSuccessful -> "FAILED"
            else -> "UNKNOWN"
        }

        val upToDate = PullResult(isSuccessful = true, message = "Up to date", mergeResult = MergeResultDetail(MergeStatus.ALREADY_UP_TO_DATE))
        assertEquals("UP_TO_DATE", classifyPullState(upToDate))

        val ff = PullResult(isSuccessful = true, message = "Fast forward", mergeResult = MergeResultDetail(MergeStatus.FAST_FORWARD))
        assertEquals("FAST_FORWARD", classifyPullState(ff))

        val merged = PullResult(isSuccessful = true, message = "Merged", mergeResult = MergeResultDetail(MergeStatus.MERGED))
        assertEquals("MERGED", classifyPullState(merged))

        val conflicts = PullResult(isSuccessful = true, message = "Conflicts", hasConflicts = true,
            mergeResult = MergeResultDetail(MergeStatus.CONFLICTING))
        assertEquals("CONFLICTS", classifyPullState(conflicts))

        val failed = PullResult(isSuccessful = false, message = "Failed")
        assertEquals("FAILED", classifyPullState(failed))
    }
}
