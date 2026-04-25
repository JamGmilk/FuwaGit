package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.DiffHunk
import jamgmilk.fuwagit.domain.model.git.DiffLine
import jamgmilk.fuwagit.domain.model.git.DiffLineType
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.model.git.InlineDiff
import jamgmilk.fuwagit.ui.theme.diffAddedBackgroundDark
import jamgmilk.fuwagit.ui.theme.diffAddedBackgroundLight
import jamgmilk.fuwagit.ui.theme.diffAddedHighlightDark
import jamgmilk.fuwagit.ui.theme.diffAddedHighlightLight
import jamgmilk.fuwagit.ui.theme.diffAddedInlineBackgroundDark
import jamgmilk.fuwagit.ui.theme.diffAddedInlineBackgroundLight
import jamgmilk.fuwagit.ui.theme.diffAddedTextDark
import jamgmilk.fuwagit.ui.theme.diffAddedTextLight
import jamgmilk.fuwagit.ui.theme.diffDeletedBackgroundDark
import jamgmilk.fuwagit.ui.theme.diffDeletedBackgroundLight
import jamgmilk.fuwagit.ui.theme.diffDeletedHighlightDark
import jamgmilk.fuwagit.ui.theme.diffDeletedHighlightLight
import jamgmilk.fuwagit.ui.theme.diffDeletedInlineBackgroundDark
import jamgmilk.fuwagit.ui.theme.diffDeletedInlineBackgroundLight
import jamgmilk.fuwagit.ui.theme.diffDeletedTextDark
import jamgmilk.fuwagit.ui.theme.diffDeletedTextLight
import jamgmilk.fuwagit.ui.util.CodeSyntaxHighlighter

private val isDarkTheme: Boolean
    @Composable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

private object DiffColors {
    val addedBackground: Color
        @Composable get() = if (isDarkTheme) diffAddedBackgroundDark else diffAddedBackgroundLight
    val addedText: Color
        @Composable get() = if (isDarkTheme) diffAddedTextDark else diffAddedTextLight
    val deletedBackground: Color
        @Composable get() = if (isDarkTheme) diffDeletedBackgroundDark else diffDeletedBackgroundLight
    val deletedText: Color
        @Composable get() = if (isDarkTheme) diffDeletedTextDark else diffDeletedTextLight
    val addedHighlight: Color
        @Composable get() = if (isDarkTheme) diffAddedHighlightDark else diffAddedHighlightLight
    val deletedHighlight: Color
        @Composable get() = if (isDarkTheme) diffDeletedHighlightDark else diffDeletedHighlightLight
    val addedInlineBackground: Color
        @Composable get() = if (isDarkTheme) diffAddedInlineBackgroundDark else diffAddedInlineBackgroundLight
    val deletedInlineBackground: Color
        @Composable get() = if (isDarkTheme) diffDeletedInlineBackgroundDark else diffDeletedInlineBackgroundLight
}

