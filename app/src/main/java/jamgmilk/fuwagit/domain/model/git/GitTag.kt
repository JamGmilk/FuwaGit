package jamgmilk.fuwagit.domain.model.git

/**
 * Git Tag type
 */
enum class GitTagType {
    /** Lightweight tag - a reference to a specific commit */
    Lightweight,
    /** Annotated tag - contains full tag object with tag info and signature */
    Annotated
}

/**
 * Git Tag domain model
 *
 * @param name Tag name
 * @param fullRef Full reference path (e.g., refs/tags/v1.0)
 * @param type Tag type (Lightweight/Annotated)
 * @param targetHash Commit hash that the tag points to
 * @param taggerName Tag creator (Annotated tags only)
 * @param taggerEmail Tag creator email (Annotated tags only)
 * @param message Tag message (Annotated tags only)
 * @param timestamp Tag creation timestamp
 * @param isPushed Whether the tag has been pushed to remote
 */
data class GitTag(
    val name: String,
    val fullRef: String,
    val type: GitTagType,
    val targetHash: String,
    val taggerName: String? = null,
    val taggerEmail: String? = null,
    val message: String? = null,
    val timestamp: Long? = null,
    val isPushed: Boolean = false
) {
    val isAnnotated: Boolean get() = type == GitTagType.Annotated
    val isLightweight: Boolean get() = type == GitTagType.Lightweight
}
