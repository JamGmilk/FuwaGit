package jamgmilk.fuwagit.ui.screen.credentials

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import kotlinx.coroutines.launch

@Composable
fun ExportCredentialsDialog(
    viewModel: CredentialStoreViewModel,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(false) }
    val copiedMessage = stringResource(R.string.credentials_copied_to_clipboard)

    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.exportCredentials()
        isLoading = false
    }

    val exportedData = uiState.exportedData

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.size(48.dp).background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text(text = stringResource(R.string.credentials_export_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (isLoading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.credentials_exporting))
                    }
                } else if (exportedData != null) {
                    Text(text = stringResource(R.string.credentials_export_success), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${exportedData.take(50)}...", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, exportedData)); scope.launch { snackbarHostState.showSnackbar(copiedMessage) } }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.action_copy), tint = colors.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val warningOrange = Color(0xFFFF9800)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(warningOrange.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = warningOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(text = stringResource(R.string.credentials_security_warning), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = warningOrange)
                            Spacer(Modifier.height(4.dp))
                            Text(text = stringResource(R.string.credentials_export_warning), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = colors.primary), shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.action_done))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ImportCredentialsDialog(
    viewModel: CredentialStoreViewModel,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var importData by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.size(48.dp).background(colors.secondary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text(text = stringResource(R.string.credentials_import_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\u26A0", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.credentials_import_warning), style = MaterialTheme.typography.bodySmall, color = colors.onErrorContainer)
                }

                Text(text = stringResource(R.string.credentials_import_paste_description), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

                OutlinedTextField(
                    value = importData,
                    onValueChange = { importData = it },
                    label = { Text(stringResource(R.string.credentials_import_data_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_import_paste_placeholder)) },
                    minLines = 5,
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.credentials_importing))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (importData.isNotBlank()) {
                        isLoading = true
                        viewModel.importCredentials(importData)
                        isLoading = false
                    }
                },
                enabled = importData.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_import))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
