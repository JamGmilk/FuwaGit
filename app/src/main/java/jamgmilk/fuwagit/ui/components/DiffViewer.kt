package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.git.DiffHunk
import jamgmilk.fuwagit.domain.model.git.DiffLine
import jamgmilk.fuwagit.domain.model.git.DiffLineType
import jamgmilk.fuwagit.domain.model.git.FileDiff

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
 * 单行 Diff 显示
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

        // 行内容
        Text(
            text = line.content,
            color = textColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
                .padding(horizontal = 4.dp)
        )
    }
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
            text = "Binary file not shown",
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
            text = "No changes",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
