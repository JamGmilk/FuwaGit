package jamgmilk.fuwagit

import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import org.junit.Assert.*
import org.junit.Test

class GitPushOptionsTest {

    // ==================== 默认值测试 ====================

    @Test
    fun `test default options values`() {
        val options = GitPushOptions()

        assertTrue("Default should push current branch", options.pushCurrentBranch)
        assertFalse("Default should not push tags", options.pushTags)
        assertFalse("Default should not push all branches", options.pushAllBranches)
        assertEquals("Default remote is origin", "origin", options.remote)
        assertNull("Default branch is null (use current)", options.branch)
    }

    @Test
    fun `test companion default factory`() {
        val options = GitPushOptions.default()

        assertTrue(options.pushCurrentBranch)
        assertFalse(options.pushTags)
        assertFalse(options.pushAllBranches)
        assertEquals("origin", options.remote)
        assertNull(options.branch)
    }

    // ==================== withTags 工厂方法测试 ====================

    @Test
    fun `test withTags factory method`() {
        val options = GitPushOptions.withTags()

        assertTrue("withTags should push current branch", options.pushCurrentBranch)
        assertTrue("withTags should push tags", options.pushTags)
        assertFalse("withTags should not push all branches", options.pushAllBranches)
        assertEquals("origin", options.remote)
        assertNull(options.branch)
    }

    @Test
    fun `test withTags differs from default`() {
        val defaultOpts = GitPushOptions.default()
        val withTagsOpts = GitPushOptions.withTags()

        assertNotEquals("withTags should differ from default in pushTags",
            defaultOpts.pushTags, withTagsOpts.pushTags)
    }

    // ==================== all 工厂方法测试 ====================

    @Test
    fun `test all factory method pushes everything`() {
        val options = GitPushOptions.all()

        assertTrue("all() should push all branches", options.pushAllBranches)
        assertTrue("all() should push tags", options.pushTags)
        assertEquals("origin", options.remote)
        assertNull(options.branch)
    }

    @Test
    fun `test all factory vs withTags`() {
        val allOpts = GitPushOptions.all()
        val withTagsOpts = GitPushOptions.withTags()

        assertNotEquals("all() and withTags() should differ in pushAllBranches",
            allOpts.pushAllBranches, withTagsOpts.pushAllBranches)
        assertEquals("Both should push tags", allOpts.pushTags, withTagsOpts.pushTags)
    }

    @Test
    fun `test all factory vs default`() {
        val allOpts = GitPushOptions.all()
        val defaultOpts = GitPushOptions.default()

        assertNotEquals(allOpts.pushAllBranches, defaultOpts.pushAllBranches)
        assertNotEquals(allOpts.pushTags, defaultOpts.pushTags)
    }

    // ==================== 自定义配置测试 ====================

    @Test
    fun `test custom push to specific remote`() {
        val options = GitPushOptions(remote = "upstream")

        assertEquals("upstream", options.remote)
        assertTrue(options.pushCurrentBranch)
    }

    @Test
    fun `test custom push specific branch`() {
        val options = GitPushOptions(
            pushCurrentBranch = false,
            branch = "feature/new-ui"
        )

        assertFalse(options.pushCurrentBranch)
        assertEquals("feature/new-ui", options.branch)
    }

    @Test
    fun `test custom push with all options enabled`() {
        val options = GitPushOptions(
            pushCurrentBranch = true,
            pushTags = true,
            pushAllBranches = true,
            remote = "origin",
            branch = "main"
        )

        assertTrue(options.pushCurrentBranch)
        assertTrue(options.pushTags)
        assertTrue(options.pushAllBranches)
        assertEquals("main", options.branch)
    }

    @Test
    fun `test custom push minimal config`() {
        val options = GitPushOptions(
            pushCurrentBranch = true,
            pushTags = false,
            pushAllBranches = false,
            remote = "custom-remote"
        )

        assertTrue(options.pushCurrentBranch)
        assertFalse(options.pushTags)
        assertFalse(options.pushAllBranches)
        assertEquals("custom-remote", options.remote)
    }

