package jamgmilk.fuwagit.ui.screen.credentials

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.BuildConfig
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.core.util.validatePrivateKey

@Composable
fun AddHttpsCredentialDialog(
    onDismiss: () -> Unit,
    onAdd: (host: String, username: String, password: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var host by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

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
        title = {
            Text(text = stringResource(R.string.credentials_add_https_credential), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.credentials_host_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_host_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.credentials_username_label)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.credentials_password_pat_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(host, username, password) },
                enabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_save))
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

@Composable
fun GenerateSshKeyDialog(
    onDismiss: () -> Unit,
    onGenerate: (name: String, type: String, comment: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("Ed25519") }
    var comment by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

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
        title = {
            Text(text = stringResource(R.string.credentials_generate_ssh_key), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.credentials_key_name_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_key_name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, focusedLabelColor = colors.tertiary, cursorColor = colors.tertiary)
                )

                Text(text = stringResource(R.string.credentials_key_type_label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SshTypeChip(
                        label = stringResource(R.string.credentials_ed25519_label),
                        description = stringResource(R.string.credentials_ed25519_description),
                        selected = selectedType == "Ed25519",
                        onClick = { selectedType = "Ed25519" },
                        modifier = Modifier.weight(1f)
                    )
                    SshTypeChip(
                        label = stringResource(R.string.credentials_rsa_label),
                        description = stringResource(R.string.credentials_rsa_description),
                        selected = selectedType == "RSA",
                        onClick = { selectedType = "RSA" },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.credentials_comment_optional_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_comment_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, focusedLabelColor = colors.tertiary, cursorColor = colors.tertiary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(name, selectedType, comment) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_generate))
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

@Composable
fun SshTypeChip(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.height(52.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.tertiary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.3f),
        border = if (selected) BorderStroke(2.dp, colors.tertiary) else BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.tertiary else colors.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.tertiary.copy(alpha = 0.7f) else colors.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun ImportSshKeyDialog(
    onDismiss: () -> Unit,
    onImport: (name: String, privateKey: String, publicKey: String?, passphrase: String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var privateKey by rememberSaveable { mutableStateOf("") }
    var publicKey by rememberSaveable { mutableStateOf("") }
    var passphrase by rememberSaveable { mutableStateOf("") }
    var showPrivateKey by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.size(48.dp).background(colors.tertiary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Text(text = stringResource(R.string.credentials_import_ssh_key), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.credentials_key_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, focusedLabelColor = colors.tertiary, cursorColor = colors.tertiary)
                )

                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text(stringResource(R.string.credentials_private_key_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_private_key_placeholder)) },
                    trailingIcon = {
                        IconButton(onClick = { showPrivateKey = !showPrivateKey }) {
                            Icon(if (showPrivateKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPrivateKey) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                        }
                    },
                    visualTransformation = if (showPrivateKey) VisualTransformation.None else PasswordVisualTransformation(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, focusedLabelColor = colors.tertiary, cursorColor = colors.tertiary)
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.credentials_passphrase_if_encrypted)) },
                    trailingIcon = {
                        IconButton(onClick = { showPassphrase = !showPassphrase }) {
                            Icon(if (showPassphrase) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassphrase) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                        }
                    },
                    visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, focusedLabelColor = colors.tertiary, cursorColor = colors.tertiary)
                )

                OutlinedTextField(
                    value = publicKey,
                    onValueChange = { publicKey = it },
                    label = { Text(stringResource(R.string.credentials_public_key_optional)) },
                    placeholder = { Text(stringResource(R.string.credentials_public_key_placeholder)) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.tertiary, focusedLabelColor = colors.tertiary, cursorColor = colors.tertiary)
                )
                
                // Validation error message
                validationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validationError = null
                    try {
                        val (isValid, keyType) = validatePrivateKey(privateKey)
                        if (isValid) {
                            if (BuildConfig.DEBUG) android.util.Log.d("ImportSshKeyDialog", "Key validation successful, type: $keyType")
                            onImport(name, privateKey, publicKey.ifBlank { null }, passphrase.ifBlank { null })
                        }
                    } catch (e: IllegalArgumentException) {
                        validationError = e.message ?: context.getString(R.string.credentials_invalid_private_key, "")
                    } catch (e: Exception) {
                        validationError = context.getString(R.string.credentials_invalid_private_key, e.message ?: "")
                    }
                },
                enabled = name.isNotBlank() && privateKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_import))
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
