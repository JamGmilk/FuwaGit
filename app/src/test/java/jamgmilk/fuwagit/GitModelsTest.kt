package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.git.*
import org.junit.Assert.*
import org.junit.Test

class GitModelsTest {

    // ==================== GitCommit 测试 ====================

    @Test
    fun `test GitCommit creation with all fields`() {
        val commit = GitCommit(
            hash = "abc123def4567890123456789012345678901234",
            shortHash = "abc123d",
            authorName = "Test Author",
            authorEmail = "test@example.com",
            message = "Initial commit",
            timestamp = 1700000000000L,
            parentHashes = listOf("parent1", "parent2")
        )

        assertEquals("abc123def4567890123456789012345678901234", commit.hash)
        assertEquals("abc123d", commit.shortHash)
        assertEquals("Test Author", commit.authorName)
        assertEquals("test@example.com", commit.authorEmail)
        assertEquals("Initial commit", commit.message)
        assertEquals(1700000000000L, commit.timestamp)
        assertEquals(2, commit.parentHashes.size)
    }

    @Test
    fun `test GitCommit isMerge with single parent`() {
        val normalCommit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            authorName = "Author",
            authorEmail = "a@b.com",
            message = "Normal commit",
            timestamp = 1000L,
            parentHashes = listOf("parent1")
        )
        assertFalse("Single parent should not be merge", normalCommit.isMerge)
    }

    @Test
    fun `test GitCommit isMerge with multiple parents`() {
        val mergeCommit = GitCommit(
            hash = "def456",
            shortHash = "def456",
            authorName = "Author",
            authorEmail = "a@b.com",
            message = "Merge branch",
            timestamp = 2000L,
            parentHashes = listOf("parent1", "parent2")
        )
        assertTrue("Multiple parents should indicate merge", mergeCommit.isMerge)
    }

    @Test
    fun `test GitCommit default parentHashes is empty`() {
        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L
        )
        assertTrue("Default parentHashes should be empty", commit.parentHashes.isEmpty())
        assertFalse("No parents means not a merge", commit.isMerge)
    }

    // ==================== GitFileStatus 测试 ====================

    @Test
    fun `test GitFileStatus for staged added file`() {
        val status = GitFileStatus(
            path = "src/main/Main.kt",
            name = "Main.kt",
            isStaged = true,
            changeType = GitChangeType.Added
        )

        assertEquals("src/main/Main.kt", status.path)
        assertEquals("Main.kt", status.name)
        assertTrue(status.isStaged)
        assertEquals(GitChangeType.Added, status.changeType)
    }

    @Test
    fun `test GitFileStatus for unstaged modified file`() {
        val status = GitFileStatus(
            path = "README.md",
            name = "README.md",
            isStaged = false,
            changeType = GitChangeType.Modified
        )
        assertFalse(status.isStaged)
        assertEquals(GitChangeType.Modified, status.changeType)
    }

    @Test
    fun `test GitFileStatus for conflicting file`() {
        val status = GitFileStatus(
            path = "conflict.txt",
            name = "conflict.txt",
            isStaged = false,
            changeType = GitChangeType.Conflicting
        )
        assertEquals(GitChangeType.Conflicting, status.changeType)
    }

    // ==================== GitBranch 测试 ====================

    @Test
    fun `test GitBranch local current branch`() {
        val branch = GitBranch(
            name = "main",
            fullRef = "refs/heads/main",
            isRemote = false,
            isCurrent = true
        )

        assertEquals("main", branch.name)
        assertEquals("refs/heads/main", branch.fullRef)
        assertFalse(branch.isRemote)
        assertTrue(branch.isCurrent)
    }

    @Test
    fun `test GitBranch remote tracking branch`() {
        val branch = GitBranch(
            name = "origin/feature",
            fullRef = "refs/remotes/origin/feature",
            isRemote = true,
            isCurrent = false
        )

        assertTrue(branch.isRemote)
        assertFalse(branch.isCurrent)
        assertTrue(branch.fullRef.startsWith("refs/remotes/"))
    }

    // ==================== GitRepoStatus 测试 ====================

    @Test
    fun `test GitRepoStatus clean repository`() {
        val status = GitRepoStatus(
            isGitRepo = true,
            branch = "main",
            hasUncommittedChanges = false,
            untrackedCount = 0,
            message = "Clean"
        )

        assertTrue(status.isGitRepo)
        assertFalse(status.hasUncommittedChanges)
        assertEquals(0, status.untrackedCount)
        assertEquals("Clean", status.message)
    }

    @Test
    fun `test GitRepoStatus dirty repository`() {
        val status = GitRepoStatus(
            isGitRepo = true,
            branch = "develop",
            hasUncommittedChanges = true,
            untrackedCount = 3,
            message = "Changes detected"
        )

        assertTrue(status.hasUncommittedChanges)
        assertEquals(3, status.untrackedCount)
        assertEquals("develop", status.branch)
    }

    // ==================== PullResult 测试 ====================

    @Test
    fun `test PullResult successful fast-forward`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Pull successful",
            mergeResult = MergeResultDetail(
                mergeStatus = MergeStatus.FAST_FORWARD,
                commitCount = 5,
                fastForward = true
            ),
            detailMessage = "Pull successful. Fast-forward merge with 5 commit(s)."
        )

        assertTrue(result.isSuccessful)
        assertTrue(result.isFastForward)
        assertFalse(result.isUpToDate)
        assertFalse(result.hasConflicts)
        assertEquals(5, result.commitCount)
    }

    @Test
    fun `test PullResult already up to date`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Pull successful",
            mergeResult = MergeResultDetail(
                mergeStatus = MergeStatus.ALREADY_UP_TO_DATE
            ),
            detailMessage = "Pull successful. Already up-to-date."
        )

        assertTrue(result.isSuccessful)
        assertTrue(result.isUpToDate)
        assertFalse(result.isFastForward)
        assertEquals(0, result.commitCount)
    }

    @Test
    fun `test PullResult with conflicts`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Pull successful",
            mergeResult = MergeResultDetail(
                mergeStatus = MergeStatus.CONFLICTING,
                conflicts = mapOf("file.txt" to 1)
            ),
            hasConflicts = true,
            detailMessage = "Pull successful. Merge conflicts detected."
        )

        assertTrue(result.hasConflicts)
        assertEquals(MergeStatus.CONFLICTING, result.mergeResult?.mergeStatus)
    }

    @Test
    fun `test PullResult failed`() {
        val result = PullResult(
            isSuccessful = false,
            message = "Pull failed"
        )

        assertFalse(result.isSuccessful)
        assertNull(result.mergeResult)
    }

    @Test
    fun `test PullResult with rebase result`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Pull successful",
            rebaseResult = RebaseResultDetail(
                status = RebaseStatus.OK,
                commitCount = 3
            )
        )

        assertNotNull(result.rebaseResult)
        assertEquals(RebaseStatus.OK, result.rebaseResult?.status)
        assertEquals(3, result.rebaseResult?.commitCount)
    }

    // ==================== MergeResultDetail 测试 ====================

    @Test
    fun `test MergeResultDetail merged status`() {
        val detail = MergeResultDetail(
            mergeStatus = MergeStatus.MERGED,
            commitCount = 10,
            fastForward = false
        )

        assertEquals(MergeStatus.MERGED, detail.mergeStatus)
        assertEquals(10, detail.commitCount)
        assertFalse(detail.fastForward)
    }

    @Test
    fun `test MergeResultDetail with conflict map`() {
        val detail = MergeResultDetail(
            mergeStatus = MergeStatus.CONFLICTING,
            conflicts = mapOf(
                "file1.kt" to 2,
                "file2.kt" to 1
            )
        )

        assertEquals(2, detail.conflicts.size)
        assertTrue(detail.conflicts.containsKey("file1.kt"))
    }

    // ==================== RebaseResultDetail 测试 ====================

    @Test
    fun `test RebaseResultDetail conflicting`() {
        val detail = RebaseResultDetail(
            status = RebaseStatus.CONFLICTING,
            conflicts = listOf("conflict.kt", "merge.kt")
        )

        assertEquals(RebaseStatus.CONFLICTING, detail.status)
        assertEquals(2, detail.conflicts.size)
    }

    @Test
    fun `test RebaseResultDetail ok status`() {
        val detail = RebaseResultDetail(
            status = RebaseStatus.OK,
            commitCount = 5
        )

        assertEquals(RebaseStatus.OK, detail.status)
        assertEquals(5, detail.commitCount)
        assertTrue(detail.conflicts.isEmpty())
    }

    // ==================== FetchResult 测试 ====================

    @Test
    fun `test FetchResult successful`() {
        val result = FetchResult(
            isSuccessful = true,
            messages = listOf("From origin", "   * [new branch] feature -> origin/feature")
        )

        assertTrue(result.isSuccessful)
        assertEquals(2, result.messages.size)
    }

    @Test
    fun `test FetchResult failed`() {
        val result = FetchResult(
            isSuccessful = false,
            messages = listOf("Could not reach remote")
        )

        assertFalse(result.isSuccessful)
    }

    // ==================== CleanResult 测试 ====================

    @Test
    fun `test CleanResult with files`() {
        val result = CleanResult(
            files = listOf("build/", ".gradle/", "temp.txt"),
            isDryRun = false
        )

        assertFalse(result.isDryRun)
        assertFalse(result.isEmpty)
        assertEquals(3, result.count)
        assertTrue(result.files.contains("temp.txt"))
    }

    @Test
    fun `test CleanResult dry run empty`() {
        val result = CleanResult(
            files = emptyList(),
            isDryRun = true
        )

        assertTrue(result.isDryRun)
        assertTrue(result.isEmpty)
        assertEquals(0, result.count)
    }

    // ==================== GitCommitFileChange 测试 ====================

    @Test
    fun `test GitCommitFileChange addition`() {
        val change = GitCommitFileChange(
            path = "newfile.kt",
            name = "newfile.kt",
            changeType = GitChangeType.Added,
            additions = 50,
            deletions = 5
        )

        assertEquals(GitChangeType.Added, change.changeType)
        assertEquals(50, change.additions)
        assertEquals(5, change.deletions)
        assertEquals(55, change.totalChanges)
    }

    @Test
    fun `test GitCommitFileChange deletion`() {
        val change = GitCommitFileChange(
            path = "oldfile.kt",
            name = "oldfile.kt",
            changeType = GitChangeType.Removed,
            additions = 0,
            deletions = 100
        )

        assertEquals(100, change.totalChanges)
    }

    @Test
    fun `test GitCommitFileChange defaults`() {
        val change = GitCommitFileChange(
            path = "modified.kt",
            name = "modified.kt",
            changeType = GitChangeType.Modified
        )

        assertEquals(0, change.additions)
        assertEquals(0, change.deletions)
        assertEquals(0, change.totalChanges)
    }

    // ==================== GitCommitDetail 测试 ====================

    @Test
    fun `test GitCommitDetail with file changes`() {
        val commit = GitCommit(
            hash = "hash1",
            shortHash = "h1",
            authorName = "Author",
            authorEmail = "a@b.com",
            message = "Feature",
            timestamp = 1000L
        )
        val changes = listOf(
            GitCommitFileChange("file1.kt", "file1.kt", GitChangeType.Added, 10, 2),
            GitCommitFileChange("file2.kt", "file2.kt", GitChangeType.Modified, 5, 3)
        )

        val detail = GitCommitDetail(
            commit = commit,
            fileChanges = changes,
            totalAdditions = 15,
            totalDeletions = 5,
            totalFiles = 2
        )

        assertEquals(commit, detail.commit)
        assertEquals(2, detail.fileChanges.size)
        assertEquals(15, detail.totalAdditions)
        assertEquals(5, detail.totalDeletions)
        assertEquals(20, detail.totalChanges)
        assertEquals(2, detail.totalFiles)
    }

    // ==================== CommitStats 测试 ====================

    @Test
    fun `test CommitStats calculation`() {
        val stats = CommitStats(
            totalCommits = 150,
            uniqueAuthors = 5,
            commitsToday = 3,
            commitsThisWeek = 12,
            commitsThisMonth = 45
        )

        assertEquals(150, stats.totalCommits)
        assertEquals(5, stats.uniqueAuthors)
        assertEquals(3, stats.commitsToday)
        assertEquals(12, stats.commitsThisWeek)
        assertEquals(45, stats.commitsThisMonth)
    }
}
