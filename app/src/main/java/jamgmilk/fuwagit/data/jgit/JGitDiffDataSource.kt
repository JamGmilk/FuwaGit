package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.DiffHunk
import jamgmilk.fuwagit.domain.model.git.DiffLine
import jamgmilk.fuwagit.domain.model.git.DiffLineType
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffEntry.ChangeType
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git diff operations using JGit.
 */
@Singleton
class JGitDiffDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitDiffDataSource {

    /**
     * 获取工作区中单个文件的差异（未暂存的更改）
     */
    override fun getWorkingTreeDiff(repoPath: String, filePath: String): Result<FileDiff> =
        core.withGit(repoPath) { git ->
            val repository = git.repository

            // 获取 HEAD 的树
            val headTree = getTreeForCommit(repository, "HEAD")

            // 使用 DiffFormatter 比较 HEAD 和工作区
            val diffEntries = getDiffEntriesForFile(repository, headTree, null, filePath)

            if (diffEntries.isEmpty()) {
                // 可能是新文件（未跟踪）
                getUntrackedFileDiff(repoPath, filePath)
            } else {
                val entry = diffEntries.first()
                parseDiffEntryWithContent(repository, entry, filePath, isStaged = false)
            }
        }

    /**
     * 获取已暂存文件的差异（staged vs HEAD）
     */
    override fun getStagedDiff(repoPath: String, filePath: String): Result<FileDiff> =
        core.withGit(repoPath) { git ->
            val repository = git.repository

            // 获取 HEAD 的树
            val headTree = getTreeForCommit(repository, "HEAD")

            // 获取 Index（暂存区）的树
            val indexTree = getTreeForIndex(repository)

            // 比较 HEAD 和 Index
            val diffEntries = getDiffEntriesForFile(repository, headTree, indexTree, filePath)

            if (diffEntries.isEmpty()) {
                throw IllegalArgumentException("No staged changes found for file: $filePath")
            }

            val entry = diffEntries.first()
            parseDiffEntryWithContent(repository, entry, filePath, isStaged = true)
        }

