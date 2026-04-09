package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.FileDiff

/**
 * Data source interface for Git diff operations.
 */
interface GitDiffDataSource {
    /**
     * Get diff for a single file in working tree (unstaged changes)
     *
     * @param repoPath Repository path
     * @param filePath File path
     * @return File diff
     */
    fun getWorkingTreeDiff(repoPath: String, filePath: String): Result<FileDiff>

    /**
     * Get diff for staged file (staged vs HEAD)
     *
     * @param repoPath Repository path
     * @param filePath File path
     * @return File diff
     */
    fun getStagedDiff(repoPath: String, filePath: String): Result<FileDiff>

    /**
     * Get diff for a single file between two commits
     *
     * @param repoPath Repository path
     * @param filePath File path
     * @param oldCommit Old commit hash
     * @param newCommit New commit hash
     * @return File diff
     */
    fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): Result<FileDiff>

    /**
     * Get diff summary for all files between two commits
     *
     * @param repoPath Repository path
     * @param oldCommit Old commit hash
     * @param newCommit New commit hash
     * @return List of all file diffs
     */
    fun getCommitDiff(repoPath: String, oldCommit: String, newCommit: String): Result<List<FileDiff>>

    /**
     * Get raw content of a single file
     *
     * @param repoPath Repository path
     * @param filePath File path
     * @param commitHash Commit hash (null means working tree)
     * @return File content
     */
    fun getFileContent(repoPath: String, filePath: String, commitHash: String? = null): Result<String>
}
