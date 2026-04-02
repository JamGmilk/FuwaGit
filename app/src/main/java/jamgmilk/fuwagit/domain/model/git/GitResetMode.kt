package jamgmilk.fuwagit.domain.model.git

/**
 * Git Reset 模式
 *
 * @param description 模式描述
 */
enum class GitResetMode(val description: String) {
    /**
     * Soft Reset: 仅移动 HEAD 指针，保留所有更改在暂存区
     * - 移动 HEAD 到指定 commit
     * - 保留所有更改在 index (staged)
     * - 工作目录保持不变
     */
    SOFT("Move HEAD only, keep all changes staged"),

    /**
     * Mixed Reset (默认): 移动 HEAD 指针，取消暂存所有更改
     * - 移动 HEAD 到指定 commit
     * - 取消暂存所有更改 (unstaged)
     * - 工作目录保持不变
     */
    MIXED("Move HEAD and unstage all changes (default)"),

    /**
     * Hard Reset: 完全重置到指定 commit，丢弃所有更改
     * - 移动 HEAD 到指定 commit
     * - 重置 index 到指定 commit
     * - 重置工作目录到指定 commit (丢弃所有未提交的更改)
     */
    HARD("Move HEAD and discard all changes (dangerous)")
}
