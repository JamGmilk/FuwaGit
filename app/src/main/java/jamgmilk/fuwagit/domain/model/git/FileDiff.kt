package jamgmilk.fuwagit.domain.model.git

/**
 * Inline diff segment (character-level diff)
 * Used to highlight which specific characters changed in a line
 *
 * @param content Segment content
 * @param isAdded true if these characters are added, false if deleted
 * @param startIndex Starting position in the original line content
 */
data class InlineDiffSegment(
    val content: String,
    val isAdded: Boolean,
    val startIndex: Int
)

/**
 * Inline diff information
 *
 * @param segments List of diff segments within a line
 * @param hasInlineDiff Whether there is inline diff
 */
data class InlineDiff(
    val segments: List<InlineDiffSegment>
) {
    val hasInlineDiff: Boolean get() = segments.size > 1
}

/**
 * Diff line type
 */
enum class DiffLineType {
    /** Added line */
    Added,
    /** Deleted line */
    Deleted,
    /** Unchanged context line */
    Context,
    /** File header information */
    Header
}

/**
 * Single line in a diff
 *
 * @param content Line content (without diff symbols)
 * @param lineType Line type
 * @param oldLineNumber Line number in old file (starting from 1, null for added lines)
 * @param newLineNumber Line number in new file (starting from 1, null for deleted lines)
 * @param inlineDiff Inline diff info (for highlighting character-level changes)
 */
data class DiffLine(
    val content: String,
    val lineType: DiffLineType,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null,
    val inlineDiff: InlineDiff? = null
) {
    /**
     * Get line display with symbols (for raw diff display)
     */
    val displayContent: String get() = when (lineType) {
        DiffLineType.Added -> "+ $content"
        DiffLineType.Deleted -> "- $content"
        DiffLineType.Context -> "  $content"
        DiffLineType.Header -> content
    }
}

/**
 * Diff Hunk - A changed block in file diff
 *
 * @param header Hunk header (e.g., @@ -1,5 +1,6 @@)
 * @param lines All lines in this hunk
 * @param oldStart Starting line number in old file
 * @param oldCount Number of lines in old file
 * @param newStart Starting line number in new file
 * @param newCount Number of lines in new file
 */
data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>,
    val oldStart: Int = 0,
    val oldCount: Int = 0,
    val newStart: Int = 0,
    val newCount: Int = 0
) {
    /**
     * Get number of added lines
     */
    val addedLines: Int get() = lines.count { it.lineType == DiffLineType.Added }

    /**
     * Get number of deleted lines
     */
    val deletedLines: Int get() = lines.count { it.lineType == DiffLineType.Deleted }
}

/**
 * File diff result
 *
 * @param oldPath Old file path (original path when renamed)
 * @param newPath New file path (new path when renamed)
 * @param changeType Change type
 * @param hunks List of diff hunks
 * @param additions Total number of added lines
 * @param deletions Total number of deleted lines
 * @param isBinary Whether this is a binary file
 * @param oldContent Old file content (for side-by-side display)
 * @param newContent New file content (for side-by-side display)
 */
data class FileDiff(
    val oldPath: String,
    val newPath: String,
    val changeType: GitChangeType,
    val hunks: List<DiffHunk> = emptyList(),
    val additions: Int = 0,
    val deletions: Int = 0,
    val isBinary: Boolean = false,
    val oldContent: String? = null,
    val newContent: String? = null
) {
    /**
     * File path (prefers new path)
     */
    val path: String get() = if (changeType == GitChangeType.Renamed) "$oldPath → $newPath" else newPath

    /**
     * File name
     */
    val fileName: String get() = newPath.substringAfterLast("/")

    /**
     * Total number of changed lines
     */
    val totalChanges: Int get() = additions + deletions

    /**
     * Whether only whitespace changed
     */
    val isWhitespaceOnly: Boolean
        get() {
            if (hunks.isEmpty()) return false
            return hunks.all { hunk ->
                hunk.lines.all { line ->
                    line.content.trim().isEmpty()
                }
            }
        }
}

/**
 * Working tree file diff query parameters
 *
 * @param filePath File path
 * @param isStaged Whether to view staged changes (true: staged vs HEAD, false: working tree vs staged)
 */
data class WorkingDiffParams(
    val filePath: String,
    val isStaged: Boolean = false
)

/**
 * File diff query parameters between commits
 *
 * @param filePath File path
 * @param oldCommit Old commit hash (supports HEAD~1, HEAD^, etc.)
 * @param newCommit New commit hash (defaults to HEAD)
 */
data class CommitDiffParams(
    val filePath: String,
    val oldCommit: String,
    val newCommit: String = "HEAD"
)
