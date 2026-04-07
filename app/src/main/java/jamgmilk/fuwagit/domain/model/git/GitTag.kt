package jamgmilk.fuwagit.domain.model.git

/**
 * Git Tag 类型
 */
enum class GitTagType {
    /** 轻量标签（仅指向特定提交的引用） */
    Lightweight,
    /** 附注标签（包含完整的标签对象，有标签信息和签名） */
    Annotated
}

/**
 * Git Tag 领域模型
 *
 * @param name 标签名称
 * @param fullRef 完整引用路径（如 refs/tags/v1.0）
 * @param type 标签类型（轻量/附注）
 * @param targetHash 标签指向的提交哈希
 * @param taggerName 标签创建者（仅附注标签）
 * @param taggerEmail 标签创建者邮箱（仅附注标签）
 * @param message 标签消息（仅附注标签）
 * @param timestamp 标签创建时间戳
 * @param isPushed 是否已推送到远程
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
