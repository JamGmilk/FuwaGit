package jamgmilk.obsigit.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import jamgmilk.obsigit.credential.HttpsCredential
import jamgmilk.obsigit.credential.SshKeyInfo
import jamgmilk.obsigit.credential.SshKeyType
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras

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
    viewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val httpsCredentials by viewModel.httpsCredentials.collectAsState()
    val sshKeys by viewModel.sshKeys.collectAsState()
    val keyStoreAvailable by viewModel.keyStoreAvailable.collectAsState()
    
    var selectedTab by remember { mutableStateOf<CredentialsTab>(CredentialsTab.Https) }
    var dialogState by remember { mutableStateOf<CredentialDialogState>(CredentialDialogState.None) }
    var showDeleteConfirm by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    
    val clipboardManager = LocalClipboardManager.current
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
                }
                Text(
                    text = "Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    text = "HTTPS",
                    selected = selectedTab is CredentialsTab.Https,
                    onClick = { selectedTab = CredentialsTab.Https },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "SSH Keys",
                    selected = selectedTab is CredentialsTab.Ssh,
                    onClick = { selectedTab = CredentialsTab.Ssh },
                    modifier = Modifier.weight(1f)
                )
            }
            
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
                            credentials = httpsCredentials,
                            keyStoreAvailable = keyStoreAvailable,
                            onAdd = { dialogState = CredentialDialogState.AddHttps() },
                            onEdit = { dialogState = CredentialDialogState.EditHttps(it) },
                            onDelete = { showDeleteConfirm = it.id to false },
                            onCopyPassword = { 
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    is CredentialsTab.Ssh -> {
                        SshKeysContent(
                            keys = sshKeys,
                            onGenerate = { dialogState = CredentialDialogState.AddSshKey() },
                            onImport = { dialogState = CredentialDialogState.ImportSshKey() },
                            onView = { dialogState = CredentialDialogState.ShowSshKey(it) },
                            onDelete = { showDeleteConfirm = it.id to true },
                            onCopyPublicKey = {
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "Public key copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
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
                    Toast.makeText(context, "Public key exported", Toast.LENGTH_SHORT).show()
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
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    
    ElevatedCard(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else uiColors.cardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) colors.primaryContainer else uiColors.cardContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) colors.primary else colors.onSurface
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
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HTTPS Credentials", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (keyStoreAvailable) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "KeyStore Available",
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(onClick = onAdd) {
                            Icon(Icons.Default.Add, contentDescription = "Add Credential")
                        }
                    }
                }
                
                if (!keyStoreAvailable) {
                    Text(
                        "KeyStore not available. Credentials may not be securely stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
                
                if (credentials.isEmpty()) {
                    Text(
                        "No HTTPS credentials saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                } else {
                    credentials.forEachIndexed { index, credential ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
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
        
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security Info", fontWeight = FontWeight.Bold, color = colors.primary)
                ListItem(
                    headlineContent = { Text("Storage") },
                    supportingContent = { Text("Credentials are encrypted using Android KeyStore with AES-256-GCM encryption") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) }
                )
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
    var showPassword by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { Text(credential.host) },
        supportingContent = {
            Column {
                Text("Username: ${credential.username}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Password: ${if (showPassword) credential.password else "••••••••"}",
                        fontFamily = if (showPassword) FontFamily.Monospace else FontFamily.Default
                    )
                    IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide" else "Show",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
        trailingContent = {
            Row {
                IconButton(onClick = onCopyPassword) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
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
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SSH Keys", fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = onGenerate) {
                            Icon(Icons.Default.Add, contentDescription = "Generate Key")
                        }
                        IconButton(onClick = onImport) {
                            Icon(Icons.Default.Key, contentDescription = "Import Key")
                        }
                    }
                }
                
                if (keys.isEmpty()) {
                    Text(
                        "No SSH keys saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                } else {
                    keys.forEachIndexed { index, key ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
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
        
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SSH Key Info", fontWeight = FontWeight.Bold, color = colors.primary)
                ListItem(
                    headlineContent = { Text("Supported Types") },
                    supportingContent = { Text("RSA (4096-bit), Ed25519") },
                    leadingContent = { Icon(Icons.Default.Key, contentDescription = null) }
                )
                ListItem(
                    headlineContent = { Text("Storage") },
                    supportingContent = { Text("Private keys are stored in app-private storage with restricted access") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
                )
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
    ListItem(
        headlineContent = { Text(key.name) },
        supportingContent = {
            Column {
                Text("${key.type.displayName} ${if (key.hasPrivateKey) "• Private key stored" else "• Public key only"}")
                Text(
                    "Fingerprint: ${key.fingerprint.take(24)}...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        leadingContent = { Icon(Icons.Default.Key, contentDescription = null) },
        trailingContent = {
            Row {
                IconButton(onClick = onCopyPublicKey) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Public Key")
                }
                IconButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, contentDescription = "View")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    placeholder = { Text("github.com") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(host, username, password) },
                enabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate SSH Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Key Name") },
                    placeholder = { Text("My Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(name, selectedType, comment) },
                enabled = name.isNotBlank()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import SSH Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Key Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    label = { Text("Public Key (optional)") },
                    placeholder = { Text("ssh-rsa AAAA...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
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
                enabled = name.isNotBlank() && privateKey.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
private fun SshKeyDetailDialog(
    keyInfo: SshKeyInfo,
    onDismiss: () -> Unit,
    onExportPublicKey: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(keyInfo.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ListItem(
                    headlineContent = { Text("Type") },
                    supportingContent = { Text(keyInfo.type.displayName) }
                )
                ListItem(
                    headlineContent = { Text("Private Key") },
                    supportingContent = { Text(if (keyInfo.hasPrivateKey) "Stored" else "Not available") }
                )
                if (keyInfo.comment.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("Comment") },
                        supportingContent = { Text(keyInfo.comment) }
                    )
                }
                ListItem(
                    headlineContent = { Text("Fingerprint") },
                    supportingContent = { 
                        Text(
                            keyInfo.fingerprint,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    }
                )
                ListItem(
                    headlineContent = { Text("Public Key") },
                    supportingContent = { 
                        Text(
                            keyInfo.publicKey.take(60) + "...",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onExportPublicKey) {
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

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun CredentialsScreenPreview() {
    val viewModel = remember { AppViewModel() }
    ObsiGitTheme {
        CredentialsScreen(
            viewModel = viewModel,
            onBack = {}
        )
    }
}
