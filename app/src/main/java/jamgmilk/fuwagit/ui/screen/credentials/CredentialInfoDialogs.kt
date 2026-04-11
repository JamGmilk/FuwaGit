package jamgmilk.fuwagit.ui.screen.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import kotlinx.coroutines.launch

@Composable
fun HttpsCredentialInfoDialog(
    credential: HttpsCredential,
    viewModel: CredentialStoreViewModel,
    isDecryptionUnlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hostCopiedText = stringResource(R.string.credentials_host_copied)
    val usernameCopiedText = stringResource(R.string.credentials_username_copied)
    val passwordCopiedText = stringResource(R.string.credentials_password_copied)

    var passwordValue by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(credential.uuid, isDecryptionUnlocked) {
        if (isDecryptionUnlocked) {
            passwordValue = viewModel.getHttpsPassword(credential.uuid)
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            isSsh = false,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { onDelete(); showDeleteConfirm = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.size(48.dp).background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SensitiveInfoRow(
                    label = stringResource(R.string.credential_info_host), value = credential.host,
                    onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, credential.host)); scope.launch { snackbarHostState.showSnackbar(message = hostCopiedText, duration = SnackbarDuration.Short) } }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                SensitiveInfoRow(
                    label = stringResource(R.string.credential_info_username), value = credential.username,
                    onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, credential.username)); scope.launch { snackbarHostState.showSnackbar(message = usernameCopiedText, duration = SnackbarDuration.Short) } }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                if (!isDecryptionUnlocked && passwordValue == null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.credentials_password_label), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(80.dp))
                        FilledTonalButton(
                            onClick = { viewModel.showUnlockDialog() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primary.copy(alpha = 0.15f), contentColor = colors.primary)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.credentials_unlock_to_reveal))
                        }
                    }
                } else {
                    SensitiveInfoRow(
                        label = stringResource(R.string.credential_info_password), value = passwordValue ?: "", isSensitive = true, isRevealed = showPassword,
                        onToggleReveal = { showPassword = !showPassword },
                        onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, passwordValue ?: "")); scope.launch { snackbarHostState.showSnackbar(message = passwordCopiedText, duration = SnackbarDuration.Short) } }
                    )
                }
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = stringResource(R.string.credential_info_created), value = formatTimestamp(credential.createdAt))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = stringResource(R.string.credential_info_updated), value = formatTimestamp(credential.updatedAt))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_delete))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SshKeyInfoDialog(
    key: SshKey,
    viewModel: CredentialStoreViewModel,
    isDecryptionUnlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val scope = rememberCoroutineScope()
    val nameCopiedText = stringResource(R.string.credentials_name_copied)
    val typeCopiedText = stringResource(R.string.credentials_type_copied)
    val fingerprintCopiedText = stringResource(R.string.credentials_fingerprint_copied)
    val commentCopiedText = stringResource(R.string.credentials_comment_copied)
    val publicKeyCopiedText = stringResource(R.string.credentials_public_key_copied)
    val privateKeyCopiedText = stringResource(R.string.credentials_private_key_copied)

    var privateKeyValue by remember { mutableStateOf<String?>(null) }
    var showPrivateKey by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(key.uuid, isDecryptionUnlocked) {
        if (isDecryptionUnlocked) {
            privateKeyValue = viewModel.getSshPrivateKey(key.uuid)
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            isSsh = true,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { onDelete(); showDeleteConfirm = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.size(48.dp).background(colors.tertiary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(24.dp))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SensitiveInfoRow(label = stringResource(R.string.credential_info_name), value = key.name, onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, key.name)); scope.launch { snackbarHostState.showSnackbar(message = nameCopiedText, duration = SnackbarDuration.Short) } })
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                SensitiveInfoRow(label = stringResource(R.string.credential_info_type), value = key.type, valueColor = colors.tertiary, onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, key.type)); scope.launch { snackbarHostState.showSnackbar(message = typeCopiedText, duration = SnackbarDuration.Short) } })
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                SensitiveInfoRow(label = stringResource(R.string.credential_info_fingerprint), value = key.fingerprint, isMonospace = true, onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, key.fingerprint)); scope.launch { snackbarHostState.showSnackbar(message = fingerprintCopiedText, duration = SnackbarDuration.Short) } })
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                if (key.comment.isNotBlank()) {
                    SensitiveInfoRow(label = stringResource(R.string.credential_info_comment), value = key.comment, onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, key.comment)); scope.launch { snackbarHostState.showSnackbar(message = commentCopiedText, duration = SnackbarDuration.Short) } })
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                }
                SensitiveInfoRow(label = stringResource(R.string.credential_info_public_key), value = key.publicKey, isMonospace = true, isSensitive = false, isRevealed = true, showToggle = false, onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, key.publicKey)); scope.launch { snackbarHostState.showSnackbar(message = publicKeyCopiedText, duration = SnackbarDuration.Short) } })
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                if (!isDecryptionUnlocked && privateKeyValue == null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.credentials_private_key_label), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(80.dp))
                        FilledTonalButton(
                            onClick = { viewModel.showUnlockDialog() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.tertiary.copy(alpha = 0.15f), contentColor = colors.tertiary)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.credentials_unlock_to_reveal))
                        }
                    }
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                } else if (privateKeyValue != null) {
                    SensitiveInfoRow(label = stringResource(R.string.credential_info_private_key), value = privateKeyValue ?: "", isMonospace = true, isSensitive = true, isRevealed = showPrivateKey, onToggleReveal = { showPrivateKey = !showPrivateKey }, onCopy = { clipboardManager.setPrimaryClip(ClipData.newPlainText(null, privateKeyValue ?: "")); scope.launch { snackbarHostState.showSnackbar(message = privateKeyCopiedText, duration = SnackbarDuration.Short) } })
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                }
                InfoRow(label = stringResource(R.string.credential_info_passphrase), value = if (key.passphrase != null) stringResource(R.string.credential_info_passphrase_protected) else stringResource(R.string.credential_info_passphrase_none), valueColor = if (key.passphrase != null) colors.tertiary else colors.onSurfaceVariant)
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = stringResource(R.string.credential_info_created), value = formatTimestamp(key.createdAt))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_delete))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
