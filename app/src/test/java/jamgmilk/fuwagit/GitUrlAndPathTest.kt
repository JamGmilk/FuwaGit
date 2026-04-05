package jamgmilk.fuwagit

import org.junit.Assert.*
import org.junit.Test

class GitUrlAndPathTest {

    // ==================== 仓库名提取测试 ====================

    @Test
    fun `test extract repo name from HTTPS URL with git suffix`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
        }

        assertEquals("repo", extractRepoName("https://github.com/user/repo.git"))
    }

    @Test
    fun `test extract repo name from HTTPS URL without git suffix`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
        }

        assertEquals("project", extractRepoName("https://gitlab.com/group/project"))
    }

    @Test
    fun `test extract repo name from SSH URL`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
                .substringAfterLast(':')
        }

        assertEquals("repo", extractRepoName("git@github.com:user/repo.git"))
        assertEquals("my-app", extractRepoName("git@gitlab.com:team/my-app.git"))
    }

    @Test
    fun `test extract repo name from ssh protocol URL format`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
        }

        assertEquals("repo", extractRepoName("ssh://git@github.com/user/repo.git"))
    }

    @Test
    fun `test extract repo name from nested path`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
                .substringAfterLast(':')
        }

        assertEquals("app", extractRepoName("https://github.com/org/team/group/app.git"))
    }

    @Test
    fun `test extract repo name with hyphens and dots`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
        }

        assertEquals("my-awesome-project.v2", extractRepoName("https://github.com/user/my-awesome-project.v2.git"))
    }

    // ==================== 主机名提取测试 ====================

    @Test
    fun `test extract host from HTTPS URL`() {
        fun extractHost(url: String): String {
            return url.removePrefix("https://").removePrefix("http://").substringBefore('/')
        }

        assertEquals("github.com", extractHost("https://github.com/user/repo.git"))
        assertEquals("gitlab.com", extractHost("https://gitlab.com/group/project"))
        assertEquals("bitbucket.org", extractHost("https://bitbucket.org/team/repo"))
    }

    @Test
    fun `test extract host from SSH URL`() {
        fun extractHostFromSsh(url: String): String {
            return url.substringAfter('@').substringBefore(':')
        }

        assertEquals("github.com", extractHostFromSsh("git@github.com:user/repo.git"))
        assertEquals("gitlab.com", extractHostFromSsh("git@gitlab.com:group/project"))
    }

    @Test
    fun `test extract host from ssh protocol URL`() {
        fun extractHostFromSshProtocol(url: String): String {
            return url.removePrefix("ssh://").substringAfter('@').substringBefore('/')
        }

        assertEquals("github.com", extractHostFromSshProtocol("ssh://git@github.com/user/repo.git"))
    }

    // ==================== URL 类型检测测试 ====================

    @Test
    fun `test detect URL type for various formats`() {
        fun detectUrlType(url: String): String = when {
            url.startsWith("ssh://") -> "SSH_PROTOCOL"
            url.startsWith("git@") -> "SSH_SCP"
            url.startsWith("https://") -> "HTTPS"
            url.startsWith("http://") -> "HTTP"
            else -> "UNKNOWN"
        }

        assertEquals("SSH_SCP", detectUrlType("git@github.com:user/repo.git"))
        assertEquals("SSH_PROTOCOL", detectUrlType("ssh://git@github.com/user/repo.git"))
        assertEquals("HTTPS", detectUrlType("https://github.com/user/repo.git"))
        assertEquals("HTTP", detectUrlType("http://github.com/user/repo.git"))
        assertEquals("UNKNOWN", detectUrlType("ftp://server.com/repo"))
        assertEquals("UNKNOWN", detectUrlType("/local/path"))
    }

    @Test
    fun `test isRemoteUrl detection`() {
        fun isRemoteUrl(url: String): Boolean {
            return url.startsWith("https://") || url.startsWith("http://") ||
                   url.startsWith("git@") || url.startsWith("ssh://")
        }

        assertTrue(isRemoteUrl("https://github.com/user/repo"))
        assertTrue(isRemoteUrl("git@github.com:user/repo"))
        assertFalse(isRemoteUrl("/usr/local/repo"))
        assertFalse(isRemoteUrl("relative/path"))
    }

    // ==================== 路径处理测试 ====================

    @Test
    fun `test get display name from path with alias`() {
        fun getDisplayName(path: String, alias: String?): String {
            return alias ?: path.substringAfterLast('/').substringAfterLast('\\')
        }

        assertEquals("Custom Name", getDisplayName("/path/to/repo", "Custom Name"))
        assertEquals("my-repo", getDisplayName("/path/to/my-repo", null))
    }

    @Test
    fun `test get display name from Windows path`() {
        fun getDisplayName(path: String, alias: String?): String {
            return alias ?: path.substringAfterLast('/').substringAfterLast('\\')
        }

        assertEquals("MyProject", getDisplayName("D:\\Projects\\MyProject", null))
        assertEquals("repo", getDisplayName("C:/Users/dev/repo", null))
    }

    @Test
    fun `test get display name from relative path`() {
        fun getDisplayName(path: String, alias: String?): String {
            return alias ?: path.substringAfterLast('/').substringAfterLast('\\')
        }

        assertEquals("my-repo", getDisplayName("./my-repo", null))
        assertEquals("repo", getDisplayName("../repo", null))
    }

    @Test
    fun `test normalize repository path`() {
        fun normalizePath(path: String): String {
            return path.trim().replace('\\', '/').removeSuffix("/")
        }

        assertEquals("/path/to/repo", normalizePath("  /path/to/repo/  "))
        assertEquals("D:/Projects/MyRepo", normalizePath("D:\\Projects\\MyRepo\\"))
        assertEquals("./my-repo", normalizePath("./my-repo/"))
    }

    // ==================== 分支引用路径解析测试 ====================

    @Test
    fun `test parse remote branch reference`() {
        val remoteBranchRegex = Regex("^([^/]+)/(.+)$")

        val matchResult1 = remoteBranchRegex.find("origin/main")
        assertNotNull(matchResult1)
        assertEquals("origin", matchResult1?.groupValues?.get(1))
        assertEquals("main", matchResult1?.groupValues?.get(2))

        val matchResult2 = remoteBranchRegex.find("upstream/feature/login")
        assertNotNull(matchResult2)
        assertEquals("upstream", matchResult2?.groupValues?.get(1))
        assertEquals("feature/login", matchResult2?.groupValues?.get(2))
    }

    @Test
    fun `test parse local branch does not match remote pattern`() {
        val remoteBranchRegex = Regex("^([^/]+)/(.+)$")

        val matchResult = remoteBranchRegex.find("main")
        assertNull("Local branch 'main' should not match remote pattern", matchResult)
    }

    @Test
    fun `test parse deeply nested remote branch`() {
        val remoteBranchRegex = Regex("^([^/]+)/(.+)$")

        val matchResult = remoteBranchRegex.find("origin/feature/new-ui/components")
        assertNotNull(matchResult)
        assertEquals("origin", matchResult?.groupValues?.get(1))
        assertEquals("feature/new-ui/components", matchResult?.groupValues?.get(2))
    }

    @Test
    fun `test build full ref from branch name`() {
        fun toFullRef(branchName: String, isRemote: Boolean = false): String {
            return if (isRemote) "refs/remotes/$branchName" else "refs/heads/$branchName"
        }

        assertEquals("refs/heads/main", toFullRef("main"))
        assertEquals("refs/heads/feature/test", toFullRef("feature/test"))
        assertEquals("refs/remotes/origin/main", toFullRef("origin/main", true))
        assertEquals("refs/remotes/upstream/develop", toFullRef("upstream/develop", true))
    }

    // ==================== 仓库路径验证测试 ====================

    @Test
    fun `test validate repo path contains git directory`() {
        val sep = java.io.File.separator
        fun isValidRepoPath(path: String): Boolean {
            return path.endsWith(".git") || path.contains(sep + ".git") ||
                   path.contains("/.git") || path.contains("\\.git")
        }

        assertTrue(isValidRepoPath("/path/to/repo/.git"))
        assertTrue(isValidRepoPath("C:\\Users\\repo\\.git"))
        assertFalse(isValidRepoPath("/path/to/regular/folder"))
        assertFalse(isValidRepoPath("not_a_repo"))
    }

    @Test
    fun `test check if path is absolute`() {
        fun isAbsolutePath(path: String): Boolean {
            return path.startsWith('/') || 
                   path.matches(Regex("^[A-Za-z]:.*")) ||
                   path.matches(Regex("^\\\\\\\\.*"))
        }

        assertTrue(isAbsolutePath("/usr/local/repo"))
        assertTrue(isAbsolutePath("D:\\Projects\\repo"))
        assertTrue(isAbsolutePath("\\\\network\\share\\repo"))
        assertFalse(isAbsolutePath("relative/path"))
        assertFalse(isAbsolutePath("./repo"))
    }

    // ==================== CloneOptions 路径构建测试 ====================

    @Test
    fun `test build clone local path from URL and parent dir`() {
        val sep = java.io.File.separator
        fun buildClonePath(parentDir: String, url: String): String {
            val repoName = url
                .removeSuffix(".git")
                .substringAfterLast('/')
                .substringAfterLast(':')
            return "$parentDir${sep}$repoName".replace('/', sep[0])
        }

        val result1 = buildClonePath("/home/user/projects", "https://github.com/user/my-app.git")
        assertTrue(result1.endsWith("my-app"))

        val result2 = buildClonePath("D:\\Repos", "git@github.com:team/project.git")
        assertTrue(result2.endsWith("project"))
    }

    @Test
    fun `test clone to existing directory detection`() {
        fun shouldPreventClone(directoryExists: Boolean, isEmpty: Boolean): Boolean {
            return directoryExists && !isEmpty
        }

        assertFalse("Non-existing dir should allow clone", shouldPreventClone(false, false))
        assertFalse("Existing empty dir should allow clone", shouldPreventClone(true, true))
        assertTrue("Existing non-empty dir should prevent clone", shouldPreventClone(true, false))
    }

    // ==================== 远程 URL 格式化测试 ====================

    @Test
    fun `test ensure URL has git suffix`() {
        fun ensureGitSuffix(url: String): String {
            return if (url.endsWith(".git")) url else "$url.git"
        }

        assertEquals("https://github.com/user/repo.git", ensureGitSuffix("https://github.com/user/repo.git"))
        assertEquals("https://github.com/user/repo.git", ensureGitSuffix("https://github.com/user/repo"))
        assertEquals("git@github.com:user/repo.git", ensureGitSuffix("git@github.com:user/repo"))
    }

    @Test
    fun `test normalize HTTPS URL protocol`() {
        fun normalizeHttpsUrl(url: String): String {
            return if (url.startsWith("http://") && !url.startsWith("https://")) {
                url.replaceFirst("http://", "https://")
            } else {
                url
            }
        }

        assertEquals("https://github.com/user/repo", normalizeHttpsUrl("http://github.com/user/repo"))
        assertEquals("https://github.com/user/repo", normalizeHttpsUrl("https://github.com/user/repo"))
    }

    // ==================== 文件路径处理测试 ====================

    @Test
    fun `test extract file name from path`() {
        fun extractFileName(filePath: String): String {
            return filePath.substringAfterLast('/').substringAfterLast('\\')
        }

        assertEquals("Main.kt", extractFileName("src/main/kotlin/Main.kt"))
        assertEquals("README.md", extractFileName("README.md"))
        assertEquals("settings.gradle.kts", extractFileName("/root/settings.gradle.kts"))
        assertEquals("config.json", extractFileName("..\\data\\config.json"))
    }

    @Test
    fun `test extract directory from file path`() {
        fun extractDirectory(filePath: String): String {
            val lastSep = maxOf(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'))
            return if (lastSep > 0) filePath.substring(0, lastSep) else "."
        }

        assertEquals("src/main/kotlin", extractDirectory("src/main/kotlin/Main.kt"))
        assertEquals(".", extractDirectory("file.txt"))
        assertEquals("/root", extractDirectory("/root/settings.gradle.kts"))
    }

    // ==================== 深度参数验证测试 ====================

    @Test
    fun `test clone depth validation`() {
        fun isValidDepth(depth: Int?): Boolean {
            return depth == null || depth > 0
        }

        assertTrue(isValidDepth(null))
        assertTrue(isValidDepth(1))
        assertTrue(isValidDepth(10))
        assertTrue(isValidDepth(100))
        assertFalse(isValidDepth(0))
        assertFalse(isValidDepth(-1))
    }

    // ==================== 分支名验证测试 ====================

    @Test
    fun `test valid branch names`() {
        fun isValidBranchName(name: String): Boolean {
            if (name.isBlank()) return false
            val invalidChars = Regex("[^a-zA-Z0-9_\\-/]")
            return !invalidChars.containsMatchIn(name) && 
                   !name.startsWith('-') && 
                   !name.endsWith('.') &&
                   name != "HEAD"
        }

        val validNames = listOf("main", "master", "develop", "feature/login",
            "fix/bug-123", "hotfix/issue-42", "test_branch",
            "MY-FEATURE", "Feature-NewUI-V2")

        validNames.forEach { name ->
            assertTrue("Branch '$name' should be valid", isValidBranchName(name))
        }
    }

    @Test
    fun `test invalid branch names`() {
        fun isValidBranchName(name: String): Boolean {
            if (name.isBlank()) return false
            val invalidChars = Regex("[^a-zA-Z0-9_\\-/]")
            return !invalidChars.containsMatchIn(name) && 
                   !name.startsWith('-') && 
                   !name.endsWith('.') &&
                   name != "HEAD"
        }

        val invalidNames = listOf("", " ", "~feature", ":bad", "@invalid",
            "^bad", "bad~", "bad.", "HEAD", "lock{msg}", "bad\\name")

        invalidNames.forEach { name ->
            assertFalse("Branch '$name' should be invalid", isValidBranchName(name))
        }
    }

    companion object {
        private val separator = java.io.File.separator
    }
}
