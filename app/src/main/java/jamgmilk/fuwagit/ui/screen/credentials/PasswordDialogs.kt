package jamgmilk.fuwagit.ui.screen.credentials

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.R

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
        stringResource(R.string.credentials_passwords_do_not_match)
    } else null

    val passwordLengthError = if (password.isNotEmpty() && password.length < 6) {
        stringResource(R.string.credentials_at_least_6_characters)
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(R.string.credentials_setup_master_password),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.credentials_setup_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.credentials_password_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_password_placeholder)) },
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
                    label = { Text(stringResource(R.string.credentials_confirm_password_label)) },
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
                    label = { Text(stringResource(R.string.credentials_password_hint_optional)) },
                    placeholder = { Text(stringResource(R.string.credentials_password_hint_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password, hint.ifBlank { null }) },
                enabled = !isLoading && password.length >= 6 && password == confirmPassword
            ) {
                Text(if (isLoading) stringResource(R.string.credentials_setting) else stringResource(R.string.credentials_set_password))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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
                    text = stringResource(R.string.credentials_unlock_credentials),
                    style = MaterialTheme.typography.titleLarge
                )
                if (biometricEnabled) {
                    IconButton(onClick = onUnlockWithBiometric) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.credentials_unlock_with_biometric),
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
                    text = stringResource(R.string.credentials_unlock_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.credentials_master_password_label)) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide)
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
                        text = stringResource(R.string.credentials_hint_format, hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                error?.let { errorMsg ->
                    Text(
                        text = errorMsg,
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
                Text(if (isLoading) stringResource(R.string.credentials_unlocking) else stringResource(R.string.credentials_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
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
        stringResource(R.string.credentials_passwords_do_not_match)
    } else null

    val passwordLengthError = if (newPassword.isNotEmpty() && newPassword.length < 6) {
        stringResource(R.string.credentials_at_least_6_characters)
    } else null

    val isFormValid = oldPassword.isNotBlank() && newPassword.length >= 6 && newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.credentials_change_master_password),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.credentials_change_password_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(stringResource(R.string.credentials_current_password_label)) },
                    visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOldPassword = !showOldPassword }) {
                            Icon(
                                if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showOldPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = colors.error) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        cursorColor = colors.primary
                    )
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = colors.outline.copy(alpha = 0.2f))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.credentials_new_password_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_password_placeholder)) },
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showNewPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = passwordLengthError != null,
                    supportingText = passwordLengthError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        cursorColor = colors.primary
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.credentials_confirm_new_password_label)) },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = passwordMatchError != null,
                    supportingText = passwordMatchError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        cursorColor = colors.primary
                    )
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text(stringResource(R.string.credentials_password_hint_optional)) },
                    placeholder = { Text(stringResource(R.string.credentials_password_hint_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        cursorColor = colors.primary
                    )
                )

                passwordHint?.let { existingHint ->
                    if (existingHint.isNotBlank() && hint.isBlank()) {
                        Text(
                            text = stringResource(R.string.credentials_current_hint_format, existingHint),
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.credentials_change_password))
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
        stringResource(R.string.credentials_passwords_do_not_match)
    } else null

    val passwordLengthError = if (password.isNotEmpty() && password.length < 6) {
        stringResource(R.string.credentials_at_least_6_characters)
    } else null

    val isFormValid = password.length >= 6 && password == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            }
        },
        title = {
            Text(text = stringResource(R.string.credentials_setup_master_password_full), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.credentials_setup_full_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.credentials_password_label)) },
                    placeholder = { Text(stringResource(R.string.credentials_password_placeholder)) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = passwordLengthError != null,
                    supportingText = passwordLengthError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, focusedLabelColor = colors.primary, cursorColor = colors.primary)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.credentials_confirm_password_label)) },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = passwordMatchError != null,
                    supportingText = passwordMatchError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, focusedLabelColor = colors.primary, cursorColor = colors.primary)
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text(stringResource(R.string.credentials_password_hint_optional)) },
                    placeholder = { Text(stringResource(R.string.credentials_password_hint_placeholder)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, focusedLabelColor = colors.primary, cursorColor = colors.primary)
                )

                error?.let { errorMsg ->
                    Text(text = errorMsg, color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password, confirmPassword, hint.ifBlank { null }) },
                enabled = !isLoading && isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.credentials_set_password))
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
        stringResource(R.string.credentials_passwords_do_not_match)
    } else null

    val passwordLengthError = if (password.isNotEmpty() && password.length < 6) {
        stringResource(R.string.credentials_at_least_6_characters)
    } else null

    val isFormValid = password.length >= 6 && password == confirmPassword

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(72.dp).background(colors.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.credentials_setup_description),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.credentials_password_label)) },
            placeholder = { Text(stringResource(R.string.credentials_password_placeholder)) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            singleLine = true,
            isError = passwordLengthError != null,
            supportingText = passwordLengthError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.credentials_confirm_password_label)) },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showConfirmPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            singleLine = true,
            isError = passwordMatchError != null,
            supportingText = passwordMatchError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it },
            label = { Text(stringResource(R.string.credentials_password_hint_optional)) },
            placeholder = { Text(stringResource(R.string.credentials_password_hint_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { errorMsg ->
            Spacer(Modifier.height(12.dp))
            Text(text = errorMsg, color = colors.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onConfirm(password, hint.ifBlank { null }) },
            enabled = !isLoading && isFormValid,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.credentials_setting))
            } else {
                Text(stringResource(R.string.credentials_set_password), fontSize = 16.sp)
            }
        }
    }
}
