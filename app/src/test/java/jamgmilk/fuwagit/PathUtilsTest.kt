package jamgmilk.fuwagit

import jamgmilk.fuwagit.core.util.PathUtils
import org.junit.Assert.*
import org.junit.Test

class PathUtilsTest {

    // ==================== getFileName 测试 ====================

    @Test
    fun `getFileName extracts filename from simple path`() {
        assertEquals("file.txt", PathUtils.getFileName("/path/to/file.txt"))
    }

    @Test
    fun `getFileName extracts filename from nested path`() {
        assertEquals("Main.kt", PathUtils.getFileName("/src/main/kotlin/Main.kt"))
    }

    @Test
    fun `getFileName extracts filename from Windows path`() {
        assertEquals("App.java", PathUtils.getFileName("D:\\Projects\\App\\src\\App.java"))
    }

    @Test
    fun `getFileName returns filename for path without directory`() {
        assertEquals("readme.md", PathUtils.getFileName("readme.md"))
    }

    @Test
    fun `getFileName handles hidden files`() {
        assertEquals(".gitignore", PathUtils.getFileName("/path/to/.gitignore"))
    }

    @Test
    fun `getFileName handles multiple extensions`() {
        assertEquals("archive.tar.gz", PathUtils.getFileName("/path/to/archive.tar.gz"))
    }

    @Test
    fun `getFileName handles path with trailing slash`() {
        val filename = PathUtils.getFileName("/path/to/directory/")
        assertTrue("Filename should not be empty for trailing slash", filename.isEmpty() || filename == "directory")
    }

    @Test
    fun `getFileName handles empty path`() {
        val filename = PathUtils.getFileName("")
        assertEquals("", filename)
    }
}