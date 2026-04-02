package jamgmilk.fuwagit.domain.model.git

/**
 * 冲突文件状态
 */
enum class ConflictStatus {
    /** 未解决 */
    UNRESOLVED,
    /** 已标记为解决 */
    RESOLVED,
    /** 已添加到暂存区 */
    STAGED
}

/**
 * 冲突文件信息
 *
 * @param path 文件路径
 * @param name 文件名
 * @param status 冲突状态
 * @param description 冲突描述（如：both modified）
 */
data class GitConflict(
    val path: String,
    val name: String,
    val status: ConflictStatus = ConflictStatus.UNRESOLVED,
    val description: String = ""
)

/**
 * Merge/Rebase 冲突结果
 *
 * @param isConflicting 是否正在冲突中
 * @param operationType 操作类型 (MERGE/REBASE)
 * @param conflicts 冲突文件列表
 * @param message 冲突消息
 */
data class ConflictResult(
    val isConflicting: Boolean = false,
    val operationType: String = "",
    val conflicts: List<GitConflict> = emptyList(),
    val message: String = ""
) {
    val hasUnresolvedConflicts: Boolean get() = conflicts.any { it.status == ConflictStatus.UNRESOLVED }
    val allResolved: Boolean get() = conflicts.isNotEmpty() && conflicts.all { it.status != ConflictStatus.UNRESOLVED }
    val allStaged: Boolean get() = conflicts.isNotEmpty() && conflicts.all { it.status == ConflictStatus.STAGED }
    val unresolvedCount: Int get() = conflicts.count { it.status == ConflictStatus.UNRESOLVED }
    val resolvedCount: Int get() = conflicts.count { it.status == ConflictStatus.RESOLVED || it.status == ConflictStatus.STAGED }
}
