package jamgmilk.fuwagit.domain.model.git

/**
 * 鎺ㄩ€侀€夐」閰嶇疆
 *
 * @param pushCurrentBranch 鏄惁鎺ㄩ€佸綋鍓嶅垎鏀紙榛樿 true锛?
 * @param pushTags 鏄惁鎺ㄩ€佹爣绛撅紙榛樿 false锛?
 * @param pushAllBranches 鏄惁鎺ㄩ€佹墍鏈夊垎鏀紙榛樿 false锛?
 * @param remote 杩滅▼浠撳簱鍚嶇О锛堥粯璁?"origin"锛?
 * @param branch 鎸囧畾瑕佹帹閫佺殑鍒嗘敮鍚嶇О锛堝鏋滀负 null 鍒欎娇鐢ㄥ綋鍓嶅垎鏀級
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
         * 榛樿閰嶇疆锛氫粎鎺ㄩ€佸綋鍓嶅垎鏀?
         */
        fun default() = GitPushOptions()

        /**
         * 鎺ㄩ€佸綋鍓嶅垎鏀拰 tags
         */
        fun withTags() = GitPushOptions(
            pushCurrentBranch = true,
            pushTags = true
        )

        /**
         * 鎺ㄩ€佹墍鏈夊垎鏀拰 tags锛堝師 setPushAll 琛屼负锛?
         */
        fun all() = GitPushOptions(
            pushAllBranches = true,
            pushTags = true
        )
    }
}
