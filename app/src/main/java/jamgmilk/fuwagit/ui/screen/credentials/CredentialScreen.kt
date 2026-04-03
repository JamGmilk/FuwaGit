package jamgmilk.fuwagit.ui.screen.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.util.calculateFingerprint
import jamgmilk.fuwagit.core.util.detectSshKeyType
import jamgmilk.fuwagit.core.util.generateSshKeyPair
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import kotlinx.coroutines.launch

private sealed class CredentialDialogState {
    data object None : CredentialDialogState()
    data object AddHttps : CredentialDialogState()
    data object GenerateSsh : CredentialDialogState()
    data object ImportSsh : CredentialDialogState()
    data object ExportCredentials : CredentialDialogState()
    data object ImportCredentials : CredentialDialogState()
    data class HttpsInfo(val credential: HttpsCredential) : CredentialDialogState()
    data class SshInfo(val key: SshKey) : CredentialDialogState()
}

@Composable
fun CredentialScreen(
    viewModel: CredentialStoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()

    var dialogState by remember { mutableStateOf<CredentialDialogState>(CredentialDialogState.None) }
    var showDeleteConfirm by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    SubSettingsTemplate(
        title = "Credentials",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) {
        if (!uiState.isMasterPasswordSet) {
            SetupMasterPasswordContent(
                onConfirm = { password, hint ->
                    viewModel.setupMasterPassword(password, password, hint)
                },
                error = uiState.error,
                isLoading = uiState.isLoading
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecuritySettingsSection(
                    isDecryptionUnlocked = uiState.isDecryptionUnlocked,
                    onExport = {
                        if (!uiState.isDecryptionUnlocked) {
                            viewModel.showUnlockDialog()
                        } else {
                            dialogState = CredentialDialogState.ExportCredentials
                        }
                    },
                    onImport = {
                        if (!uiState.isDecryptionUnlocked) {
                            viewModel.showUnlockDialog()
                        } else {
                            dialogState = CredentialDialogState.ImportCredentials
                        }
                    },
                    onLockToggle = {
                        if (uiState.isDecryptionUnlocked) {
                            viewModel.lock()
                        } else {
                            viewModel.showUnlockDialog()
                        }
                    }
                )

                HttpsCredentialsSection(
                    credentials = uiState.httpsCredentials,
                    onAdd = {
                        if (!uiState.isDecryptionUnlocked) {
                            viewModel.showUnlockDialog()
                        } else {
                            dialogState = CredentialDialogState.AddHttps
                        }
                    },
                    onInfo = { dialogState = CredentialDialogState.HttpsInfo(it) }
                )

                SshKeysSection(
                    keys = uiState.sshKeys,
                    onGenerate = {
                        if (!uiState.isDecryptionUnlocked) {
                            viewModel.showUnlockDialog()
                        } else {
                            dialogState = CredentialDialogState.GenerateSsh
                        }
                    },
                    onImport = {
                        if (!uiState.isDecryptionUnlocked) {
                            viewModel.showUnlockDialog()
                        } else {
                            dialogState = CredentialDialogState.ImportSsh
                        }
                    },
                    onInfo = { dialogState = CredentialDialogState.SshInfo(it) }
                )
            }
        }
    }

    if (uiState.showUnlockDialog) {
        UnlockDialog(
            onDismiss = { viewModel.dismissUnlockDialog() },
            onUnlock = { password ->
                viewModel.unlockWithPassword(password)
            },
            biometricEnabled = uiState.isBiometricEnabled,
            onUnlockWithBiometric = {
                activity?.let { viewModel.unlockWithBiometric(it) }
            },
            passwordHint = uiState.passwordHint,
            error = uiState.error,
            isLoading = uiState.isLoading
        )
    }

    when (val state = dialogState) {
        is CredentialDialogState.AddHttps -> {
            AddHttpsCredentialDialog(
                onDismiss = { dialogState = CredentialDialogState.None },
                onAdd = { host, username, password ->
                    viewModel.addHttpsCredential(host, username, password)
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.GenerateSsh -> {
            GenerateSshKeyDialog(
                onDismiss = { dialogState = CredentialDialogState.None },
                onGenerate = { name, type, comment ->
                    try {
                        val keyPair = generateSshKeyPair(type, comment)
                        if (keyPair.first.isNotBlank() && keyPair.second.isNotBlank()) {
                            val fingerprint = calculateFingerprint(keyPair.first)
                            viewModel.addSshKey(
                                name = name,
                                type = type,
                                publicKey = keyPair.first,
                                privateKey = keyPair.second,
                                passphrase = null,
                                fingerprint = fingerprint.ifBlank { "unknown" }
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to generate SSH key pair")
                            }
                        }
                    } catch (e: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Error generating SSH key: ${e.message}")
                        }
                    }
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.ImportSsh -> {
            ImportSshKeyDialog(
                onDismiss = { dialogState = CredentialDialogState.None },
                onImport = { name, privateKey, publicKey, passphrase ->
                    if (privateKey.isNotBlank()) {
                        try {
                            val fingerprint = calculateFingerprint(publicKey ?: "")
                            viewModel.addSshKey(
                                name = name,
                                type = detectSshKeyType(privateKey),
                                publicKey = publicKey ?: "",
                                privateKey = privateKey,
                                passphrase = passphrase,
                                fingerprint = fingerprint.ifBlank { "unknown" }
                            )
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Error importing SSH key: ${e.message}")
                            }
                        }
                    }
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.HttpsInfo -> {
            HttpsCredentialInfoDialog(
                credential = state.credential,
                viewModel = viewModel,
                isDecryptionUnlocked = uiState.isDecryptionUnlocked,
                snackbarHostState = snackbarHostState,
                onDismiss = { dialogState = CredentialDialogState.None },
                onDelete = {
                    viewModel.deleteHttpsCredential(state.credential.uuid)
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.SshInfo -> {
            SshKeyInfoDialog(
                key = state.key,
                viewModel = viewModel,
                isDecryptionUnlocked = uiState.isDecryptionUnlocked,
                snackbarHostState = snackbarHostState,
                onDismiss = { dialogState = CredentialDialogState.None },
                onDelete = {
                    viewModel.deleteSshKey(state.key.uuid)
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.ExportCredentials -> {
            ExportCredentialsDialog(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onDismiss = { dialogState = CredentialDialogState.None }
            )
        }
        is CredentialDialogState.ImportCredentials -> {
            ImportCredentialsDialog(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onDismiss = { dialogState = CredentialDialogState.None }
            )
        }
        is CredentialDialogState.None -> {}
    }

    showDeleteConfirm?.let { (id, isSsh) ->
        DeleteConfirmDialog(
            isSsh = isSsh,
            onDismiss = { showDeleteConfirm = null },
            onConfirm = {
                if (isSsh) {
                    viewModel.deleteSshKey(id)
                } else {
                    viewModel.deleteHttpsCredential(id)
                }
                showDeleteConfirm = null
            }
        )
    }
}