    // ==================== 推送策略逻辑测试 ====================

    @Test
    fun `test determine push strategy for default options`() {
        val options = GitPushOptions.default()

        var strategy = ""
        when {
            options.pushAllBranches -> strategy = "ALL_BRANCHES"
            options.branch != null -> strategy = "SPECIFIC_BRANCH"
            options.pushCurrentBranch -> strategy = "CURRENT_BRANCH"
            else -> strategy = "NOTHING"
        }

        assertEquals("CURRENT_BRANCH", strategy)
    }

    @Test
    fun `test determine push strategy for all branches`() {
        val options = GitPushOptions.all()

        var strategy = ""
        when {
            options.pushAllBranches -> strategy = "ALL_BRANCHES"
            options.branch != null -> strategy = "SPECIFIC_BRANCH"
            options.pushCurrentBranch -> strategy = "CURRENT_BRANCH"
            else -> strategy = "NOTHING"
        }

        assertEquals("ALL_BRANCHES", strategy)
    }

    @Test
    fun `test determine push strategy for specific branch`() {
        val options = GitPushOptions(branch = "develop")

        var strategy = ""
        when {
            options.pushAllBranches -> strategy = "ALL_BRANCHES"
            options.branch != null -> strategy = "SPECIFIC_BRANCH"
            options.pushCurrentBranch -> strategy = "CURRENT_BRANCH"
            else -> strategy = "NOTHING"
        }

        assertEquals("SPECIFIC_BRANCH", strategy)
    }

    @Test
    fun `test push priority order - all branches highest then specific then current`() {
        val allPriority = listOf(
            GitPushOptions(pushAllBranches = true),
            GitPushOptions(branch = "specific"),
            GitPushOptions(pushCurrentBranch = true)
        )

        val priorities = allPriority.map { opts ->
            when {
                opts.pushAllBranches -> 3
                opts.branch != null -> 2
                opts.pushCurrentBranch -> 1
                else -> 0
            }
        }

        assertEquals(listOf(3, 2, 1), priorities)
    }

    // ==================== 数据类相等性测试 ====================

    @Test
    fun `test data class equality same values`() {
        val opts1 = GitPushOptions(
            pushCurrentBranch = true,
            pushTags = false,
            pushAllBranches = false,
            remote = "origin",
            branch = null
        )
        val opts2 = GitPushOptions(
            pushCurrentBranch = true,
            pushTags = false,
            pushAllBranches = false,
            remote = "origin",
            branch = null
        )

        assertEquals(opts1, opts2)
    }

    @Test
    fun `test data class inequality different values`() {
        val opts1 = GitPushOptions.default()
        val opts2 = GitPushOptions.withTags()
        val opts3 = GitPushOptions.all()

        assertNotEquals(opts1, opts2)
        assertNotEquals(opts2, opts3)
        assertNotEquals(opts1, opts3)
    }

    @Test
    fun `test data class copy modification`() {
        val original = GitPushOptions.default()
        val modified = original.copy(pushTags = true)

        assertFalse(original.pushTags)
        assertTrue(modified.pushTags)
        assertEquals(original.pushCurrentBranch, modified.pushCurrentBranch)
        assertEquals(original.remote, modified.remote)
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `test all flags disabled still has valid state`() {
        val options = GitPushOptions(
            pushCurrentBranch = false,
            pushTags = false,
            pushAllBranches = false,
            remote = "origin"
        )

        assertFalse(options.pushCurrentBranch)
        assertFalse(options.pushTags)
        assertFalse(options.pushAllBranches)
        assertNotNull(options.remote)
    }

    @Test
    fun `test empty remote name is allowed by data class`() {
        val options = GitPushOptions(remote = "")
        assertEquals("", options.remote)
    }

    @Test
    fun `test various remote names`() {
        val remotes = listOf("origin", "upstream", "my-fork", "production", "staging")

        remotes.forEach { remote ->
            val options = GitPushOptions(remote = remote)
            assertEquals(remote, options.remote)
        }
    }
}
