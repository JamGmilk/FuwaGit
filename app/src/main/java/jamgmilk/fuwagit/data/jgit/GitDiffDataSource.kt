package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.FileDiff

/**
 * Data source interface for Git diff operations.
 */
interface GitDiffDataSource {
    /**
     * 获取工作区中单个文件的差异（未暂存的更改）
     *
     * @param repoPath 仓库路径
     * @param filePath 文件路径
     * @return 文件差异
     */
    fun getWorkingTreeDiff(repoPath: String, filePath: String): Result<FileDiff>

    /**
     * 获取已暂存文件的差异（staged vs HEAD）
     *
     * @param repoPath 仓库路径
     * @param filePath 文件路径
     * @return 文件差异
     */
    fun getStagedDiff(repoPath: String, filePath: String): Result<FileDiff>

    /**
     * 获取两个提交之间单个文件的差异
     *
     * @param repoPath 仓库路径
     * @param filePath 文件路径
     * @param oldCommit 旧提交哈希
     * @param newCommit 新提交哈希
     * @return 文件差异
     */
    fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): Result<FileDiff>

    /**
     * 获取两个提交之间所有文件的差异摘要
     *
     * @param repoPath 仓库路径
     * @param oldCommit 旧提交哈希
     * @param newCommit 新提交哈希
     * @return 所有文件的差异列表
     */
    fun getCommitDiff(repoPath: String, oldCommit: String, newCommit: String): Result<List<FileDiff>>

    /**
     * 获取单个文件的原始内容
     *
     * @param repoPath 仓库路径
     * @param filePath 文件路径
     * @param commitHash 提交哈希（null 表示工作区）
     * @return 文件内容
     */
    fun getFileContent(repoPath: String, filePath: String, commitHash: String? = null): Result<String>
}