@Composable
fun DiffViewer(
    fileDiff: FileDiff,
    modifier: Modifier = Modifier
) {
    val additionsText = stringResource(R.string.diff_additions_prefix, fileDiff.additions)
    val deletionsText = stringResource(R.string.diff_deletions_prefix, fileDiff.deletions)
    val fileHeaderDesc = "$additionsText $deletionsText ${fileDiff.changeType.name}"

    if (fileDiff.isBinary) {
        BinaryFileIndicator(modifier = modifier.semantics { contentDescription = fileDiff.path })
        return
    }

    if (fileDiff.hunks.isEmpty()) {
        NoChangesIndicator(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .semantics { contentDescription = fileHeaderDesc }
    ) {
        item(key = "file_header_${fileDiff.path}") {
            DiffFileHeader(fileDiff = fileDiff)
        }

        fileDiff.hunks.forEachIndexed { hunkIndex, hunk ->
            item(key = "hunk_header_$hunkIndex") {
                HunkHeader(hunk = hunk)
            }

            itemsIndexed(
                items = hunk.lines,
                key = { _, line -> "hunk_${hunkIndex}_line_${line.oldLineNumber}_${line.newLineNumber}" }
            ) { _, line ->
                DiffLineItem(line = line)
            }
        }
    }
}

@Composable
private fun DiffFileHeader(fileDiff: FileDiff) {
    val colors = MaterialTheme.colorScheme
    val additionsText = stringResource(R.string.diff_additions_prefix, fileDiff.additions)
    val deletionsText = stringResource(R.string.diff_deletions_prefix, fileDiff.deletions)
    val changeTypeText = stringResource(R.string.diff_change_type_format, fileDiff.changeType.name)
    val semanticsDesc = "${fileDiff.path}, $additionsText, $deletionsText, $changeTypeText"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh)
            .padding(12.dp)
            .semantics { contentDescription = semanticsDesc }
    ) {
        Text(
            text = fileDiff.path,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )

        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fileDiff.additions > 0) {
                Text(
                    text = additionsText,
                    color = DiffColors.addedText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (fileDiff.deletions > 0) {
                Text(
                    text = deletionsText,
                    color = DiffColors.deletedText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = changeTypeText,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HunkHeader(hunk: DiffHunk) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(vertical = 4.dp)
            .semantics { contentDescription = hunk.header }
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

@Composable
private fun DiffLineItem(line: DiffLine) {
    val colors = MaterialTheme.colorScheme

    val (backgroundColor, textColor, lineIndicator) = when (line.lineType) {
        DiffLineType.Added -> Triple(
            DiffColors.addedBackground.copy(alpha = 0.4f),
            DiffColors.addedText,
            "+"
        )
        DiffLineType.Deleted -> Triple(
            DiffColors.deletedBackground.copy(alpha = 0.4f),
            DiffColors.deletedText,
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

    val lineDesc = buildString {
        append(lineIndicator)
        append(" ")
        append(line.oldLineNumber ?: "")
        append(" -> ")
        append(line.newLineNumber ?: "")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .semantics { contentDescription = lineDesc }
    ) {
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

        Row(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            if (line.inlineDiff != null && line.inlineDiff.hasInlineDiff) {
                InlineDiffContent(
                    inlineDiff = line.inlineDiff,
                    lineType = line.lineType,
                    baseTextColor = textColor
                )
            } else {
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
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun InlineDiffContent(
    inlineDiff: InlineDiff,
    lineType: DiffLineType,
    baseTextColor: Color
) {
    val annotatedText = buildAnnotatedString {
        for (segment in inlineDiff.segments) {
            val style = if (segment.isAdded) {
                when (lineType) {
                    DiffLineType.Added -> SpanStyle(
                        background = DiffColors.addedHighlight.copy(alpha = 0.6f),
                        color = DiffColors.addedText,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    DiffLineType.Deleted -> SpanStyle(
                        background = DiffColors.deletedHighlight.copy(alpha = 0.4f),
                        color = baseTextColor,
                        textDecoration = TextDecoration.LineThrough,
                        fontFamily = FontFamily.Monospace
                    )
                    else -> SpanStyle(
                        background = DiffColors.addedHighlight.copy(alpha = 0.3f),
                        color = baseTextColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                when (lineType) {
                    DiffLineType.Added -> SpanStyle(
                        color = DiffColors.addedText,
                        fontFamily = FontFamily.Monospace
                    )
                    DiffLineType.Deleted -> SpanStyle(
                        color = DiffColors.deletedText,
                        fontFamily = FontFamily.Monospace
                    )
                    DiffLineType.Context -> {
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
        fontSize = 13.sp
    )
}

@Composable
private fun BinaryFileIndicator(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val binaryFileDesc = stringResource(R.string.diff_binary_file)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(24.dp)
            .semantics { contentDescription = binaryFileDesc },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = binaryFileDesc,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NoChangesIndicator(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val noChangesDesc = stringResource(R.string.diff_no_changes)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(24.dp)
            .semantics { contentDescription = noChangesDesc },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = noChangesDesc,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
