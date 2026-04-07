package jamgmilk.fuwagit.domain.model.git

/**
 * 行内差异（character-level diff）
 * 用于高亮显示一行中哪些具体字符发生了变化
 *
 * @param content 字符内容
 * @param isAdded true 表示这些字符是新增的，false 表示这些字符是删除的
 * @param startIndex 在原行内容中的起始位置
 */
data class InlineDiffSegment(
    val content: String,
    val isAdded: Boolean,
    val startIndex: Int
)

/**
 * 行内差异信息
 *
 * @param segments 行内的差异段列表
 * @param hasInlineDiff 是否有行内差异
 */
data class InlineDiff(
    val segments: List<InlineDiffSegment>
) {
    val hasInlineDiff: Boolean get() = segments.size > 1
}

/**
 * Diff 行类型
 */
enum class DiffLineType {
    /** 新增的行 */
    Added,
    /** 删除的行 */
    Deleted,
    /** 未更改的上下文行 */
    Context,
    /** 文件头信息 */
    Header
}

/**
 * Diff 中的单行
 *
 * @param content 行内容（不包含 diff 符号）
 * @param lineType 行类型
 * @param oldLineNumber 旧文件中的行号（从 1 开始，null 表示新增）
 * @param newLineNumber 新文件中的行号（从 1 开始，null 表示删除）
 * @param inlineDiff 行内差异信息（用于高亮行内变化的字符）
 */
data class DiffLine(
    val content: String,
    val lineType: DiffLineType,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null,
    val inlineDiff: InlineDiff? = null
) {
    /**
     * 获取带符号的行显示（用于原始 diff 显示）
     */
    val displayContent: String get() = when (lineType) {
        DiffLineType.Added -> "+ $content"
        DiffLineType.Deleted -> "- $content"
        DiffLineType.Context -> "  $content"
        DiffLineType.Header -> content
    }
}

/**
 * Diff Hunk - 文件差异中的一个变更块
 *
 * @param header Hunk 头信息（如 @@ -1,5 +1,6 @@）
 * @param lines 该 hunk 中的所有行
 * @param oldStart 旧文件中的起始行号
 * @param oldCount 旧文件中的行数
 * @param newStart 新文件中的起始行号
 * @param newCount 新文件中的行数
 */
data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>,
    val oldStart: Int = 0,
    val oldCount: Int = 0,
    val newStart: Int = 0,
    val newCount: Int = 0
) {
    /**
     * 获取新增行数
     */
    val addedLines: Int get() = lines.count { it.lineType == DiffLineType.Added }

    /**
     * 获取删除行数
     */
    val deletedLines: Int get() = lines.count { it.lineType == DiffLineType.Deleted }
}

/**
 * 文件差异结果
 *
 * @param oldPath 旧文件路径（重命名时为原路径）
 * @param newPath 新文件路径（重命名时为新路径）
 * @param changeType 变更类型
 * @param hunks 差异块列表
 * @param additions 总新增行数
 * @param deletions 总删除行数
 * @param isBinary 是否为二进制文件
 * @param oldContent 旧文件内容（用于并排显示）
 * @param newContent 新文件内容（用于并排显示）
 */
data class FileDiff(
    val oldPath: String,
    val newPath: String,
    val changeType: GitChangeType,
    val hunks: List<DiffHunk> = emptyList(),
    val additions: Int = 0,
    val deletions: Int = 0,
    val isBinary: Boolean = false,
    val oldContent: String? = null,
    val newContent: String? = null
) {
    /**
     * 文件路径（优先使用新路径）
     */
    val path: String get() = if (changeType == GitChangeType.Renamed) "$oldPath → $newPath" else newPath

    /**
     * 文件名
     */
    val fileName: String get() = newPath.substringAfterLast("/")

    /**
     * 总变更行数
     */
    val totalChanges: Int get() = additions + deletions

    /**
     * 是否只有空白字符变更
     */
    val isWhitespaceOnly: Boolean
        get() {
            if (hunks.isEmpty()) return false
            return hunks.all { hunk ->
                hunk.lines.all { line ->
                    line.content.trim().isEmpty()
                }
            }
        }
}

/**
 * 工作区文件差异查询参数
 *
 * @param filePath 文件路径
 * @param isStaged 是否查看已暂存的更改（true: staged vs HEAD, false: working tree vs staged）
 */
data class WorkingDiffParams(
    val filePath: String,
    val isStaged: Boolean = false
)

/**
 * Commit 之间的文件差异查询参数
 *
 * @param filePath 文件路径
 * @param oldCommit 旧提交哈希（支持 HEAD~1, HEAD^ 等引用）
 * @param newCommit 新提交哈希（默认为 HEAD）
 */
data class CommitDiffParams(
    val filePath: String,
    val oldCommit: String,
    val newCommit: String = "HEAD"
)
