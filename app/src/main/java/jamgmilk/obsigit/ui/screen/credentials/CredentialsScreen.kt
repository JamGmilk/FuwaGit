package jamgmilk.obsigit.ui.screen.credentials

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.obsigit.credential.SshKeyInfo
import jamgmilk.obsigit.credential.SshKeyType
import jamgmilk.obsigit.domain.model.HttpsCredential
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import jamgmilk.obsigit.ui.theme.Sakura50
import jamgmilk.obsigit.ui.theme.Sakura80
import jamgmilk.obsigit.ui.theme.Sakura90
import jamgmilk.obsigit.ui.components.SubSettingsTemplate
import kotlinx.coroutines.launch

sealed class CredentialsTab {
    data object Https : CredentialsTab()
    data object Ssh : CredentialsTab()
}

sealed class CredentialDialogState {
    data object None : CredentialDialogState()
    data class AddHttps(val placeholder: Unit = Unit) : CredentialDialogState()
    data class EditHttps(val credential: HttpsCredential) : CredentialDialogState()
    data class AddSshKey(val placeholder: Unit = Unit) : CredentialDialogState()
    data class ImportSshKey(val placeholder: Unit = Unit) : CredentialDialogState()
    data class ShowSshKey(val key: SshKeyInfo) : CredentialDialogState()
}

