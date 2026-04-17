package jamgmilk.fuwagit.ui.screen.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.UiMessage
import jamgmilk.fuwagit.domain.model.toResource
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate

enum class MasterPasswordMode {
    SETUP,
    CHANGE
}

@Composable
fun MasterPasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit = {},
    viewModel: MasterPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mode = if (uiState.isMasterPasswordSet) MasterPasswordMode.CHANGE else MasterPasswordMode.SETUP

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onSuccess()
        }
    }

    SubSettingsTemplate(
        title = when (mode) {
            MasterPasswordMode.SETUP -> stringResource(R.string.credentials_set_password)
            MasterPasswordMode.CHANGE -> stringResource(R.string.credentials_change_password)
        },
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            MasterPasswordContent(
                mode = mode,
                passwordHint = uiState.passwordHint,
                isBiometricEnabled = uiState.isBiometricEnabled,
                error = uiState.error,
                isLoading = uiState.isLoading,
                onSetup = { password, confirmPassword, hint ->
                    viewModel.setupMasterPassword(password, confirmPassword, hint)
                },
                onChange = { oldPassword, newPassword, confirmPassword, hint ->
                    viewModel.changeMasterPassword(oldPassword, newPassword, confirmPassword, hint)
                },
                onEnableBiometric = { activity ->
                    viewModel.enableBiometric(activity)
                },
                onDisableBiometric = {
                    viewModel.disableBiometric()
                },
                onClearError = {
                    viewModel.clearError()
                },
                onComplete = onBack
            )
        }
    }
}

@Composable
private fun MasterPasswordContent(
    mode: MasterPasswordMode,
    passwordHint: String?,
    isBiometricEnabled: Boolean,
    error: UiMessage?,
    isLoading: Boolean,
    onSetup: (password: String, confirmPassword: String, hint: String?) -> Unit,
    onChange: (oldPassword: String, newPassword: String, confirmPassword: String, hint: String?) -> Unit,
    onEnableBiometric: (FragmentActivity) -> Unit,
    onDisableBiometric: () -> Unit,
    onClearError: () -> Unit,
    onComplete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var oldPassword by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val passwordMatchError = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
        stringResource(R.string.credentials_passwords_do_not_match)
    } else null

    val passwordLengthError = if (password.isNotEmpty() && password.length < 6) {
        stringResource(R.string.credentials_at_least_6_characters)
    } else null

    val isSetupMode = mode == MasterPasswordMode.SETUP
    val isFormValid = if (isSetupMode) {
        password.length >= 6 && password == confirmPassword
    } else {
        oldPassword.isNotBlank() && password.length >= 6 && password == confirmPassword
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    when (mode) {
                        MasterPasswordMode.SETUP -> colors.primary.copy(alpha = 0.12f)
                        MasterPasswordMode.CHANGE -> colors.tertiary.copy(alpha = 0.12f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (mode) {
                    MasterPasswordMode.SETUP -> Icons.Default.Lock
                    MasterPasswordMode.CHANGE -> Icons.Default.Key
                },
                contentDescription = null,
                tint = when (mode) {
                    MasterPasswordMode.SETUP -> colors.primary
                    MasterPasswordMode.CHANGE -> colors.tertiary
                },
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (mode) {
                MasterPasswordMode.SETUP -> stringResource(R.string.credentials_setup_description)
                MasterPasswordMode.CHANGE -> stringResource(R.string.credentials_change_password_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isSetupMode) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        if (error == UiMessage.Credential.IncorrectOldPassword) {
                            onClearError()
                        }
                    },
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = error == UiMessage.Credential.IncorrectOldPassword,
                    supportingText = if (error == UiMessage.Credential.IncorrectOldPassword) {
                        { Text(stringResource(error.toResource())) }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.tertiary,
                        focusedLabelColor = colors.tertiary,
                        cursorColor = colors.tertiary
                    )
                )

                if (!passwordHint.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.credentials_current_hint_format, passwordHint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = colors.outline.copy(alpha = 0.2f)
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        if (isSetupMode) stringResource(R.string.credentials_password_label)
                        else stringResource(R.string.credentials_new_password_label)
                    )
                },
                placeholder = { Text(stringResource(R.string.credentials_password_placeholder)) },
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
                    imeAction = ImeAction.Next
                ),
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
                label = {
                    Text(
                        if (isSetupMode) stringResource(R.string.credentials_confirm_password_label)
                        else stringResource(R.string.credentials_confirm_new_password_label)
                    )
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPassword) stringResource(R.string.credentials_hide) else stringResource(R.string.credentials_show_hide)
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

            error?.let { errorMsg ->
                if (errorMsg != UiMessage.Credential.IncorrectOldPassword) {
                    Text(
                        text = stringResource(errorMsg.toResource()),
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        BiometricUnlockCard(
            isBiometricEnabled = isBiometricEnabled,
            onEnableBiometric = { activity?.let { onEnableBiometric(it) } },
            onDisableBiometric = onDisableBiometric
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isSetupMode) {
                    onSetup(password, confirmPassword, hint.ifBlank { null })
                } else {
                    onChange(oldPassword, password, confirmPassword, hint.ifBlank { null })
                }
            },
            enabled = !isLoading && isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (mode) {
                    MasterPasswordMode.SETUP -> colors.primary
                    MasterPasswordMode.CHANGE -> colors.tertiary
                }
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSetupMode) stringResource(R.string.credentials_setting)
                    else stringResource(R.string.credentials_changing)
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSetupMode) stringResource(R.string.credentials_set_password)
                    else stringResource(R.string.credentials_change_password),
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BiometricUnlockCard(
    isBiometricEnabled: Boolean,
    onEnableBiometric: () -> Unit,
    onDisableBiometric: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_biometric_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.onboarding_biometric_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = { if (it) onEnableBiometric() else onDisableBiometric() },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
            )
        }
    }
}