package jamgmilk.fuwagit

import jamgmilk.fuwagit.core.util.UrlUtils
import jamgmilk.fuwagit.core.util.UrlUtils.UrlType
import jamgmilk.fuwagit.core.util.UrlUtils.ValidationResult
import org.junit.Assert.*
import org.junit.Test

class UrlUtilsTest {

    // ==================== validateGitUrl 有效 URL 测试 ====================

    @Test
    fun `validateGitUrl accepts valid HTTPS URL with git suffix`() {
        val result = UrlUtils.validateGitUrl("https://github.com/user/repo.git")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.HTTPS, (result as ValidationResult.Valid).type)
    }

    @Test
    fun `validateGitUrl accepts valid HTTPS URL without git suffix`() {
        val result = UrlUtils.validateGitUrl("https://gitlab.com/group/project")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.HTTPS, (result as ValidationResult.Valid).type)
    }

    @Test
    fun `validateGitUrl accepts valid SSH URL git@ format`() {
        val result = UrlUtils.validateGitUrl("git@github.com:user/repo.git")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.SSH, (result as ValidationResult.Valid).type)
    }

    @Test
    fun `validateGitUrl accepts valid SSH URL ssh protocol format`() {
        val result = UrlUtils.validateGitUrl("ssh://git@github.com/user/repo.git")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.HTTPS, (result as ValidationResult.Valid).type)
    }

    @Test
    fun `validateGitUrl accepts HTTP URL`() {
        val result = UrlUtils.validateGitUrl("http://github.com/user/repo.git")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.HTTPS, (result as ValidationResult.Valid).type)
    }

    @Test
    fun `validateGitUrl accepts file URL`() {
        val result = UrlUtils.validateGitUrl("file:///home/user/repo.git")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.FILE, (result as ValidationResult.Valid).type)
    }

    // ==================== validateGitUrl 无效 URL 测试 ====================

    @Test
    fun `validateGitUrl rejects blank URL`() {
        val result = UrlUtils.validateGitUrl("")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("URL cannot be empty", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun `validateGitUrl rejects whitespace-only URL`() {
        val result = UrlUtils.validateGitUrl("   ")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("URL cannot be empty", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun `validateGitUrl rejects URL with spaces`() {
        val result = UrlUtils.validateGitUrl("https://github.com/user/repo .git")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("URL cannot contain spaces", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun `validateGitUrl rejects SSH URL without @`() {
        val result = UrlUtils.validateGitUrl("git@github.comuser/repo.git")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateGitUrl rejects SSH URL without colon`() {
        val result = UrlUtils.validateGitUrl("git@github.com/user/repo.git")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateGitUrl rejects SSH URL with empty host`() {
        val result = UrlUtils.validateGitUrl("git@:user/repo.git")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("SSH URL host is empty", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun `validateGitUrl rejects SSH URL with empty path`() {
        val result = UrlUtils.validateGitUrl("git@github.com:")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("SSH URL path is empty", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun `validateGitUrl rejects SSH URL with invalid hostname`() {
        val result = UrlUtils.validateGitUrl("git@localhost:user/repo.git")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("SSH URL host should be a valid hostname (e.g., github.com)", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun `validateGitUrl accepts URL ending with git without protocol`() {
        val result = UrlUtils.validateGitUrl("github.com/user/repo.git")
        assertTrue(result is ValidationResult.Valid)
        assertEquals(UrlType.HTTPS, (result as ValidationResult.Valid).type)
    }

    @Test
    fun `validateGitUrl rejects URL with just special characters`() {
        val result = UrlUtils.validateGitUrl("!!!@@@###")
        assertTrue(result is ValidationResult.Invalid)
    }

    // ==================== getUrlType 测试 ====================

    @Test
    fun `getUrlType returns SSH for git@ format`() {
        assertEquals(UrlType.SSH, UrlUtils.getUrlType("git@github.com:user/repo.git"))
    }

    @Test
    fun `getUrlType returns HTTPS for https protocol`() {
        assertEquals(UrlType.HTTPS, UrlUtils.getUrlType("https://github.com/user/repo.git"))
    }

    @Test
    fun `getUrlType returns HTTPS for http protocol`() {
        assertEquals(UrlType.HTTPS, UrlUtils.getUrlType("http://gitlab.com/project"))
    }

    @Test
    fun `getUrlType returns FILE for file protocol`() {
        assertEquals(UrlType.FILE, UrlUtils.getUrlType("file:///path/to/repo"))
    }

    @Test
    fun `getUrlType returns HTTPS for URL ending with git`() {
        assertEquals(UrlType.HTTPS, UrlUtils.getUrlType("https://example.com/repo.git"))
    }

    @Test
    fun `getUrlType returns UNKNOWN for unrecognized format`() {
        assertEquals(UrlType.UNKNOWN, UrlUtils.getUrlType("ftp://server.com/repo"))
        assertEquals(UrlType.UNKNOWN, UrlUtils.getUrlType("/local/path"))
    }

    // ==================== normalizeUrl 测试 ====================

    @Test
    fun `normalizeUrl adds git suffix when missing`() {
        assertEquals("https://github.com/user/repo.git", UrlUtils.normalizeUrl("https://github.com/user/repo"))
    }

    @Test
    fun `normalizeUrl keeps git suffix when present`() {
        assertEquals("https://github.com/user/repo.git", UrlUtils.normalizeUrl("https://github.com/user/repo.git"))
    }

    @Test
    fun `normalizeUrl keeps URL with query params unchanged`() {
        val url = "https://github.com/user/repo?ref=main"
        assertEquals(url, UrlUtils.normalizeUrl(url))
    }

    @Test
    fun `normalizeUrl trims whitespace`() {
        assertEquals("https://github.com/user/repo.git", UrlUtils.normalizeUrl("  https://github.com/user/repo  "))
    }

    // ==================== extractHost 测试 ====================

    @Test
    fun `extractHost returns host from HTTPS URL`() {
        assertEquals("github.com", UrlUtils.extractHost("https://github.com/user/repo.git"))
    }

    @Test
    fun `extractHost returns host from HTTP URL`() {
        assertEquals("gitlab.com", UrlUtils.extractHost("http://gitlab.com/group/project"))
    }

    @Test
    fun `extractHost returns host from SSH URL`() {
        assertEquals("github.com", UrlUtils.extractHost("git@github.com:user/repo.git"))
    }

    @Test
    fun `extractHost returns host without port from URL with port`() {
        assertEquals("github.com", UrlUtils.extractHost("https://github.com:8080/user/repo.git"))
    }

    @Test
    fun `extractHost returns null for unrecognized format`() {
        assertNull(UrlUtils.extractHost("invalid-url"))
        assertNull(UrlUtils.extractHost("/local/path"))
    }

    // ==================== formatUrlForDisplay 测试 ====================

    @Test
    fun `formatUrlForDisplay returns SSH URL unchanged`() {
        val url = "git@github.com:user/repo.git"
        assertEquals(url, UrlUtils.formatUrlForDisplay(url))
    }

    @Test
    fun `formatUrlForDisplay removes protocol and git suffix for HTTPS`() {
        assertEquals("github.com/user/repo", UrlUtils.formatUrlForDisplay("https://github.com/user/repo.git"))
    }

    @Test
    fun `formatUrlForDisplay returns unknown format unchanged`() {
        val url = "github.com/user/repo.git"
        val formatted = UrlUtils.formatUrlForDisplay(url)
        assertEquals("github.com/user/repo.git", formatted)
    }

    @Test
    fun `formatUrlForDisplay returns path unchanged`() {
        val url = "/local/path/to/repo"
        assertEquals(url, UrlUtils.formatUrlForDisplay(url))
    }
}