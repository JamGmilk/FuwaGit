package jamgmilk.fuwagit.domain.model.git

/**
 * 推送选项配置
 *
 * @param pushCurrentBranch 是否推送当前分支（默认 true）
 * @param pushTags 是否推送标签（默认 false）
 * @param pushAllBranches 是否推送所有分支（默认 false）
 * @param remote 远程仓库名称（默认 "origin"）
 * @param branch 指定要推送的分支名称（如果为 null 则使用当前分支）
 * @param forcePush 是否强制推送（覆盖远程分支，默认 false）
 * @param forceWithLease 是否使用 force-with-lease 推送（更安全的强制推送，默认 false）
 */
data class GitPushOptions(
    val pushCurrentBranch: Boolean = true,
    val pushTags: Boolean = false,
    val pushAllBranches: Boolean = false,
    val remote: String = "origin",
    val branch: String? = null,
    val forcePush: Boolean = false,
    val forceWithLease: Boolean = false,
    val setUpstreamOnPush: Boolean = true
) {
    companion object {
        /**
         * 默认配置：仅推送当前分支
         */
        fun default() = GitPushOptions()

        /**
         * 推送所有分支和 tags（原 setPushAll 行为）
         */
        fun all() = GitPushOptions(
            pushAllBranches = true,
            pushTags = true
        )

        /**
         * 强制推送当前分支
         */
        fun force() = GitPushOptions(
            pushCurrentBranch = true,
            forcePush = true
        )
    }
}
