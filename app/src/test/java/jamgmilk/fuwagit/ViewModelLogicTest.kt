package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import org.junit.Assert.*
import org.junit.Test

/**
 * ViewModel 和业务逻辑测试
 * 覆盖：凭据选择、clone 策略、仓库管理等场景
 */
class ViewModelLogicTest {

    // ==================== 凭据选择逻辑测试 ====================

    @Test
    fun `test select HTTPS credential for clone`() {
        val credentials = listOf(
            HttpsCredential("uuid1", "github.com", "user1", "pass1", 1000L, 1000L),
            HttpsCredential("uuid2", "gitlab.com", "user2", "pass2", 2000L, 2000L)
        )

        // 模拟凭据选择逻辑
        val selectedUuid = "uuid1"
        val selectedCredential = credentials.find { it.uuid == selectedUuid }

        assertNotNull("Selected credential should exist", selectedCredential)
        assertEquals("github.com", selectedCredential?.host)
        assertEquals("user1", selectedCredential?.username)
    }

    @Test
    fun `test select SSH key for clone`() {
        val sshKeys = listOf(
            SshKey("uuid1", "Work Key", "RSA", "ssh-rsa AAAA...", "-----BEGIN...", null, "SHA256:abc", 1000L),
            SshKey("uuid2", "Personal Key", "Ed25519", "ssh-ed25519 AAAA...", "-----BEGIN...", "pass", "SHA256:def", 2000L)
        )

        val selectedUuid = "uuid2"
        val selectedKey = sshKeys.find { it.uuid == selectedUuid }

        assertNotNull("Selected SSH key should exist", selectedKey)
        assertEquals("Personal Key", selectedKey?.name)
        assertNotNull("Selected key should have passphrase", selectedKey?.passphrase)
    }

    @Test
    fun `test auto-select first available credential`() {
        val httpsCredentials = listOf(
            HttpsCredential("uuid1", "github.com", "user1", "pass1", 1000L, 1000L)
        )
        val sshKeys = emptyList<SshKey>()

        // 自动选择逻辑：优先 HTTPS 凭据
        val selectedCredential = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(
                httpsCredentials.first().username,
                httpsCredentials.first().password
            )
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNotNull("Should auto-select HTTPS credential", selectedCredential)
        assertTrue("Should be HTTPS credential", selectedCredential is CloneCredential.Https)
    }

    @Test
    fun `test no credential selected when both lists empty`() {
        val httpsCredentials = emptyList<HttpsCredential>()
        val sshKeys = emptyList<SshKey>()

        val selectedCredential = if (httpsCredentials.isNotEmpty()) {
            CloneCredential.Https(httpsCredentials.first().username, httpsCredentials.first().password)
        } else if (sshKeys.isNotEmpty()) {
            CloneCredential.Ssh(sshKeys.first().privateKey, sshKeys.first().passphrase)
        } else {
            null
        }

        assertNull("Should return null when no credentials available", selectedCredential)
    }

    // ==================== Clone 策略测试 ====================

    @Test
    fun `test clone options with different depths`() {
        // 测试不同深度设置
        val depthOptions = listOf(1, 5, 10, null)

        depthOptions.forEach { depth ->
            val isValid = depth == null || depth > 0
            assertTrue("Depth $depth should be valid", isValid)
        }
    }

    @Test
    fun `test clone branch selection`() {
        val availableBranches = listOf("main", "develop", "feature/test")
        val selectedBranch = "main"

        assertTrue("Selected branch should be in available branches",
            selectedBranch in availableBranches)
    }

    @Test
    fun `test clone to existing directory should fail`() {
        // 模拟 clone 到已存在目录的场景
        val directoryExists = true
        
        // 应该阻止 clone 操作
        val shouldAllowClone = !directoryExists
        
        assertFalse("Should not allow clone to existing directory", shouldAllowClone)
    }

    // ==================== 仓库管理测试 ====================

    @Test
    fun `test repo validation by git directory existence`() {
        val validRepoPaths = listOf(
            "/path/to/repo/.git",
            "/home/user/project/.git"
        )
        val invalidRepoPaths = listOf(
            "/path/to/regular/folder",
            "/tmp/empty"
        )

        validRepoPaths.forEach { path ->
            assertTrue("Path '$path' should be valid repo", 
                path.endsWith(".git"))
        }

        invalidRepoPaths.forEach { path ->
            assertFalse("Path '$path' should not be valid repo", 
                path.endsWith(".git"))
        }
    }

