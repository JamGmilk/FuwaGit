package jamgmilk.fuwagit.domain.model.git

/**
 * 鍐茬獊鏂囦欢鐘舵€?
 */
enum class ConflictStatus {
    /** 鏈В鍐?*/
    UNRESOLVED,
    /** 宸叉爣璁颁负瑙ｅ喅 */
    RESOLVED,
    /** 宸叉坊鍔犲埌鏆傚瓨鍖?*/
    STAGED
}

/**
 * 鍐茬獊鏂囦欢淇℃伅
 *
 * @param path 鏂囦欢璺緞
 * @param name 鏂囦欢鍚?
 * @param status 鍐茬獊鐘舵€?
 * @param description 鍐茬獊鎻忚堪锛堝锛歜oth modified锛?
 */
data class GitConflict(
    val path: String,
    val name: String,
    val status: ConflictStatus = ConflictStatus.UNRESOLVED,
    val description: String = ""
)

/**
 * Merge/Rebase 鍐茬獊缁撴灉
 *
 * @param isConflicting 鏄惁姝ｅ湪鍐茬獊涓?
 * @param operationType 鎿嶄綔绫诲瀷 (MERGE/REBASE)
 * @param conflicts 鍐茬獊鏂囦欢鍒楄〃
 * @param message 鍐茬獊娑堟伅
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
