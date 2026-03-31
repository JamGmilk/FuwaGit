package jamgmilk.fuwagit.ui.screen.credentials

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey
import jamgmilk.fuwagit.data.local.credential.extractCommentFromPublicKey
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.Sakura80
import jamgmilk.fuwagit.ui.theme.Sakura90
import kotlinx.coroutines.launch

sealed class CredentialsTab {
    data object Https : CredentialsTab()
    data object Ssh : CredentialsTab()
}

private sealed class CredentialDialogState {
    data object None : CredentialDialogState()
    data object SetupFirst : CredentialDialogState()
    data object AddHttps : CredentialDialogState()
    data object GenerateSsh : CredentialDialogState()
    data object ImportSsh : CredentialDialogState()
    data object ExportCredentials : CredentialDialogState()
    data object ImportCredentials : CredentialDialogState()
    data class HttpsInfo(val credential: HttpsCredential) : CredentialDialogState()
    data class SshInfo(val key: SshKey) : CredentialDialogState()
}

@Composable
fun CredentialsScreen(
    viewModel: CredentialsStoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf<CredentialsTab>(CredentialsTab.Https) }
    var dialogState by remember { mutableStateOf<CredentialDialogState>(CredentialDialogState.None) }
    var showDeleteConfirm by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    SubSettingsTemplate(
        title = "Credentials",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CredentialsTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                httpsCount = uiState.httpsCredentials.size,
                sshCount = uiState.sshKeys.size
            )

            SecuritySettingsSection(
                isBiometricEnabled = uiState.isBiometricEnabled,
                isDecryptionUnlocked = uiState.isDecryptionUnlocked,
                onToggleBiometric = {
                    if (!uiState.isDecryptionUnlocked) {
                        viewModel.showUnlockDialog()
                    } else if (uiState.isBiometricEnabled) {
                        viewModel.disableBiometric()
                    } else {
                        activity?.let {
                            viewModel.enableBiometric(
                                activity = it,
                                onSuccess = { },
                                onError = { error -> scope.launch { snackbarHostState.showSnackbar(error) } }
                            )
                        }
                    }
                },
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
                onLock = { viewModel.lock() }
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "credentials_tab_transition"
            ) { tab ->
                when (tab) {
                    is CredentialsTab.Https -> {
                        HttpsCredentialsSection(
                            credentials = uiState.httpsCredentials,
                            onAdd = {
                                when {
                                    !uiState.isMasterPasswordSet -> {
                                        dialogState = CredentialDialogState.SetupFirst
                                    }
                                    !uiState.isDecryptionUnlocked -> {
                                        viewModel.showUnlockDialog()
                                    }
                                    else -> {
                                        dialogState = CredentialDialogState.AddHttps
                                    }
                                }
                            },
                            onInfo = { dialogState = CredentialDialogState.HttpsInfo(it) }
                        )
                    }
                    is CredentialsTab.Ssh -> {
                        SshKeysSection(
                            keys = uiState.sshKeys,
                            onGenerate = {
                                when {
                                    !uiState.isMasterPasswordSet -> {
                                        dialogState = CredentialDialogState.SetupFirst
                                    }
                                    !uiState.isDecryptionUnlocked -> {
                                        viewModel.showUnlockDialog()
                                    }
                                    else -> {
                                        dialogState = CredentialDialogState.GenerateSsh
                                    }
                                }
                            },
                            onImport = {
                                when {
                                    !uiState.isMasterPasswordSet -> {
                                        dialogState = CredentialDialogState.SetupFirst
                                    }
                                    !uiState.isDecryptionUnlocked -> {
                                        viewModel.showUnlockDialog()
                                    }
                                    else -> {
                                        dialogState = CredentialDialogState.ImportSsh
                                    }
                                }
                            },
                            onInfo = { dialogState = CredentialDialogState.SshInfo(it) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showUnlockDialog) {
        UnlockDialog(
            onDismiss = { viewModel.dismissUnlockDialog() },
            onUnlock = { password ->
                viewModel.unlockWithPassword(password)
            },
            passwordHint = uiState.passwordHint,
            error = uiState.error,
            isLoading = uiState.isLoading
        )
    }

    when (val state = dialogState) {
        is CredentialDialogState.SetupFirst -> {
            SetupPasswordDialog(
                onDismiss = { dialogState = CredentialDialogState.None },
                onConfirm = { password, hint ->
                    viewModel.setupMasterPassword(password, password, hint)
                    dialogState = CredentialDialogState.None
                },
                error = uiState.error,
                isLoading = uiState.isLoading
            )
        }
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

@Composable
private fun SecurityStatusCard(
    isUnlocked: Boolean,
    isMasterPasswordSet: Boolean,
    isBiometricEnabled: Boolean,
    httpsCount: Int,
    sshCount: Int,
    onUnlockPassword: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Sakura80.copy(alpha = 0.15f), Sakura80.copy(alpha = 0.05f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Sakura80.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Sakura80,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vault Locked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Sakura80
                    )
                    Text(
                        text = if (isMasterPasswordSet) "Tap to unlock" else "Set up master password",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (isMasterPasswordSet) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isBiometricEnabled) {
                        FilledTonalButton(
                            onClick = onUnlockPassword,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Sakura80.copy(alpha = 0.12f),
                                contentColor = Sakura80
                            )
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Fingerprint")
                        }
                    }

                    Button(
                        onClick = onUnlockPassword,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Sakura80)
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Password")
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialsTabSelector(
    selectedTab: CredentialsTab,
    onTabSelected: (CredentialsTab) -> Unit,
    httpsCount: Int,
    sshCount: Int
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabChip(
            label = "HTTPS",
            count = httpsCount,
            icon = Icons.Default.Link,
            selected = selectedTab is CredentialsTab.Https,
            accentColor = Sakura80,
            onClick = { onTabSelected(CredentialsTab.Https) },
            modifier = Modifier.weight(1f)
        )

        TabChip(
            label = "SSH Keys",
            count = sshCount,
            icon = Icons.Default.Key,
            selected = selectedTab is CredentialsTab.Ssh,
            accentColor = Sakura90,
            onClick = { onTabSelected(CredentialsTab.Ssh) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabChip(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else uiColors.cardContainer,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, uiColors.cardBorder)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else colors.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (selected) accentColor.copy(alpha = 0.2f) else colors.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else colors.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun HttpsCredentialsSection(
    credentials: List<HttpsCredential>,
    onAdd: () -> Unit,
    onInfo: (HttpsCredential) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Sakura80.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Sakura80.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Sakura80,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "HTTPS Credentials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Sakura80
                    )
                }

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Sakura80.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Sakura80,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (credentials.isEmpty()) {
                EmptyStateContent(
                    icon = Icons.Default.Link,
                    title = "No HTTPS Credentials",
                    subtitle = "Add your first credential to securely store passwords and PATs",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    credentials.forEachIndexed { index, cred ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = uiColors.cardBorder
                            )
                        }
                        HttpsCredentialItem(
                            credential = cred,
                            onInfo = { onInfo(cred) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HttpsCredentialItem(
    credential: HttpsCredential,
    onInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInfo)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Sakura80.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Sakura80,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = credential.host,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onInfo,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = Sakura80,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SshKeysSection(
    keys: List<SshKey>,
    onGenerate: () -> Unit,
    onImport: () -> Unit,
    onInfo: (SshKey) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Sakura90.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Sakura90.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = Sakura90,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "SSH Keys",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Sakura90
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onGenerate,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Sakura90.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Generate",
                            tint = Sakura90,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onImport,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Sakura90.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Import",
                            tint = Sakura90,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (keys.isEmpty()) {
                EmptyStateContent(
                    icon = Icons.Default.Key,
                    title = "No SSH Keys",
                    subtitle = "Generate a new key pair or import an existing one",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    keys.forEachIndexed { index, key ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = uiColors.cardBorder
                            )
                        }
                        SshKeyItem(
                            key = key,
                            onInfo = { onInfo(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SshKeyItem(
    key: SshKey,
    onInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInfo)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Sakura90.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Key,
                contentDescription = null,
                tint = Sakura90,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = key.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Sakura90.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = key.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = Sakura90,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = key.fingerprint.take(20) + "...",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        IconButton(
            onClick = onInfo,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = Sakura90,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    colors.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun HttpsCredentialInfoDialog(
    credential: HttpsCredential,
    viewModel: CredentialsStoreViewModel,
    isDecryptionUnlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Sakura80.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = Sakura80,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensitiveInfoRow(
                    label = "Host",
                    value = credential.host,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(credential.host))
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Host copied", duration = SnackbarDuration.Short)
                        }
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                SensitiveInfoRow(
                    label = "Username",
                    value = credential.username,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(credential.username))
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Username copied", duration = SnackbarDuration.Short)
                        }
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                if (!isDecryptionUnlocked && passwordValue == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.width(80.dp)
                        )
                        FilledTonalButton(
                            onClick = { viewModel.showUnlockDialog() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Sakura80.copy(alpha = 0.15f),
                                contentColor = Sakura80
                            )
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unlock to Reveal")
                        }
                    }
                } else {
                    SensitiveInfoRow(
                        label = "Password",
                        value = passwordValue ?: "",
                        isSensitive = true,
                        isRevealed = showPassword,
                        onToggleReveal = { showPassword = !showPassword },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(passwordValue ?: ""))
                            scope.launch {
                                snackbarHostState.showSnackbar(message = "Password copied", duration = SnackbarDuration.Short)
                            }
                        }
                    )
                }
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = "Created", value = formatTimestamp(credential.created_at))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = "Updated", value = formatTimestamp(credential.updated_at))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SshKeyInfoDialog(
    key: SshKey,
    viewModel: CredentialsStoreViewModel,
    isDecryptionUnlocked: Boolean,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var privateKeyValue by remember { mutableStateOf<String?>(null) }
    var showPrivateKey by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var needsUnlock by remember { mutableStateOf(false) }

    LaunchedEffect(key.uuid, isDecryptionUnlocked) {
        if (isDecryptionUnlocked) {
            privateKeyValue = viewModel.getSshPrivateKey(key.uuid)
            needsUnlock = false
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            isSsh = true,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Sakura90.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = Sakura90,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensitiveInfoRow(
                    label = "Name",
                    value = key.name,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(key.name))
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Name copied", duration = SnackbarDuration.Short)
                        }
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                SensitiveInfoRow(
                    label = "Type",
                    value = key.type,
                    valueColor = Sakura90,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(key.type))
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Type copied", duration = SnackbarDuration.Short)
                        }
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                SensitiveInfoRow(
                    label = "Fingerprint",
                    value = key.fingerprint,
                    isMonospace = true,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(key.fingerprint))
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Fingerprint copied", duration = SnackbarDuration.Short)
                        }
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                if (key.comment.isNotBlank()) {
                    SensitiveInfoRow(
                        label = "Comment",
                        value = key.comment,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(key.comment))
                            scope.launch {
                                snackbarHostState.showSnackbar(message = "Comment copied", duration = SnackbarDuration.Short)
                            }
                        }
                    )
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                }
                SensitiveInfoRow(
                    label = "Public Key",
                    value = key.public_key,
                    isMonospace = true,
                    isSensitive = false,
                    isRevealed = true,
                    showToggle = false,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(key.public_key))
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Public key copied", duration = SnackbarDuration.Short)
                        }
                    }
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                if (!isDecryptionUnlocked && privateKeyValue == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Private Key",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.width(80.dp)
                        )
                        FilledTonalButton(
                            onClick = { viewModel.showUnlockDialog() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Sakura90.copy(alpha = 0.15f),
                                contentColor = Sakura90
                            )
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unlock to Reveal")
                        }
                    }
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                } else if (privateKeyValue != null) {
                    SensitiveInfoRow(
                        label = "Private Key",
                        value = privateKeyValue ?: "",
                        isMonospace = true,
                        isSensitive = true,
                        isRevealed = showPrivateKey,
                        onToggleReveal = { showPrivateKey = !showPrivateKey },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(privateKeyValue ?: ""))
                            scope.launch {
                                snackbarHostState.showSnackbar(message = "Private key copied", duration = SnackbarDuration.Short)
                            }
                        }
                    )
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                }
                InfoRow(
                    label = "Passphrase",
                    value = if (key.passphrase != null) "Protected" else "None",
                    valueColor = if (key.passphrase != null) Color(0xFFFF9800) else colors.onSurfaceVariant
                )
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = "Created", value = formatTimestamp(key.created_at))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SensitiveInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isMonospace: Boolean = false,
    isSensitive: Boolean = false,
    isRevealed: Boolean = false,
    showToggle: Boolean = true,
    onToggleReveal: () -> Unit = {},
    onCopy: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onCopy() })
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSensitive && !isRevealed) "••••••••••••" else value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                maxLines = if (isSensitive && !isRevealed) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isSensitive && showToggle) {
                IconButton(
                    onClick = onToggleReveal,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isRevealed) "Hide" else "Show",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun DeleteConfirmDialog(
    isSsh: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.error.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isSsh) "Delete SSH Key?" else "Delete Credential?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (isSsh) {
                    "This SSH key will be permanently removed from your vault. This action cannot be undone."
                } else {
                    "This credential will be permanently removed from your vault. This action cannot be undone."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun AddHttpsCredentialDialog(
    onDismiss: () -> Unit,
    onAdd: (host: String, username: String, password: String) -> Unit
) {
    var host by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Sakura80.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = Sakura80,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Add HTTPS Credential",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("github.com") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / PAT") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Password,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide" else "Show"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(host, username, password) },
                enabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Sakura80),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun GenerateSshKeyDialog(
    onDismiss: () -> Unit,
    onGenerate: (name: String, type: String, comment: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("ED25519") }
    var comment by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Sakura90.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = Sakura90,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Generate SSH Key",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Key Name") },
                    placeholder = { Text("My Key") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90,
                        cursorColor = Sakura90
                    )
                )

                Text(
                    text = "Key Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SshTypeChip(
                        label = "ED25519",
                        description = "Recommended",
                        selected = selectedType == "ED25519",
                        onClick = { selectedType = "ED25519" },
                        modifier = Modifier.weight(1f)
                    )
                    SshTypeChip(
                        label = "RSA",
                        description = "Legacy",
                        selected = selectedType == "RSA",
                        onClick = { selectedType = "RSA" },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    placeholder = { Text("user@host") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90,
                        cursorColor = Sakura90
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(name, selectedType, comment) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Sakura90),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SshTypeChip(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Sakura90.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.3f),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, Sakura90)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Sakura90 else colors.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Sakura90.copy(alpha = 0.7f) else colors.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun ImportSshKeyDialog(
    onDismiss: () -> Unit,
    onImport: (name: String, privateKey: String, publicKey: String?, passphrase: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var privateKey by rememberSaveable { mutableStateOf("") }
    var publicKey by rememberSaveable { mutableStateOf("") }
    var passphrase by rememberSaveable { mutableStateOf("") }
    var showPrivateKey by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Sakura90.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Sakura90,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Import SSH Key",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Key Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90,
                        cursorColor = Sakura90
                    )
                )

                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text("Private Key") },
                    placeholder = { Text("Paste your private key here...") },
                    trailingIcon = {
                        IconButton(onClick = { showPrivateKey = !showPrivateKey }) {
                            Icon(
                                if (showPrivateKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPrivateKey) "Hide" else "Show"
                            )
                        }
                    },
                    visualTransformation = if (showPrivateKey) VisualTransformation.None else PasswordVisualTransformation(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90,
                        cursorColor = Sakura90
                    )
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase (if encrypted)") },
                    trailingIcon = {
                        IconButton(onClick = { showPassphrase = !showPassphrase }) {
                            Icon(
                                if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassphrase) "Hide" else "Show"
                            )
                        }
                    },
                    visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90,
                        cursorColor = Sakura90
                    )
                )

                OutlinedTextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    label = { Text("Public Key (optional)") },
                    placeholder = { Text("ssh-rsa AAAA...") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90,
                        cursorColor = Sakura90
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImport(
                        name,
                        privateKey,
                        publicKey.ifBlank { null },
                        passphrase.ifBlank { null }
                    )
                },
                enabled = name.isNotBlank() && privateKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Sakura90),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SecuritySettingsSection(
    isBiometricEnabled: Boolean,
    isDecryptionUnlocked: Boolean,
    onToggleBiometric: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLock: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3).copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF2196F3).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Security Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleBiometric() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isBiometricEnabled) Color(0xFF4CAF50) else colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Biometric Unlock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isBiometricEnabled) "Enabled" else "Use fingerprint to unlock",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBiometricEnabled) Color(0xFF4CAF50).copy(alpha = 0.15f) else colors.surfaceVariant
                    ) {
                        Text(
                            text = if (isBiometricEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBiometricEnabled) Color(0xFF4CAF50) else colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onExport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            contentColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Export")
                    }

                    FilledTonalButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.15f),
                            contentColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                }

                if (isDecryptionUnlocked) {
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.1f))
                    
                    Button(
                        onClick = onLock,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error.copy(alpha = 0.1f),
                            contentColor = colors.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Lock Vault")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCredentialsDialog(
    viewModel: CredentialsStoreViewModel,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Export Credentials",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Exporting...")
                    }
                } else if (exportedData != null) {
                    Text(
                        text = "Your credentials have been exported. Copy the data below to save it securely:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${exportedData.take(50)}...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(exportedData))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied to clipboard!")
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ImportCredentialsDialog(
    viewModel: CredentialsStoreViewModel,
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
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Import Credentials",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste your exported credentials data below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = importData,
                    onValueChange = { importData = it },
                    label = { Text("Credentials Data") },
                    placeholder = { Text("Paste JSON data here...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    viewModel.importCredentials(importData)
                    scope.launch {
                        snackbarHostState.showSnackbar("Credentials imported successfully!")
                        onDismiss()
                    }
                },
                enabled = importData.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun generateSshKeyPair(type: String, comment: String = ""): Pair<String, String> {
    Log.d("SSH_KEY", "Starting SSH key generation, type: $type, comment: $comment")
    return try {
        when (type) {
            "RSA" -> {
                Log.d("SSH_KEY", "Generating RSA key pair...")
                generateRsaKeyPair(comment)
            }
            else -> {
                Log.d("SSH_KEY", "Generating Ed25519 key pair...")
                generateEd25519KeyPair(comment)
            }
        }.also { result ->
            Log.d("SSH_KEY", "Key generation successful. PublicKey length: ${result.first.length}, PrivateKey length: ${result.second.length}")
        }
    } catch (e: Exception) {
        Log.e("SSH_KEY", "Key generation failed: ${e.javaClass.simpleName}: ${e.message}", e)
        Pair("", "")
    }
}

private fun generateRsaKeyPair(comment: String = ""): Pair<String, String> {
    Log.d("SSH_KEY", "generateRsaKeyPair: Starting RSA key generation, comment: $comment")
    return try {
        val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
        Log.d("SSH_KEY", "generateRsaKeyPair: Initializing RSA 4096-bit key pair generator")
        keyPairGenerator.initialize(4096)
        Log.d("SSH_KEY", "generateRsaKeyPair: Generating RSA key pair...")
        val keyPair = keyPairGenerator.generateKeyPair()
        Log.d("SSH_KEY", "generateRsaKeyPair: RSA key pair generated")

        val publicKey = keyPair.public as java.security.interfaces.RSAPublicKey
        val publicKeyEncoded = encodeRsaPublicKey(publicKey, comment)
        Log.d("SSH_KEY", "generateRsaKeyPair: Public key encoded: ${publicKeyEncoded.take(50)}...")

        val privateKey = encodeRsaPrivateKey(keyPair.private as java.security.interfaces.RSAPrivateKey, comment)
        Log.d("SSH_KEY", "generateRsaKeyPair: Private key encoded, length: ${privateKey.length}")

        return Pair(publicKeyEncoded, privateKey)
    } catch (e: Exception) {
        Log.e("SSH_KEY", "generateRsaKeyPair: Failed - ${e.javaClass.simpleName}: ${e.message}", e)
        throw e
    }
}

private fun encodeRsaPublicKey(publicKey: java.security.interfaces.RSAPublicKey, comment: String = ""): String {
    val byteStream = java.io.ByteArrayOutputStream()
    val dos = java.io.DataOutputStream(byteStream)

    dos.writeInt(7)
    dos.write("ssh-rsa".toByteArray())

    val exponent = publicKey.publicExponent
    val modulus = publicKey.modulus

    var exponentBytes = exponent.toByteArray()
    if (exponentBytes.isNotEmpty() && exponentBytes[0] == 0x00.toByte()) {
        exponentBytes = exponentBytes.copyOfRange(1, exponentBytes.size)
    }

    var modulusBytes = modulus.toByteArray()
    if (modulusBytes.isNotEmpty() && modulusBytes[0] == 0x00.toByte()) {
        modulusBytes = modulusBytes.copyOfRange(1, modulusBytes.size)
    }

    dos.writeInt(exponentBytes.size)
    dos.write(exponentBytes)

    dos.writeInt(modulusBytes.size)
    dos.write(modulusBytes)

    val base64Key = java.util.Base64.getEncoder().encodeToString(byteStream.toByteArray())
    return if (comment.isNotBlank()) "ssh-rsa $base64Key $comment" else "ssh-rsa $base64Key"
}

private fun encodeRsaPrivateKey(privateKey: java.security.interfaces.RSAPrivateKey, comment: String = ""): String {
    val pkcs8Encoded = privateKey.encoded
    val base64 = java.util.Base64.getMimeEncoder(64, "\n".toByteArray())
    val keyContent = base64.encodeToString(pkcs8Encoded)
    return "-----BEGIN PRIVATE KEY-----\n$keyContent\n-----END PRIVATE KEY-----"
}

private fun generateEd25519KeyPair(comment: String = ""): Pair<String, String> {
    Log.d("SSH_KEY", "generateEd25519KeyPair: Starting Ed25519 key generation with BouncyCastle, comment: $comment")
    return try {
        java.security.Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        Log.d("SSH_KEY", "generateEd25519KeyPair: BC provider registered")

        val secureRandom = java.security.SecureRandom()

        val keyGen = org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator()
        keyGen.init(org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters(secureRandom))
        Log.d("SSH_KEY", "generateEd25519KeyPair: KeyPairGenerator initialized")

        val keyPair = keyGen.generateKeyPair()
        Log.d("SSH_KEY", "generateEd25519KeyPair: Key pair generated")

        val publicKeyParams = keyPair.public as org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
        val privateKeyParams = keyPair.private as org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters

        val publicKeyEncoded = encodeEd25519PublicKeyBC(publicKeyParams, comment)
        Log.d("SSH_KEY", "generateEd25519KeyPair: Public key encoded: ${publicKeyEncoded.take(50)}...")

        val privateKeyEncoded = encodeEd25519PrivateKeyBC(privateKeyParams, comment)
        Log.d("SSH_KEY", "generateEd25519KeyPair: Private key encoded, length: ${privateKeyEncoded.length}")

        Pair(publicKeyEncoded, privateKeyEncoded)
    } catch (e: Exception) {
        Log.e("SSH_KEY", "generateEd25519KeyPair: Failed - ${e.javaClass.simpleName}: ${e.message}", e)
        throw e
    }
}

private fun encodeEd25519PublicKeyBC(publicKey: org.bouncycastle.crypto.params.Ed25519PublicKeyParameters, comment: String = ""): String {
    val byteStream = java.io.ByteArrayOutputStream()
    val dos = java.io.DataOutputStream(byteStream)

    dos.writeInt(11)
    dos.write("ssh-ed25519".toByteArray())

    val keyBytes = publicKey.encoded
    dos.writeInt(keyBytes.size)
    dos.write(keyBytes)

    val base64Key = java.util.Base64.getEncoder().encodeToString(byteStream.toByteArray())
    return if (comment.isNotBlank()) "ssh-ed25519 $base64Key $comment" else "ssh-ed25519 $base64Key"
}

private fun encodeEd25519PrivateKeyBC(privateKey: org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters, comment: String = ""): String {
    val byteStream = java.io.ByteArrayOutputStream()
    val dos = java.io.DataOutputStream(byteStream)

    val privateKeyBytes = privateKey.encoded
    dos.writeInt(32)
    dos.write(privateKeyBytes.copyOfRange(0, 32))

    val publicKey = privateKey.generatePublicKey() as org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
    val publicKeyBytes = publicKey.encoded
    dos.writeInt(32)
    dos.write(publicKeyBytes)

    val base64 = java.util.Base64.getMimeEncoder(64, "\n".toByteArray())
    val keyContent = base64.encodeToString(byteStream.toByteArray())
    return "-----BEGIN OPENSSH PRIVATE KEY-----\n$keyContent\n-----END OPENSSH PRIVATE KEY-----"
}

private fun encodeEd25519PublicKey(publicKey: java.security.PublicKey): String {
    Log.d("SSH_KEY", "encodeEd25519PublicKey: Starting encoding, key class: ${publicKey.javaClass.name}")
    return try {
        val byteStream = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(byteStream)
        
        Log.d("SSH_KEY", "encodeEd25519PublicKey: Writing ssh-ed25519 header")
        dos.writeInt(11)
        dos.write("ssh-ed25519".toByteArray())
        
        val encoded = publicKey.encoded
        Log.d("SSH_KEY", "encodeEd25519PublicKey: Public key encoded length: ${encoded.size}")
        val keyBytes = extractEd25519PublicKeyBytes(encoded)
        Log.d("SSH_KEY", "encodeEd25519PublicKey: Extracted key bytes length: ${keyBytes.size}")
        
        dos.writeInt(keyBytes.size)
        dos.write(keyBytes)
        
        val result = "ssh-ed25519 ${java.util.Base64.getEncoder().encodeToString(byteStream.toByteArray())}"
        Log.d("SSH_KEY", "encodeEd25519PublicKey: Final result length: ${result.length}")
        result
    } catch (e: Exception) {
        Log.e("SSH_KEY", "encodeEd25519PublicKey: Failed - ${e.javaClass.simpleName}: ${e.message}", e)
        throw e
    }
}

private fun extractEd25519PublicKeyBytes(encoded: ByteArray): ByteArray {
    Log.d("SSH_KEY", "extractEd25519PublicKeyBytes: Input length: ${encoded.size}")
    var idx = 0
    if (encoded[idx++].toInt() and 0xFF != 0x30) {
        Log.e("SSH_KEY", "extractEd25519PublicKeyBytes: Invalid key - expected 0x30 at index 0")
        throw IllegalArgumentException("Invalid Ed25519 public key")
    }
    idx++
    
    if (encoded[idx++].toInt() and 0xFF != 0x30) {
        Log.e("SSH_KEY", "extractEd25519PublicKeyBytes: Invalid key - expected 0x30 at index 2")
        throw IllegalArgumentException("Invalid Ed25519 public key")
    }
    idx++
    
    if (encoded[idx++].toInt() and 0xFF != 0x06) {
        Log.e("SSH_KEY", "extractEd25519PublicKeyBytes: Invalid key - expected 0x06 at index 4")
        throw IllegalArgumentException("Invalid Ed25519 public key")
    }
    val oidLen = encoded[idx++].toInt() and 0xFF
    Log.d("SSH_KEY", "extractEd25519PublicKeyBytes: OID length: $oidLen")
    idx += oidLen
    
    if (encoded[idx++].toInt() and 0xFF != 0x03) {
        Log.e("SSH_KEY", "extractEd25519PublicKeyBytes: Invalid key - expected 0x03 at index ${idx-1}")
        throw IllegalArgumentException("Invalid Ed25519 public key")
    }
    idx++
    
    if (encoded[idx++].toInt() and 0xFF != 0x00) {
        Log.e("SSH_KEY", "extractEd25519PublicKeyBytes: Invalid key - expected 0x00 at index ${idx-1}")
        throw IllegalArgumentException("Invalid Ed25519 public key")
    }
    
    val result = encoded.copyOfRange(idx, encoded.size)
    Log.d("SSH_KEY", "extractEd25519PublicKeyBytes: Extracted ${result.size} bytes")
    return result
}

private fun encodeEd25519PrivateKey(privateKey: java.security.PrivateKey): String {
    val pkcs8Encoded = privateKey.encoded
    val base64 = java.util.Base64.getMimeEncoder(64, "\n".toByteArray())
    val keyContent = base64.encodeToString(pkcs8Encoded)
    return "-----BEGIN PRIVATE KEY-----\n$keyContent\n-----END PRIVATE KEY-----"
}

private fun calculateFingerprint(publicKey: String): String {
    Log.d("SSH_KEY", "Calculating fingerprint for publicKey: ${publicKey.take(50)}...")
    return try {
        val keyPart = publicKey.substringAfter(" ").substringBefore(" ")
        Log.d("SSH_KEY", "Key part for fingerprint: ${keyPart.take(20)}...")
        val keyBytes = java.util.Base64.getDecoder().decode(keyPart)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(keyBytes)
        val fingerprint = "SHA256:${java.util.Base64.getEncoder().withoutPadding().encodeToString(digest)}"
        Log.d("SSH_KEY", "Fingerprint calculated: $fingerprint")
        fingerprint
    } catch (e: Exception) {
        Log.e("SSH_KEY", "Fingerprint calculation failed: ${e.javaClass.simpleName}: ${e.message}", e)
        "unknown"
    }
}

private fun detectSshKeyType(privateKey: String): String {
    return when {
        privateKey.contains("BEGIN RSA PRIVATE KEY") -> "RSA"
        privateKey.contains("BEGIN OPENSSH PRIVATE KEY") -> "ED25519"
        else -> "ED25519"
    }
}
