package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.git.DiffHunk
import jamgmilk.fuwagit.domain.model.git.DiffLine
import jamgmilk.fuwagit.domain.model.git.DiffLineType
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.model.git.InlineDiff
import jamgmilk.fuwagit.domain.model.git.InlineDiffSegment
import jamgmilk.fuwagit.ui.util.CodeSyntaxHighlighter

/**
 * Diff 查看器组件 - 显示单个文件的差异
 *
 * @param fileDiff 文件差异对象
 * @param modifier 修饰符
 */
@Composable
fun DiffViewer(
    fileDiff: FileDiff,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        // 文件头信息
        DiffFileHeader(fileDiff = fileDiff)

        // 二进制文件提示
        if (fileDiff.isBinary) {
            BinaryFileIndicator()
        } else {
            // Diff 内容
            if (fileDiff.hunks.isEmpty()) {
                NoChangesIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fileDiff.hunks.forEachIndexed { index, hunk ->
                        // Hunk 头
                        item(key = "hunk_header_$index") {
                            HunkHeader(hunk = hunk)
                        }

                        // Hunk 内容
                        items(
                            items = hunk.lines,
                            key = { line -> "${line.oldLineNumber}-${line.newLineNumber}-${line.content.hashCode()}" }
                        ) { line ->
                            DiffLineItem(line = line)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 文件头显示
 */
@Composable
private fun DiffFileHeader(fileDiff: FileDiff) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        // 文件路径
        Text(
            text = fileDiff.path,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // 变更统计
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fileDiff.additions > 0) {
                Text(
                    text = "+${fileDiff.additions}",
                    color = Color(0xFF22863A),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (fileDiff.deletions > 0) {
                Text(
                    text = " -${fileDiff.deletions}",
                    color = Color(0xFFB31D28),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = " (${fileDiff.changeType.name})",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Hunk 头显示
 */
@Composable
private fun HunkHeader(hunk: DiffHunk) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = hunk.header,
            color = colors.primary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/**
 * 单行 Diff 显示（支持行内差异高亮和语法高亮）
 */
@Composable
private fun DiffLineItem(line: DiffLine) {
    val colors = MaterialTheme.colorScheme

    val (backgroundColor, textColor, lineIndicator) = when (line.lineType) {
        DiffLineType.Added -> Triple(
            Color(0xFFCCFFD0).copy(alpha = 0.4f),
            Color(0xFF22863A),
            "+"
        )
        DiffLineType.Deleted -> Triple(
            Color(0xFFFFEDEF).copy(alpha = 0.4f),
            Color(0xFFB31D28),
            "-"
        )
        DiffLineType.Context -> Triple(
            colors.surface,
            colors.onSurface,
            " "
        )
        DiffLineType.Header -> Triple(
            colors.surfaceContainerLow,
            colors.primary,
            ""
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        // 旧行号
        Box(
            modifier = Modifier
                .width(50.dp)
                .align(Alignment.Top),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = line.oldLineNumber?.toString() ?: "",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // 新行号
        Box(
            modifier = Modifier
                .width(50.dp)
                .align(Alignment.Top),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = line.newLineNumber?.toString() ?: "",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // 行指示符 (+/-/ )
        Text(
            text = lineIndicator,
            color = textColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .width(20.dp)
                .align(Alignment.Top)
                .padding(start = 4.dp)
        )

        // 行内容（支持行内差异高亮和语法高亮）
        Row(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            if (line.inlineDiff != null && line.inlineDiff.hasInlineDiff) {
                // 有行内差异，高亮显示变化的部分
                InlineDiffContent(
                    inlineDiff = line.inlineDiff,
                    lineType = line.lineType,
                    baseTextColor = textColor
                )
            } else {
                // 无行内差异，使用语法高亮
                val highlightedText = if (line.lineType == DiffLineType.Context) {
                    CodeSyntaxHighlighter.highlightSimple(line.content)
                } else {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = textColor, fontFamily = FontFamily.Monospace)) {
                            append(line.content)
                        }
                    }
                }
                
                Text(
                    text = highlightedText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

/**
 * 行内差异内容显示
 * 高亮显示行中新增和删除的字符
 */
@Composable
private fun InlineDiffContent(
    inlineDiff: InlineDiff,
    lineType: DiffLineType,
    baseTextColor: Color
) {
    val addedHighlightColor = Color(0xFF94D1A4) // 更亮的绿色用于行内新增
    val deletedHighlightColor = Color(0xFFFFB3B3) // 更亮的红色用于行内删除

    val annotatedText = buildAnnotatedString {
        for (segment in inlineDiff.segments) {
            val style = if (segment.isAdded) {
                // 行内新增：深色背景 + 亮色前景
                when (lineType) {
                    DiffLineType.Added -> SpanStyle(
                        background = addedHighlightColor.copy(alpha = 0.6f),
                        color = Color(0xFF004D00),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    DiffLineType.Deleted -> SpanStyle(
                        background = deletedHighlightColor.copy(alpha = 0.4f),
                        color = baseTextColor,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        fontFamily = FontFamily.Monospace
                    )
                    else -> SpanStyle(
                        background = addedHighlightColor.copy(alpha = 0.3f),
                        color = baseTextColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // 未变更部分：使用语法高亮或默认样式
                when (lineType) {
                    DiffLineType.Added -> SpanStyle(
                        color = Color(0xFF22863A),
                        fontFamily = FontFamily.Monospace
                    )
                    DiffLineType.Deleted -> SpanStyle(
                        color = Color(0xFFB31D28),
                        fontFamily = FontFamily.Monospace
                    )
                    DiffLineType.Context -> {
                        // 上下文行使用语法高亮
                        SpanStyle(
                            color = baseTextColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    else -> SpanStyle(
                        color = baseTextColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            withStyle(style) {
                append(segment.content)
            }
        }
    }

    Text(
        text = annotatedText,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Visible
    )
}

/**
 * 二进制文件指示器
 */
@Composable
private fun BinaryFileIndicator() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = jamgmilk.fuwagit.R.string.diff_binary_file),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 无变更指示器
 */
@Composable
private fun NoChangesIndicator() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = jamgmilk.fuwagit.R.string.diff_no_changes),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
