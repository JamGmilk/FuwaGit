package jamgmilk.fuwagit.domain.model.git

/**
 * Git Reset 妯″紡
 *
 * @param description 妯″紡鎻忚堪
 */
enum class GitResetMode(val description: String) {
    /**
     * Soft Reset: 浠呯Щ鍔?HEAD 鎸囬拡锛屼繚鐣欐墍鏈夋洿鏀瑰湪鏆傚瓨鍖?
     * - 绉诲姩 HEAD 鍒版寚瀹?commit
     * - 淇濈暀鎵€鏈夋洿鏀瑰湪 index (staged)
     * - 宸ヤ綔鐩綍淇濇寔涓嶅彉
     */
    SOFT("Move HEAD only, keep all changes staged"),

    /**
     * Mixed Reset (榛樿): 绉诲姩 HEAD 鎸囬拡锛屽彇娑堟殏瀛樻墍鏈夋洿鏀?
     * - 绉诲姩 HEAD 鍒版寚瀹?commit
     * - 鍙栨秷鏆傚瓨鎵€鏈夋洿鏀?(unstaged)
     * - 宸ヤ綔鐩綍淇濇寔涓嶅彉
     */
    MIXED("Move HEAD and unstage all changes (default)"),

    /**
     * Hard Reset: 瀹屽叏閲嶇疆鍒版寚瀹?commit锛屼涪寮冩墍鏈夋洿鏀?
     * - 绉诲姩 HEAD 鍒版寚瀹?commit
     * - 閲嶇疆 index 鍒版寚瀹?commit
     * - 閲嶇疆宸ヤ綔鐩綍鍒版寚瀹?commit (涓㈠純鎵€鏈夋湭鎻愪氦鐨勬洿鏀?
     */
    HARD("Move HEAD and discard all changes (dangerous)")
}
