package jamgmilk.fuwagit

import jamgmilk.fuwagit.data.jgit.JGitDataSource
import org.eclipse.jgit.api.Git
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JGitDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataSource: JGitDataSource
    private lateinit var repoDir: File

    @Before
    fun setup() {
        dataSource = JGitDataSource()
        repoDir = tempFolder.newFolder("test-repo")
    }

    @Test
    fun `test initRepo`() {
        val result = dataSource.initRepo(repoDir.absolutePath)
        assertTrue(result.isSuccess)
        assertTrue(File(repoDir, ".git").exists())
    }

    @Test
    fun `test stageFile with deletion`() {
        // Init
        dataSource.initRepo(repoDir.absolutePath)
        val git = Git.open(repoDir)
        
        // Create and commit a file
        val file = File(repoDir, "test.txt")
        file.writeText("hello")
        git.add().addFilepattern("test.txt").call()
        git.commit().setMessage("Initial commit").call()
        
        // Delete the file
        file.delete()
        
        // Stage the deletion
        val stageResult = dataSource.stageFile(repoDir.absolutePath, "test.txt")
        assertTrue(stageResult.isSuccess)
        
        // Verify staged in index
        val status = git.status().call()
        assertTrue("File should be in 'removed' set", status.removed.contains("test.txt"))
        git.close()
    }

    @Test
    fun `test stageAll with deletions and new files`() {
        dataSource.initRepo(repoDir.absolutePath)
        val git = Git.open(repoDir)
        
        // 1. Existing file to be deleted
        val file1 = File(repoDir, "file1.txt")
        file1.writeText("content1")
        git.add().addFilepattern("file1.txt").call()
        git.commit().setMessage("Initial").call()
        
        // 2. Modify existing file
        val file2 = File(repoDir, "file2.txt")
        file2.writeText("content2")
        git.add().addFilepattern("file2.txt").call()
        git.commit().setMessage("Initial 2").call()
        
        // Operations
        file1.delete() // Removed
        file2.writeText("modified2") // Modified
        val file3 = File(repoDir, "file3.txt")
        file3.writeText("new3") // Untracked
        
        // Stage All
        dataSource.stageAll(repoDir.absolutePath)
        
        val status = git.status().call()
        assertTrue(status.removed.contains("file1.txt"))
        assertTrue(status.changed.contains("file2.txt"))
        assertTrue(status.added.contains("file3.txt"))
        
        git.close()
    }

    @Test
    fun `test configureRemote add and update`() {
        dataSource.initRepo(repoDir.absolutePath)
        
        // 1. Add new remote
        val addResult = dataSource.configureRemote(repoDir.absolutePath, "origin", "https://github.com/user/repo.git")
        assertTrue(addResult.isSuccess)
        assertEquals("Remote origin added: https://github.com/user/repo.git", addResult.getOrNull())
        
        // Verify URL
        assertEquals("https://github.com/user/repo.git", dataSource.getRemoteUrl(repoDir.absolutePath, "origin"))
        
        // 2. Update existing remote
        val updateResult = dataSource.configureRemote(repoDir.absolutePath, "origin", "https://github.com/user/new-repo.git")
        assertTrue(updateResult.isSuccess)
        assertEquals("Remote origin updated: https://github.com/user/new-repo.git", updateResult.getOrNull())
        
        // Verify Updated URL
        assertEquals("https://github.com/user/new-repo.git", dataSource.getRemoteUrl(repoDir.absolutePath, "origin"))
    }
}
