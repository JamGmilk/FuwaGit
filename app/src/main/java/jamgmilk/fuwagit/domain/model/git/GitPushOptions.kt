package jamgmilk.fuwagit.domain.model.git

/**
 * 推送选项配置
 *
 * @param pushCurrentBranch 是否推送当前分支（默认 true）
 * @param pushTags 是否推送标签（默认 false）
 * @param pushAllBranches 是否推送所有分支（默认 false）
 * @param remote 远程仓库名称（默认 "origin"）
 * @param branch 指定要推送的分支名称（如果为 null 则使用当前分支）
 */
data class GitPushOptions(
    val pushCurrentBranch: Boolean = true,
    val pushTags: Boolean = false,
    val pushAllBranches: Boolean = false,
    val remote: String = "origin",
    val branch: String? = null
) {
    companion object {
        /**
         * 默认配置：仅推送当前分支
         */
        fun default() = GitPushOptions()

        /**
         * 推送当前分支和 tags
         */
        fun withTags() = GitPushOptions(
            pushCurrentBranch = true,
            pushTags = true
        )

        /**
         * 推送所有分支和 tags（原 setPushAll 行为）
         */
        fun all() = GitPushOptions(
            pushAllBranches = true,
            pushTags = true
        )
    }
}