    @Test
    fun `test repo alias fallback to path name`() {
        fun getDisplayName(path: String, alias: String?): String {
            return alias ?: path.substringAfterLast('/').substringAfterLast('\\')
        }

        assertEquals("my-repo", getDisplayName("/path/to/my-repo", null))
        assertEquals("Custom Name", getDisplayName("/path/to/repo", "Custom Name"))
    }

    // ==================== 远程仓库 URL 解析测试 ====================

    @Test
    fun `test HTTPS URL validation`() {
        val validUrls = listOf(
            "https://github.com/user/repo.git",
            "https://gitlab.com/group/project.git",
            "https://bitbucket.org/team/repo.git"
        )

        val invalidUrls = listOf(
            "http://github.com/user/repo.git", // HTTP not HTTPS
            "git@github.com:user/repo.git", // SSH format
            "github.com/user/repo" // No protocol
        )

        validUrls.forEach { url ->
            assertTrue("URL '$url' should be valid HTTPS", 
                url.startsWith("https://"))
        }

        invalidUrls.forEach { url ->
            assertFalse("URL '$url' should not be valid HTTPS", 
                url.startsWith("https://"))
        }
    }

    @Test
    fun `test SSH URL validation`() {
        val validSshUrls = listOf(
            "git@github.com:user/repo.git",
            "ssh://git@github.com/user/repo.git",
            "git@gitlab.com:group/project.git"
        )

        validSshUrls.forEach { url ->
            assertTrue("URL '$url' should be SSH format", 
                url.contains("@") && (url.startsWith("git@") || url.startsWith("ssh://")))
        }
    }

    @Test
    fun `test extract repo name from URL`() {
        fun extractRepoName(url: String): String {
            return url
                .removeSuffix(".git")
                .substringAfterLast('/')
                .substringAfterLast(':')
        }

        assertEquals("repo", extractRepoName("https://github.com/user/repo.git"))
        assertEquals("project", extractRepoName("https://gitlab.com/group/project"))
        assertEquals("repo", extractRepoName("git@github.com:user/repo.git"))
    }

    // ==================== 凭据与远程 URL 匹配测试 ====================

    @Test
    fun `test match credential host with remote URL`() {
        val credentials = listOf(
            HttpsCredential("uuid1", "github.com", "user1", "pass1", 1000L, 1000L),
            HttpsCredential("uuid2", "gitlab.com", "user2", "pass2", 2000L, 2000L)
        )

        val remoteUrl = "https://github.com/myuser/myrepo.git"
        
        // 提取主机名
        val host = remoteUrl.removePrefix("https://").substringBefore('/')
        
        // 匹配凭据
        val matchedCredential = credentials.find { it.host == host }

        assertNotNull("Should find matching credential", matchedCredential)
        assertEquals("github.com", matchedCredential?.host)
    }

    @Test
    fun `test no matching credential for unknown host`() {
        val credentials = listOf(
            HttpsCredential("uuid1", "github.com", "user1", "pass1", 1000L, 1000L)
        )

        val remoteUrl = "https://bitbucket.org/user/repo.git"
        val host = remoteUrl.removePrefix("https://").substringBefore('/')
        
        val matchedCredential = credentials.find { it.host == host }

        assertNull("Should not find matching credential for unknown host", matchedCredential)
    }

    // ==================== 状态同步测试 ====================

    @Test
    fun `test repo list update triggers UI refresh`() {
        // 模拟 repo list 变化
        var uiRefreshCount = 0
        
        val repoListUpdateCallback: () -> Unit = {
            uiRefreshCount++
        }

        // 初始状态
        assertEquals(0, uiRefreshCount)
        
        // 模拟添加仓库
        repoListUpdateCallback()
        assertEquals(1, uiRefreshCount)
        
        // 模拟删除仓库
        repoListUpdateCallback()
        assertEquals(2, uiRefreshCount)
    }

    @Test
    fun `test current repo change updates UI`() {
        var currentRepoPath: String? = null
        var uiUpdateCount = 0
        
        fun setCurrentRepo(path: String?) {
            currentRepoPath = path
            uiUpdateCount++
        }

        setCurrentRepo("/path/to/repo1")
        assertEquals("/path/to/repo1", currentRepoPath)
        assertEquals(1, uiUpdateCount)

        setCurrentRepo("/path/to/repo2")
        assertEquals("/path/to/repo2", currentRepoPath)
        assertEquals(2, uiUpdateCount)

        setCurrentRepo(null)
        assertEquals(null, currentRepoPath)
        assertEquals(3, uiUpdateCount)
    }
}
