package jamgmilk.fuwagit.ui.screen.tags

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitTag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TagDetailDialog(
    tag: GitTag,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    
    // Pre-fetch strings for use in non-composable contexts
    val strTagNameLabel = stringResource(R.string.tags_tag_name_label)
    val strTagNameCopied = stringResource(R.string.tags_tag_name_copied)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.tags_tag_details),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                DetailRow(label = stringResource(R.string.tags_tag_name_label), value = tag.name)
                DetailRow(
                    label = stringResource(R.string.tags_tag_type_label),
                    value = if (tag.isAnnotated) stringResource(R.string.tags_annotated_badge) else stringResource(R.string.tags_lightweight_badge)
                )
                DetailRow(
                    label = stringResource(R.string.tags_tag_target_commit),
                    value = tag.targetHash,
                    isMonospace = true
                )
                
                if (tag.isAnnotated) {
                    if (tag.taggerName != null) {
                        val taggerInfo = buildString {
                            append(tag.taggerName)
                            if (tag.taggerEmail != null) append(" <${tag.taggerEmail}>")
                        }
                        DetailRow(label = stringResource(R.string.tags_tag_tagger), value = taggerInfo)
                    }
                    if (tag.message != null) {
                        DetailRow(label = stringResource(R.string.tags_tag_message), value = tag.message, isMultiLine = true)
                    }
                }
                
                if (tag.timestamp != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                    DetailRow(label = stringResource(R.string.tags_tag_created), value = dateFormat.format(Date(tag.timestamp)))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(strTagNameLabel, tag.name)
                    clipboard.setPrimaryClip(clip)
                    onShowSnackbar(strTagNameCopied)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    isMultiLine: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = if (isMonospace) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodySmall,
            color = colors.onSurface,
            maxLines = if (isMultiLine) 5 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
