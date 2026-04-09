package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.git.*
import org.junit.Assert.*
import org.junit.Test

class GitModelsTest {

    // ==================== GitCommit 计算属性测试 ====================

    @Test
    fun `GitCommit isMerge returns true when multiple parents`() {
        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            authorName = "Author",
            authorEmail = "a@b.com",
            message = "Merge branch",
            timestamp = 1000L,
            parentHashes = listOf("parent1", "parent2")
        )
        assertTrue(commit.isMerge)
    }

    @Test
    fun `GitCommit isMerge returns false with single parent`() {
        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            authorName = "Author",
            authorEmail = "a@b.com",
            message = "Normal commit",
            timestamp = 1000L,
            parentHashes = listOf("parent1")
        )
        assertFalse(commit.isMerge)
    }

    @Test
    fun `GitCommit isMerge returns false with no parents`() {
        val commit = GitCommit(
            hash = "abc123",
            shortHash = "abc123",
            authorName = "Author",
            authorEmail = "a@b.com",
            message = "Initial commit",
            timestamp = 1000L
        )
        assertFalse(commit.isMerge)
        assertTrue(commit.isInitialCommit)
    }

    @Test
    fun `GitCommit shortMessage truncates long messages at 72 chars`() {
        val longMessage = "A".repeat(100)
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = longMessage,
            timestamp = 0L
        )
        assertEquals(72, commit.shortMessage.length)
        assertFalse(commit.shortMessage.endsWith("..."))
    }

    @Test
    fun `GitCommit shortMessage keeps short messages intact`() {
        val shortMessage = "Short message"
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = shortMessage,
            timestamp = 0L
        )
        assertEquals(shortMessage, commit.shortMessage)
    }

    @Test
    fun `GitCommit shortMessage takes first line only`() {
        val multiLineMessage = "First line\nSecond line\nThird line"
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = multiLineMessage,
            timestamp = 0L
        )
        assertEquals("First line", commit.shortMessage)
    }

    @Test
    fun `GitCommit relativeTime returns just now for recent time`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 30_000
        )
        assertEquals("just now", commit.relativeTime)
    }

    @Test
    fun `GitCommit relativeTime returns minutes`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 5 * 60_000
        )
        assertEquals("5m ago", commit.relativeTime)
    }

    @Test
    fun `GitCommit relativeTime returns hours`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 3 * 60 * 60_000
        )
        assertEquals("3h ago", commit.relativeTime)
    }

    @Test
    fun `GitCommit relativeTime returns days`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 5 * 24 * 60 * 60_000
        )
        assertEquals("5d ago", commit.relativeTime)
    }

    @Test
    fun `GitCommit relativeTime returns weeks`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 3 * 7 * 24 * 60 * 60_000
        )
        assertEquals("3w ago", commit.relativeTime)
    }

    @Test
    fun `GitCommit relativeTime returns months`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 8L * 30 * 24 * 60 * 60 * 1000
        )
        val result = commit.relativeTime
        assertTrue("Expected months format but got: $result", result.endsWith("mo ago"))
    }

    @Test
    fun `GitCommit relativeTime returns years`() {
        val now = System.currentTimeMillis()
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = now - 5L * 365 * 24 * 60 * 60 * 1000
        )
        val result = commit.relativeTime
        assertTrue("Expected years format but got: $result", result.endsWith("y ago"))
    }

    @Test
    fun `GitCommit formattedTimestamp formats correctly`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L
        )
        val formatted = commit.formattedTimestamp
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun `GitCommit authorDisplayName handles full name`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "John Doe",
            authorEmail = "john@example.com",
            message = "msg",
            timestamp = 0L
        )
        assertEquals("John D.", commit.authorDisplayName)
    }

    @Test
    fun `GitCommit authorDisplayName handles name with email format`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "John Doe <john@example.com>",
            authorEmail = "john@example.com",
            message = "msg",
            timestamp = 0L
        )
        assertEquals("John Doe", commit.authorDisplayName)
    }

    @Test
    fun `GitCommit authorDisplayName falls back to email username`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "",
            authorEmail = "john@example.com",
            message = "msg",
            timestamp = 0L
        )
        assertEquals("john", commit.authorDisplayName)
    }

    @Test
    fun `GitCommit authorDisplayName handles single name`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "John",
            authorEmail = "john@example.com",
            message = "msg",
            timestamp = 0L
        )
        assertEquals("John", commit.authorDisplayName)
    }

    @Test
    fun `GitCommit parentCount returns correct number`() {
        val commitWith3Parents = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L,
            parentHashes = listOf("p1", "p2", "p3")
        )
        assertEquals(3, commitWith3Parents.parentCount)

        val commitWithNoParents = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L
        )
        assertEquals(0, commitWithNoParents.parentCount)
    }

    @Test
    fun `GitCommit primaryParentHash returns first parent`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L,
            parentHashes = listOf("first", "second")
        )
        assertEquals("first", commit.primaryParentHash)
    }

    @Test
    fun `GitCommit primaryParentHash returns null for no parents`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L
        )
        assertNull(commit.primaryParentHash)
    }

    // ==================== GitCommitFileChange 计算属性测试 ====================

    @Test
    fun `GitCommitFileChange totalChanges calculates correctly`() {
        val change = GitCommitFileChange(
            path = "file.kt",
            name = "file.kt",
            changeType = GitChangeType.Added,
            additions = 50,
            deletions = 10
        )
        assertEquals(60, change.totalChanges)
    }

    @Test
    fun `GitCommitFileChange totalChanges with zeros`() {
        val change = GitCommitFileChange(
            path = "file.kt",
            name = "file.kt",
            changeType = GitChangeType.Added
        )
        assertEquals(0, change.totalChanges)
    }

    // ==================== GitCommitDetail 计算属性测试 ====================

    @Test
    fun `GitCommitDetail totalChanges calculates correctly`() {
        val commit = GitCommit(
            hash = "abc",
            shortHash = "abc",
            authorName = "A",
            authorEmail = "a@b.com",
            message = "msg",
            timestamp = 0L
        )
        val detail = GitCommitDetail(
            commit = commit,
            totalAdditions = 100,
            totalDeletions = 50
        )
        assertEquals(150, detail.totalChanges)
    }

    // ==================== PullResult 计算属性测试 ====================

    @Test
    fun `PullResult isUpToDate returns true for ALREADY_UP_TO_DATE`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Up to date",
            mergeResult = MergeResultDetail(MergeStatus.ALREADY_UP_TO_DATE)
        )
        assertTrue(result.isUpToDate)
        assertFalse(result.isFastForward)
        assertFalse(result.isMerged)
    }

    @Test
    fun `PullResult isFastForward returns true for FAST_FORWARD`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Fast forward",
            mergeResult = MergeResultDetail(MergeStatus.FAST_FORWARD, commitCount = 5)
        )
        assertTrue(result.isFastForward)
        assertFalse(result.isUpToDate)
        assertEquals(5, result.commitCount)
    }

    @Test
    fun `PullResult isMerged returns true for MERGED`() {
        val result = PullResult(
            isSuccessful = true,
            message = "Merged",
            mergeResult = MergeResultDetail(MergeStatus.MERGED, commitCount = 3)
        )
        assertTrue(result.isMerged)
        assertEquals(3, result.commitCount)
    }

    @Test
    fun `PullResult commitCount returns zero when no merge result`() {
        val result = PullResult(
            isSuccessful = false,
            message = "Failed"
        )
        assertEquals(0, result.commitCount)
    }

    // ==================== CleanResult 计算属性测试 ====================

    @Test
    fun `CleanResult isEmpty returns true for empty list`() {
        val result = CleanResult(files = emptyList(), isDryRun = false)
        assertTrue(result.isEmpty)
        assertEquals(0, result.count)
    }

    @Test
    fun `CleanResult isEmpty returns false for non-empty list`() {
        val result = CleanResult(files = listOf("file1", "file2"), isDryRun = false)
        assertFalse(result.isEmpty)
        assertEquals(2, result.count)
    }

    // ==================== GitBranch 测试 ====================

    @Test
    fun `GitBranch local current branch`() {
        val branch = GitBranch(
            name = "main",
            fullRef = "refs/heads/main",
            isRemote = false,
            isCurrent = true
        )
        assertEquals("main", branch.name)
        assertFalse(branch.isRemote)
        assertTrue(branch.isCurrent)
    }

    @Test
    fun `GitBranch remote tracking branch`() {
        val branch = GitBranch(
            name = "origin/main",
            fullRef = "refs/remotes/origin/main",
            isRemote = true,
            isCurrent = false
        )
        assertTrue(branch.isRemote)
        assertFalse(branch.isCurrent)
    }

    // ==================== GitRepoStatus 测试 ====================

    @Test
    fun `GitRepoStatus clean repository`() {
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
    }

    @Test
    fun `GitRepoStatus dirty repository`() {
        val status = GitRepoStatus(
            isGitRepo = true,
            branch = "develop",
            hasUncommittedChanges = true,
            untrackedCount = 5,
            message = "Changes"
        )
        assertTrue(status.hasUncommittedChanges)
        assertEquals(5, status.untrackedCount)
    }

    // ==================== MergeResultDetail 测试 ====================

    @Test
    fun `MergeResultDetail with conflicts map`() {
        val detail = MergeResultDetail(
            mergeStatus = MergeStatus.CONFLICTING,
            conflicts = mapOf("file1.kt" to 2, "file2.kt" to 1)
        )
        assertEquals(MergeStatus.CONFLICTING, detail.mergeStatus)
        assertEquals(2, detail.conflicts.size)
    }

    @Test
    fun `MergeResultDetail with fast forward flag`() {
        val detail = MergeResultDetail(
            mergeStatus = MergeStatus.FAST_FORWARD,
            commitCount = 10,
            fastForward = true
        )
        assertTrue(detail.fastForward)
        assertEquals(10, detail.commitCount)
    }

    // ==================== RebaseResultDetail 测试 ====================

    @Test
    fun `RebaseResultDetail with conflicts`() {
        val detail = RebaseResultDetail(
            status = RebaseStatus.CONFLICTING,
            conflicts = listOf("file1.kt", "file2.kt")
        )
        assertEquals(RebaseStatus.CONFLICTING, detail.status)
        assertEquals(2, detail.conflicts.size)
    }

    @Test
    fun `RebaseResultDetail OK status`() {
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
    fun `FetchResult successful with messages`() {
        val result = FetchResult(
            isSuccessful = true,
            messages = listOf("From origin", "   * [new branch] feature -> origin/feature")
        )
        assertTrue(result.isSuccessful)
        assertEquals(2, result.messages.size)
    }

    @Test
    fun `FetchResult failed`() {
        val result = FetchResult(
            isSuccessful = false,
            messages = listOf("Could not resolve remote")
        )
        assertFalse(result.isSuccessful)
    }
}