    /**
     * 获取两个提交之间单个文件的差异
     */
    override fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): Result<FileDiff> =
        core.withGit(repoPath) { git ->
            val repository = git.repository

            val oldTree = getTreeForCommit(repository, oldCommit)
            val newTree = getTreeForCommit(repository, newCommit)

            val diffEntries = getDiffEntriesForFile(repository, oldTree, newTree, filePath)

            if (diffEntries.isEmpty()) {
                throw IllegalArgumentException("No changes found for file: $filePath")
            }

            val entry = diffEntries.first()
            parseDiffEntry(repository, entry, oldCommit, newCommit)
        }

    /**
     * 获取两个提交之间所有文件的差异摘要
     */
    override fun getCommitDiff(
        repoPath: String,
        oldCommit: String,
        newCommit: String
    ): Result<List<FileDiff>> =
        core.withGit(repoPath) { git ->
            val repository = git.repository

            val oldTree = getTreeForCommit(repository, oldCommit)
            val newTree = getTreeForCommit(repository, newCommit)

            val diffEntries = getAllDiffEntries(repository, oldTree, newTree)

            diffEntries.map { entry ->
                parseDiffEntry(repository, entry, oldCommit, newCommit)
            }
        }

    /**
     * 获取单个文件的原始内容
     */
    override fun getFileContent(
        repoPath: String,
        filePath: String,
        commitHash: String?
    ): Result<String> =
        core.withGit(repoPath) { git ->
            val repository = git.repository

            if (commitHash == null) {
                // 读取工作区文件
                val repoDir = repository.workTree
                val file = File(repoDir, filePath)
                if (!file.exists()) {
                    throw IllegalArgumentException("File not found: $filePath")
                }
                file.readText()
            } else {
                // 读取提交中的文件
                val revWalk = RevWalk(repository)
                val commit = revWalk.parseCommit(repository.resolve(commitHash))
                val tree = commit.tree

                val treeWalk = TreeWalk(repository)
                treeWalk.addTree(tree)
                treeWalk.isRecursive = true

                var found = false
                var content: String? = null
                while (treeWalk.next()) {
                    if (treeWalk.pathString == filePath) {
                        val objectId = treeWalk.getObjectId(0)
                        val loader = repository.open(objectId)
                        content = String(loader.bytes)
                        found = true
                        break
                    }
                }

                treeWalk.close()
                revWalk.close()

                if (!found) {
                    throw IllegalArgumentException("File not found in commit: $filePath")
                }
                
                content!!
            }
        }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取提交对应的树
     */
    private fun getTreeForCommit(repository: Repository, commitHash: String): RevTree {
        val revWalk = RevWalk(repository)
        val commit = revWalk.parseCommit(repository.resolve(commitHash))
        val tree = commit.tree
        revWalk.dispose()
        return tree
    }

    /**
     * 获取 Index（暂存区）的树
     */
    private fun getTreeForIndex(repository: Repository): RevTree {
        val revWalk = RevWalk(repository)
        val headCommit = revWalk.parseCommit(repository.resolve("HEAD"))
        val tree = headCommit.tree
        revWalk.dispose()
        return tree
    }

    /**
     * 获取单个文件的 DiffEntry
     */
    private fun getDiffEntriesForFile(
        repository: Repository,
        oldTree: RevTree,
        newTree: RevTree?,
        filePath: String
    ): List<DiffEntry> {
        val outputStream = DisabledOutputStream.INSTANCE
        val diffFormatter = DiffFormatter(outputStream)
        diffFormatter.setRepository(repository)
        diffFormatter.setDiffComparator(RawTextComparator.DEFAULT)

        return try {
            val entries = if (newTree == null) {
                // 比较树和工作区
                diffFormatter.scan(oldTree, null)
            } else {
                // 比较两棵树
                diffFormatter.scan(oldTree, newTree)
            }

            // 过滤出指定文件
            entries.filter { entry ->
                entry.newPath == filePath || entry.oldPath == filePath
            }
        } finally {
            diffFormatter.close()
        }
    }

    /**
     * 获取所有 DiffEntry
     */
    private fun getAllDiffEntries(
        repository: Repository,
        oldTree: RevTree,
        newTree: RevTree
    ): List<DiffEntry> {
        val outputStream = DisabledOutputStream.INSTANCE
        val diffFormatter = DiffFormatter(outputStream)
        diffFormatter.setRepository(repository)
        diffFormatter.setDiffComparator(RawTextComparator.DEFAULT)

        return try {
            diffFormatter.scan(oldTree, newTree)
        } finally {
            diffFormatter.close()
        }
    }

    /**
     * 解析 DiffEntry 为 FileDiff（带内容）
     */
    private fun parseDiffEntryWithContent(
        repository: Repository,
        entry: DiffEntry,
        filePath: String,
        isStaged: Boolean
    ): FileDiff {
        val changeType = entry.changeType.toGitChangeType()
        val oldPath = entry.oldPath
        val newPath = entry.newPath

        // 读取文件内容
        val oldContent = if (changeType != GitChangeType.Added) {
            readFileContent(repository, entry.oldId.toObjectId(), oldPath)
        } else null

        val newContent = if (changeType != GitChangeType.Removed) {
            if (isStaged) {
                // 读取暂存区内容
                readIndexFileContent(filePath)
            } else {
                // 读取工作区内容
                readWorkingFileContent(repository.workTree, filePath)
            }
        } else null

        // 计算 diff hunks
        val hunks = if (oldContent != null && newContent != null) {
            computeHunks(oldContent, newContent)
        } else {
            emptyList()
        }

        val additions = hunks.sumOf { it.addedLines }
        val deletions = hunks.sumOf { it.deletedLines }

        return FileDiff(
            oldPath = oldPath,
            newPath = newPath,
            changeType = changeType,
            hunks = hunks,
            additions = additions,
            deletions = deletions,
            isBinary = isBinaryFile(oldContent, newContent),
            oldContent = oldContent,
            newContent = newContent
        )
    }

    /**
     * 解析 DiffEntry 为 FileDiff
     */
    private fun parseDiffEntry(
        repository: Repository,
        entry: DiffEntry,
        oldCommit: String,
        newCommit: String
    ): FileDiff {
        val changeType = entry.changeType.toGitChangeType()
        val oldPath = entry.oldPath
        val newPath = entry.newPath

        // 读取文件内容
        val oldContent = if (changeType != GitChangeType.Added) {
            readFileContent(repository, entry.oldId.toObjectId(), oldPath)
        } else null

        val newContent = if (changeType != GitChangeType.Removed) {
            readFileContent(repository, entry.newId.toObjectId(), newPath)
        } else null

        // 计算 diff hunks
        val hunks = if (oldContent != null && newContent != null) {
            computeHunks(oldContent, newContent)
        } else {
            emptyList()
        }

        val additions = hunks.sumOf { it.addedLines }
        val deletions = hunks.sumOf { it.deletedLines }

        return FileDiff(
            oldPath = oldPath,
            newPath = newPath,
            changeType = changeType,
            hunks = hunks,
            additions = additions,
            deletions = deletions,
            isBinary = isBinaryFile(oldContent, newContent),
            oldContent = oldContent,
            newContent = newContent
        )
    }

    /**
     * 获取未跟踪文件的差异
     */
    private fun getUntrackedFileDiff(repoPath: String, filePath: String): FileDiff {
        val content = readWorkingFileContent(File(repoPath), filePath)
            ?: throw IllegalArgumentException("File not found: $filePath")

        val lines = content.lines()
        val hunkLines = lines.map { line ->
            DiffLine(
                content = line,
                lineType = DiffLineType.Added,
                oldLineNumber = null,
                newLineNumber = lines.indexOf(line) + 1
            )
        }

        val hunk = DiffHunk(
            header = "@@ -0,0 +1,${lines.size} @@",
            lines = hunkLines,
            oldStart = 0,
            oldCount = 0,
            newStart = 1,
            newCount = lines.size
        )

        return FileDiff(
            oldPath = "/dev/null",
            newPath = filePath,
            changeType = GitChangeType.Added,
            hunks = listOf(hunk),
            additions = lines.size,
            deletions = 0,
            isBinary = isBinaryString(content),
            oldContent = null,
            newContent = content
        )
    }

    /**
     * 读取提交中的文件内容
     */
    private fun readFileContent(
        repository: Repository,
        objectId: org.eclipse.jgit.lib.ObjectId,
        path: String
    ): String? {
        return try {
            val loader = repository.open(objectId)
            String(loader.bytes)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取工作区文件
     */
    private fun readWorkingFileContent(repoDir: File, filePath: String): String? {
        return try {
            val file = File(repoDir, filePath)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取暂存区文件
     */
    private fun readIndexFileContent(filePath: String): String? {
        // 简化实现：实际上需要从 Git index 中读取
        // 这里暂时返回 null，实际应该使用 DirCache 读取
        return null
    }

    /**
     * 计算两个文本之间的 Hunks
     */
    private fun computeHunks(oldContent: String, newContent: String): List<DiffHunk> {
        val oldLines = oldContent.lines().toTypedArray()
        val newLines = newContent.lines().toTypedArray()

        // 使用 Myers diff 算法简化实现
        val hunks = mutableListOf<DiffHunk>()
        val diffLines = simpleDiff(oldLines, newLines)

        if (diffLines.isEmpty()) return emptyList()

        // 将变更行分组为 hunks（带上下文）
        val contextLines = 3
        var i = 0
        while (i < diffLines.size) {
            if (diffLines[i].lineType != DiffLineType.Context) {
                // 找到一个变更块
                val start = maxOf(0, i - contextLines)
                var end = minOf(diffLines.size, i + 1)

                // 向后查找连续的变更
                while (end < diffLines.size && diffLines[end].lineType != DiffLineType.Context) {
                    end++
                }
                end = minOf(diffLines.size, end + contextLines)

                // 创建 hunk
                val hunkLines = diffLines.slice(start until end)
                val oldStart = hunkLines.firstOrNull { it.oldLineNumber != null }?.oldLineNumber ?: 0
                val newStart = hunkLines.firstOrNull { it.newLineNumber != null }?.newLineNumber ?: 0

                val hunk = DiffHunk(
                    header = "@@ -$oldStart,${hunkLines.count { it.oldLineNumber != null || it.lineType != DiffLineType.Added }} +$newStart,${hunkLines.count { it.newLineNumber != null || it.lineType != DiffLineType.Deleted }} @@",
                    lines = hunkLines,
                    oldStart = oldStart,
                    oldCount = hunkLines.count { it.oldLineNumber != null || it.lineType != DiffLineType.Added },
                    newStart = newStart,
                    newCount = hunkLines.count { it.newLineNumber != null || it.lineType != DiffLineType.Deleted }
                )
                hunks.add(hunk)

                i = end
            } else {
                i++
            }
        }

        return hunks
    }

    /**
     * 简化的 Diff 算法（基于 LCS）
     */
    private fun simpleDiff(oldLines: Array<String>, newLines: Array<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        val m = oldLines.size
        val n = newLines.size

        // 构建 LCS 表
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                if (oldLines[i - 1] == newLines[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        // 回溯找出 diff
        var i = m
        var j = n
        val diffResult = mutableListOf<Triple<Int?, Int?, DiffLineType>>()

        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1] -> {
                    diffResult.add(Triple(i, j, DiffLineType.Context))
                    i--
                    j--
                }
                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                    diffResult.add(Triple(null, j, DiffLineType.Added))
                    j--
                }
                i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j]) -> {
                    diffResult.add(Triple(i, null, DiffLineType.Deleted))
                    i--
                }
                else -> {
                    break
                }
            }
        }

        // 反转并构建 DiffLine
        diffResult.reverse()
        var oldLineNum = 1
        var newLineNum = 1

        for ((oldL, newL, type) in diffResult) {
            val line = when (type) {
                DiffLineType.Context -> {
                    val line = DiffLine(
                        content = oldLines[oldLineNum - 1],
                        lineType = DiffLineType.Context,
                        oldLineNumber = oldLineNum,
                        newLineNumber = newLineNum
                    )
                    oldLineNum++
                    newLineNum++
                    line
                }
                DiffLineType.Added -> {
                    val line = DiffLine(
                        content = newLines[newLineNum - 1],
                        lineType = DiffLineType.Added,
                        oldLineNumber = null,
                        newLineNumber = newLineNum
                    )
                    newLineNum++
                    line
                }
                DiffLineType.Deleted -> {
                    val line = DiffLine(
                        content = oldLines[oldLineNum - 1],
                        lineType = DiffLineType.Deleted,
                        oldLineNumber = oldLineNum,
                        newLineNumber = null
                    )
                    oldLineNum++
                    line
                }
                else -> continue
            }
            result.add(line)
        }

        return result
    }

    /**
     * 检查是否为二进制文件
     */
    private fun isBinaryFile(oldContent: String?, newContent: String?): Boolean {
        return isBinaryString(oldContent) || isBinaryString(newContent)
    }

    /**
     * 检查字符串是否包含二进制字符
     */
    private fun isBinaryString(content: String?): Boolean {
        if (content == null) return false
        return content.take(8000).any { char ->
            char.code in 1..8 || char.code in 14..31
        }
    }

    /**
     * 转换 JGit ChangeType 到 GitChangeType
     */
    private fun ChangeType.toGitChangeType(): GitChangeType = when (this) {
        ChangeType.ADD -> GitChangeType.Added
        ChangeType.DELETE -> GitChangeType.Removed
        ChangeType.MODIFY -> GitChangeType.Modified
        ChangeType.RENAME -> GitChangeType.Renamed
        ChangeType.COPY -> GitChangeType.Added
        else -> GitChangeType.Modified
    }
}
