package jamgmilk.fuwagit.domain.model.git

/**
 * Conflict file status
 */
enum class ConflictStatus {
    /** Unresolved */
    UNRESOLVED,
    /** Marked as resolved */
    RESOLVED,
    /** Added to staging area */
    STAGED
}

/**
 * Conflict file information
 *
 * @param path File path
 * @param name File name
 * @param status Conflict status
 * @param description Conflict description (e.g., "both modified")
 */
data class GitConflict(
    val path: String,
    val name: String,
    val status: ConflictStatus = ConflictStatus.UNRESOLVED,
    val description: String = ""
)

/**
 * Merge/Rebase conflict result
 *
 * @param isConflicting Whether it is currently in a conflicting state
 * @param operationType Type of operation (MERGE/REBASE)
 * @param conflicts List of conflicted files
 * @param message Conflict message
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