@Composable
fun CredentialsScreen(
    viewModel: CredentialsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf<CredentialsTab>(CredentialsTab.Https) }
    var dialogState by remember { mutableStateOf<CredentialDialogState>(CredentialDialogState.None) }
    var showDeleteConfirm by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val clipboardManager = LocalClipboardManager.current

    SubSettingsTemplate(
        title = "Credentials",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) {
        CredentialsTabSelector(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
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
                    HttpsCredentialsContent(
                        credentials = uiState.httpsCredentials,
                        keyStoreAvailable = uiState.keyStoreAvailable,
                        onAdd = { dialogState = CredentialDialogState.AddHttps() },
                        onEdit = { dialogState = CredentialDialogState.EditHttps(it) },
                        onDelete = { showDeleteConfirm = it.id to false },
                        onCopyPassword = {
                            clipboardManager.setText(AnnotatedString(it))
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Password copied",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
                is CredentialsTab.Ssh -> {
                    SshKeysContent(
                        keys = uiState.sshKeys,
                        onGenerate = { dialogState = CredentialDialogState.AddSshKey() },
                        onImport = { dialogState = CredentialDialogState.ImportSshKey() },
                        onView = { dialogState = CredentialDialogState.ShowSshKey(it) },
                        onDelete = { showDeleteConfirm = it.id to true },
                        onCopyPublicKey = {
                            clipboardManager.setText(AnnotatedString(it))
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Public key copied",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    when (val state = dialogState) {
        is CredentialDialogState.AddHttps -> {
            HttpsCredentialDialog(
                title = "Add HTTPS Credential",
                onDismiss = { dialogState = CredentialDialogState.None },
                onSave = { host, username, password ->
                    viewModel.saveHttpsCredential(
                        HttpsCredential(
                            id = "",
                            host = host,
                            username = username,
                            password = password
                        )
                    )
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.EditHttps -> {
            HttpsCredentialDialog(
                title = "Edit HTTPS Credential",
                initialHost = state.credential.host,
                initialUsername = state.credential.username,
                initialPassword = state.credential.password,
                onDismiss = { dialogState = CredentialDialogState.None },
                onSave = { host, username, password ->
                    viewModel.updateHttpsCredential(
                        state.credential.copy(
                            host = host,
                            username = username,
                            password = password
                        )
                    )
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.AddSshKey -> {
            GenerateSshKeyDialog(
                onDismiss = { dialogState = CredentialDialogState.None },
                onGenerate = { name, type, comment ->
                    viewModel.generateSshKey(name, type, comment)
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.ImportSshKey -> {
            ImportSshKeyDialog(
                onDismiss = { dialogState = CredentialDialogState.None },
                onImport = { name, privateKey, publicKey, passphrase ->
                    viewModel.importSshKey(name, privateKey, publicKey, passphrase)
                    dialogState = CredentialDialogState.None
                }
            )
        }
        is CredentialDialogState.ShowSshKey -> {
            SshKeyDetailDialog(
                keyInfo = state.key,
                onDismiss = { dialogState = CredentialDialogState.None },
                onExportPublicKey = {
                    viewModel.exportSshPublicKey(state.key.id)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Public key exported",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
        is CredentialDialogState.None -> {}
    }

    showDeleteConfirm?.let { (id, isSsh) ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Confirm Delete") },
            text = { Text(if (isSsh) "Are you sure you want to delete this SSH key?" else "Are you sure you want to delete this credential?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSsh) {
                            viewModel.deleteSshKey(id)
                        } else {
                            viewModel.deleteHttpsCredential(id)
                        }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CredentialsTabSelector(
    selectedTab: CredentialsTab,
    onTabSelected: (CredentialsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabButton(
            text = "HTTPS",
            icon = Icons.Default.Link,
            selected = selectedTab is CredentialsTab.Https,
            onClick = { onTabSelected(CredentialsTab.Https) },
            modifier = Modifier.weight(1f)
        )
        TabButton(
            text = "SSH Keys",
            icon = Icons.Default.Key,
            selected = selectedTab is CredentialsTab.Ssh,
            onClick = { onTabSelected(CredentialsTab.Ssh) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val accentColor = if (text == "HTTPS") Sakura80 else Sakura90

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.15f) else uiColors.cardContainer,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, uiColors.cardBorder)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else colors.onSurface
            )
        }
    }
}

@Composable
private fun HttpsCredentialsContent(
    credentials: List<HttpsCredential>,
    keyStoreAvailable: Boolean,
    onAdd: () -> Unit,
    onEdit: (HttpsCredential) -> Unit,
    onDelete: (HttpsCredential) -> Unit,
    onCopyPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    Column(modifier = modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Sakura80.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Sakura80,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HTTPS Credentials",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Sakura80
                                )
                                Text(
                                    text = "${credentials.size} saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onAdd) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Credential",
                                tint = Sakura80
                            )
                        }
                    }
                }

                if (!keyStoreAvailable) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "KeyStore unavailable. Credentials may not be securely stored.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                    }
                }

                if (credentials.isEmpty()) {
                    EmptyStateMessage(
                        icon = Icons.Default.Lock,
                        message = "No HTTPS credentials saved",
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        credentials.forEachIndexed { index, credential ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(1.dp)
                                        .background(uiColors.cardBorder)
                                )
                            }
                            HttpsCredentialItem(
                                credential = credential,
                                onEdit = { onEdit(credential) },
                                onDelete = { onDelete(credential) },
                                onCopyPassword = { onCopyPassword(credential.password) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HttpsCredentialItem(
    credential: HttpsCredential,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyPassword: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Sakura80.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = Sakura80,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = credential.host,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onCopyPassword, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Password",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = colors.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Password,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (showPassword) credential.password else "••••••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (showPassword) FontFamily.Monospace else FontFamily.Default,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showPassword = !showPassword },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide" else "Show",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SshKeysContent(
    keys: List<SshKeyInfo>,
    onGenerate: () -> Unit,
    onImport: () -> Unit,
    onView: (SshKeyInfo) -> Unit,
    onDelete: (SshKeyInfo) -> Unit,
    onCopyPublicKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    Column(modifier = modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Sakura90.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = Sakura90,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SSH Keys",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Sakura90
                                )
                                Text(
                                    text = "${keys.size} saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = onGenerate) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Generate Key",
                                    tint = Sakura90
                                )
                            }
                            IconButton(onClick = onImport) {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = "Import Key",
                                    tint = Sakura90
                                )
                            }
                        }
                    }
                }

                if (keys.isEmpty()) {
                    EmptyStateMessage(
                        icon = Icons.Default.Key,
                        message = "No SSH keys saved",
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        keys.forEachIndexed { index, key ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(1.dp)
                                        .background(uiColors.cardBorder)
                                )
                            }
                            SshKeyItem(
                                key = key,
                                onView = { onView(key) },
                                onDelete = { onDelete(key) },
                                onCopyPublicKey = { onCopyPublicKey(key.publicKey) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SshKeyItem(
    key: SshKeyInfo,
    onView: () -> Unit,
    onDelete: () -> Unit,
    onCopyPublicKey: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Sakura90.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = Sakura90,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = key.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = if (key.hasPrivateKey) "Private stored" else "Public only",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (key.hasPrivateKey) Color(0xFF4CAF50) else colors.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onCopyPublicKey, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Public Key",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = colors.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Text(
                text = "Fingerprint: ${key.fingerprint.take(32)}...",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HttpsCredentialDialog(
    title: String,
    initialHost: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    onDismiss: () -> Unit,
    onSave: (host: String, username: String, password: String) -> Unit
) {
    var host by rememberSaveable { mutableStateOf(initialHost) }
    var username by rememberSaveable { mutableStateOf(initialUsername) }
    var password by rememberSaveable { mutableStateOf(initialPassword) }
    var showPassword by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("github.com") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80
                    )
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / PAT") },
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
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
                        focusedLabelColor = Sakura80
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(host, username, password) },
                enabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Sakura80),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateSshKeyDialog(
    onDismiss: () -> Unit,
    onGenerate: (name: String, type: SshKeyType, comment: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(SshKeyType.ED25519) }
    var comment by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate SSH Key", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        focusedLabelColor = Sakura90
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Key Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sakura90,
                            focusedLabelColor = Sakura90
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SshKeyType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
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
                        focusedLabelColor = Sakura90
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
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import SSH Key", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Key Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura90,
                        focusedLabelColor = Sakura90
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
                        focusedLabelColor = Sakura90
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
                        focusedLabelColor = Sakura90
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
                        focusedLabelColor = Sakura90
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
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SshKeyDetailDialog(
    keyInfo: SshKeyInfo,
    onDismiss: () -> Unit,
    onExportPublicKey: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = Sakura90,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(keyInfo.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Type", keyInfo.type.displayName)
                DetailRow("Private Key", if (keyInfo.hasPrivateKey) "Stored" else "Not available")
                if (keyInfo.comment.isNotBlank()) {
                    DetailRow("Comment", keyInfo.comment)
                }
                Column {
                    Text(
                        "Fingerprint",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        keyInfo.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(
                        "Public Key",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        keyInfo.publicKey.take(80) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onExportPublicKey,
                colors = ButtonDefaults.buttonColors(containerColor = Sakura90),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Copy Public Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
