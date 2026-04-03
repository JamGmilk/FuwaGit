package jamgmilk.fuwagit.ui.screen.credentials

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.theme.Sakura80
import jamgmilk.fuwagit.ui.theme.Sakura90
import kotlinx.coroutines.launch

@Composable
fun SetupPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String, hint: String?) -> Unit,
    error: String? = null,
    isLoading: Boolean = false
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val passwordMatchError = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
        "Passwords do not match"
    } else null

    val passwordLengthError = if (password.isNotEmpty() && password.length < 8) {
        "At least 8 characters"
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Set Master Password",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create a master password to protect your sensitive credentials. You'll need this password to view passwords and private keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("At least 8 characters") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = passwordLengthError != null,
                    supportingText = passwordLengthError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = passwordMatchError != null,
                    supportingText = passwordMatchError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Password Hint (optional)") },
                    placeholder = { Text("Help you remember") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password, hint.ifBlank { null }) },
                enabled = !isLoading && password.length >= 8 && password == confirmPassword
            ) {
                Text(if (isLoading) "Setting..." else "Set Password")
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
fun UnlockDialog(
    onDismiss: () -> Unit,
    onUnlock: (password: String) -> Unit,
    biometricEnabled: Boolean = false,
    onUnlockWithBiometric: () -> Unit = {},
    passwordHint: String? = null,
    error: String? = null,
    isLoading: Boolean = false
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (biometricEnabled) {
            onUnlockWithBiometric()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unlock Credentials",
                    style = MaterialTheme.typography.titleLarge
                )
                if (biometricEnabled) {
                    IconButton(onClick = onUnlockWithBiometric) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Unlock with biometric",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your master password to access sensitive credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Master Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                passwordHint?.let { hint ->
                    Text(
                        text = "Hint: $hint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUnlock(password) },
                enabled = !isLoading && password.isNotBlank()
            ) {
                Text(if (isLoading) "Unlocking..." else "Unlock")
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
fun AddHttpsCredentialDialog(
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
                            Icons.Default.Lock,
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
fun ChangeMasterPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPassword: String, newPassword: String, confirmPassword: String, hint: String?) -> Unit,
    passwordHint: String? = null,
    error: String? = null,
    isLoading: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val passwordMatchError = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
        "Passwords do not match"
    } else null

    val passwordLengthError = if (newPassword.isNotEmpty() && newPassword.length < 8) {
        "At least 8 characters"
    } else null

    val isFormValid = oldPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmPassword

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
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = Sakura80,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Change Master Password",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your current password and set a new one. This will also disable biometric unlock if enabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOldPassword = !showOldPassword }) {
                            Icon(
                                if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showOldPassword) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = colors.error) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colors.outline.copy(alpha = 0.2f)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    placeholder = { Text("At least 8 characters") },
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showNewPassword) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = passwordLengthError != null,
                    supportingText = passwordLengthError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = passwordMatchError != null,
                    supportingText = passwordMatchError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Password Hint (optional)") },
                    placeholder = { Text("Help you remember") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                passwordHint?.let { existingHint ->
                    if (existingHint.isNotBlank() && hint.isBlank()) {
                        Text(
                            text = "Current hint: $existingHint",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPassword, newPassword, confirmPassword, hint.ifBlank { null }) },
                enabled = !isLoading && isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = Sakura80),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
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
                Text("Change Password")
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
fun SetupMasterPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String, confirmPassword: String, hint: String?) -> Unit,
    error: String? = null,
    isLoading: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val passwordMatchError = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
        "Passwords do not match"
    } else null

    val passwordLengthError = if (password.isNotEmpty() && password.length < 8) {
        "At least 8 characters"
    } else null

    val isFormValid = password.length >= 8 && password == confirmPassword

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
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Sakura80,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Set Master Password",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Create a master password to protect your sensitive credentials. You'll need this password to view passwords and private keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("At least 8 characters") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = passwordLengthError != null,
                    supportingText = passwordLengthError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = passwordMatchError != null,
                    supportingText = passwordMatchError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Password Hint (optional)") },
                    placeholder = { Text("Help you remember") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80,
                        cursorColor = Sakura80
                    )
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password, confirmPassword, hint.ifBlank { null }) },
                enabled = !isLoading && isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = Sakura80),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
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
                Text("Set Password")
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
fun SetupMasterPasswordContent(
    onConfirm: (password: String, hint: String?) -> Unit,
    error: String? = null,
    isLoading: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    val passwordMatchError = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
        "Passwords do not match"
    } else null
    
    val passwordLengthError = if (password.isNotEmpty() && password.length < 8) {
        "At least 8 characters"
    } else null
    
    val isFormValid = password.length >= 8 && password == confirmPassword
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    colors.primary.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Create a master password to protect your sensitive credentials. You'll need this password to view passwords and private keys.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(Modifier.height(20.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("At least 8 characters") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide" else "Show"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            isError = passwordLengthError != null,
            supportingText = passwordLengthError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showConfirmPassword) "Hide" else "Show"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            isError = passwordMatchError != null,
            supportingText = passwordMatchError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it },
            label = { Text("Password Hint (optional)") },
            placeholder = { Text("Help you remember") },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { onConfirm(password, hint.ifBlank { null }) },
            enabled = !isLoading && isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Setting...")
            } else {
                Text("Set Password", fontSize = 16.sp)
            }
        }
    }
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
                        label = "Ed25519",
                        description = "Recommended",
                        selected = selectedType == "Ed25519",
                        onClick = { selectedType = "Ed25519" },
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
fun ImportSshKeyDialog(
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
fun HttpsCredentialInfoDialog(
    credential: HttpsCredential,
    viewModel: CredentialStoreViewModel,
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
                InfoRow(label = "Created", value = formatTimestamp(credential.createdAt))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                InfoRow(label = "Updated", value = formatTimestamp(credential.updatedAt))
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
fun SshKeyInfoDialog(
    key: SshKey,
    viewModel: CredentialStoreViewModel,
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
                    value = key.publicKey,
                    isMonospace = true,
                    isSensitive = false,
                    isRevealed = true,
                    showToggle = false,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(key.publicKey))
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
                InfoRow(label = "Created", value = formatTimestamp(key.createdAt))
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
fun ExportCredentialsDialog(
    viewModel: CredentialStoreViewModel,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                        CircularProgressIndicator(
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Importing will replace existing credentials",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
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
                    CircularProgressIndicator(
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

