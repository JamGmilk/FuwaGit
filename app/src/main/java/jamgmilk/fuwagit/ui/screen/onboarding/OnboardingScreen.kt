package jamgmilk.fuwagit.ui.screen.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.ui.navigation.AddRepoTab
import jamgmilk.fuwagit.ui.theme.AppShapes
import androidx.core.graphics.createBitmap

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onAddRepository: (tab: AddRepoTab) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val steps = OnboardingStep.entries
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    MaterialTheme.colorScheme
    var isPermissionGranted by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentStep) {
        pagerState.animateScrollToPage(uiState.currentStep.ordinal)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        StepIndicator(
            currentStep = uiState.currentStep,
            totalSteps = steps.size,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f)
        ) { page ->
            val step = steps[page]
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.PERMISSIONS -> PermissionsStep(
                        onPermissionGranted = { isPermissionGranted = it }
                    )
                    OnboardingStep.MASTER_PASSWORD -> MasterPasswordStep(
                        uiState = uiState,
                        onUpdatePassword = viewModel::updatePassword,
                        onUpdateConfirmPassword = viewModel::updateConfirmPassword,
                        onUpdatePasswordHint = viewModel::updatePasswordHint,
                        onUpdateEnableBiometric = viewModel::updateEnableBiometric
                    )
                    OnboardingStep.GIT_CONFIG -> GitConfigStep(
                        uiState = uiState,
                        onUpdateUserName = viewModel::updateUserName,
                        onUpdateUserEmail = viewModel::updateUserEmail,
                        onUpdateDefaultBranch = viewModel::updateDefaultBranch
                    )
                    OnboardingStep.ADD_REPO -> AddRepoStep(
                        onAddRepository = onAddRepository
                    )
                    OnboardingStep.COMPLETE -> CompleteStep(modifier = Modifier.weight(1f))
                }
            }
        }

        BottomNavigationSlot(
            currentStep = uiState.currentStep,
            isPermissionGranted = isPermissionGranted,
            isPasswordValid = uiState.password.isNotBlank() && uiState.confirmPassword.isNotBlank() && !uiState.isSettingPassword,
            isSavingConfig = uiState.isSavingConfig,
            isSettingPassword = uiState.isSettingPassword,
            isGitConfigValid = uiState.userName.isNotBlank() && uiState.userEmail.isNotBlank() && uiState.defaultBranch.isNotBlank(),
            onNext = viewModel::nextStep,
            onSkipPassword = viewModel::skipPassword,
            onSetupPassword = { viewModel.setupPasswordAndContinue(fragmentActivity) },
            onSaveConfig = viewModel::saveConfigAndContinue,
            onAddRepository = onAddRepository,
            onComplete = {
                viewModel.completeOnboarding()
                onComplete()
            }
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StepIndicator(
    currentStep: OnboardingStep,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingStep.entries.forEachIndexed { index, step ->
            val isActive = index <= currentStep.ordinal
            val isCurrent = index == currentStep.ordinal

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) colors.primary else colors.outlineVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(colors.onPrimary)
                    )
                }
            }
            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (index < currentStep.ordinal) colors.primary else colors.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationSlot(
    currentStep: OnboardingStep,
    isPermissionGranted: Boolean,
    isPasswordValid: Boolean,
    isSavingConfig: Boolean,
    isSettingPassword: Boolean,
    isGitConfigValid: Boolean,
    onNext: () -> Unit,
    onSkipPassword: () -> Unit,
    onSetupPassword: () -> Unit,
    onSaveConfig: () -> Unit,
    onAddRepository: (tab: AddRepoTab) -> Unit,
    onComplete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    when (currentStep) {
        OnboardingStep.WELCOME -> {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(stringResource(R.string.onboarding_get_started))
            }
        }
        OnboardingStep.PERMISSIONS -> {
            Button(
                onClick = onNext,
                enabled = isPermissionGranted,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                )
            ) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
        OnboardingStep.MASTER_PASSWORD -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onSkipPassword, modifier = Modifier.height(52.dp)) {
                    Text(stringResource(R.string.onboarding_skip))
                }

                Button(
                    onClick = onSetupPassword,
                    enabled = isPasswordValid,
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                    )
                ) {
                    if (isSettingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.credentials_setting))
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.credentials_set_password))
                    }
                }
            }
        }
        OnboardingStep.GIT_CONFIG -> {
            Button(
                onClick = onSaveConfig,
                enabled = isGitConfigValid && !isSavingConfig,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                )
            ) {
                if (isSavingConfig) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.onboarding_saving))
                } else {
                    Text(stringResource(R.string.onboarding_next))
                }
            }
        }
        OnboardingStep.ADD_REPO -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onNext, modifier = Modifier.height(52.dp)) {
                    Text(stringResource(R.string.onboarding_skip))
                }

                Button(
                    onClick = { onAddRepository(AddRepoTab.Clone) },
                    modifier = Modifier.weight(2f).height(52.dp),
                    shape = AppShapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(stringResource(R.string.onboarding_add_repo))
                }
            }
        }
        OnboardingStep.COMPLETE -> {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(stringResource(R.string.onboarding_finish))
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = colors.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .size(72.dp)
                    .wrapContentSize(Alignment.Center)
            )
        }

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureItem(
                icon = Icons.Default.Storage,
                title = stringResource(R.string.onboarding_feature_git_title),
                description = stringResource(R.string.onboarding_feature_git_desc)
            )
            FeatureItem(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.onboarding_feature_security_title),
                description = stringResource(R.string.onboarding_feature_security_desc)
            )
            FeatureItem(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.onboarding_feature_repos_title),
                description = stringResource(R.string.onboarding_feature_repos_desc)
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.small,
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
                        icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    onPermissionGranted: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isInspectionMode = LocalInspectionMode.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    fun checkAllFilesStatus(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) PermissionStatus.Granted else PermissionStatus.Denied
        } else {
            val writeGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (writeGranted) PermissionStatus.Granted else PermissionStatus.Denied
        }
    }

    var allFilesStatus by remember {
        mutableStateOf(
            if (isInspectionMode) PermissionStatus.Unknown else checkAllFilesStatus()
        )
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    allFilesStatus = checkAllFilesStatus()
                }
            }
        })
    }

    LaunchedEffect(allFilesStatus) {
        onPermissionGranted(allFilesStatus == PermissionStatus.Granted)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StepHeader(
            icon = Icons.Default.Shield,
            title = stringResource(R.string.onboarding_permissions_title),
            subtitle = stringResource(R.string.onboarding_permissions_subtitle)
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.permissions_all_files_access),
                    description = stringResource(R.string.onboarding_permissions_storage_desc),
                    status = allFilesStatus,
                    actionLabel = stringResource(R.string.permissions_grant),
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = "package:${context.packageName}".toUri()
                            })
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    status: PermissionStatus,
    actionLabel: String,
    onAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(status = status)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onAction,
                enabled = status != PermissionStatus.Granted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PermissionStatus) {
    val colors = MaterialTheme.colorScheme
    val (color, icon, text) = when (status) {
        PermissionStatus.Granted -> Triple(colors.primary, Icons.Default.CheckCircle, stringResource(R.string.permissions_status_granted))
        PermissionStatus.Denied -> Triple(colors.error, Icons.Default.Error, stringResource(R.string.permissions_status_denied))
        PermissionStatus.Unknown -> Triple(colors.tertiary, Icons.Default.Info, stringResource(R.string.permissions_status_unknown))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
    }
}

private enum class PermissionStatus {
    Granted, Denied, Unknown
}

@Composable
private fun MasterPasswordStep(
    uiState: OnboardingUiState,
    onUpdatePassword: (String) -> Unit,
    onUpdateConfirmPassword: (String) -> Unit,
    onUpdatePasswordHint: (String) -> Unit,
    onUpdateEnableBiometric: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepHeader(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.onboarding_master_password_title),
            subtitle = stringResource(R.string.onboarding_master_password_subtitle)
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onUpdatePassword,
            label = { Text(stringResource(R.string.credentials_password_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary,
                cursorColor = colors.primary
            )
        )

        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = onUpdateConfirmPassword,
            label = { Text(stringResource(R.string.credentials_confirm_password_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary,
                cursorColor = colors.primary
            )
        )

        OutlinedTextField(
            value = uiState.passwordHint,
            onValueChange = onUpdatePasswordHint,
            label = { Text(stringResource(R.string.credentials_password_hint_optional)) },
            placeholder = { Text(stringResource(R.string.credentials_password_hint_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary,
                cursorColor = colors.primary
            )
        )

        if (uiState.passwordError != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.passwordError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
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
                    checked = uiState.enableBiometric,
                    onCheckedChange = onUpdateEnableBiometric,
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.primary)
                )
            }
        }
    }
}

@Composable
private fun GitConfigStep(
    uiState: OnboardingUiState,
    onUpdateUserName: (String) -> Unit,
    onUpdateUserEmail: (String) -> Unit,
    onUpdateDefaultBranch: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepHeader(
            icon = Icons.Default.Person,
            title = stringResource(R.string.onboarding_git_config_title),
            subtitle = stringResource(R.string.onboarding_git_config_subtitle)
        )

        OutlinedTextField(
            value = uiState.userName,
            onValueChange = onUpdateUserName,
            label = { Text(stringResource(R.string.settings_user_name_label)) },
            placeholder = { Text(stringResource(R.string.settings_user_name_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary,
                cursorColor = colors.primary
            )
        )

        OutlinedTextField(
            value = uiState.userEmail,
            onValueChange = onUpdateUserEmail,
            label = { Text(stringResource(R.string.settings_user_email_input_label)) },
            placeholder = { Text(stringResource(R.string.settings_user_email_input_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary,
                cursorColor = colors.primary
            )
        )

        OutlinedTextField(
            value = uiState.defaultBranch,
            onValueChange = onUpdateDefaultBranch,
            label = { Text(stringResource(R.string.settings_default_branch_label)) },
            placeholder = { Text(stringResource(R.string.settings_default_branch_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary,
                cursorColor = colors.primary
            )
        )
    }
}

@Composable
private fun AddRepoStep(
    onAddRepository: (tab: AddRepoTab) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepHeader(
            icon = Icons.Default.Folder,
            title = stringResource(R.string.onboarding_add_repo_title),
            subtitle = stringResource(R.string.onboarding_add_repo_subtitle)
        )

        ElevatedCard(
            onClick = { onAddRepository(AddRepoTab.Clone) },
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
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_clone_repo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_clone_repo_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        ElevatedCard(
            onClick = { onAddRepository(AddRepoTab.Local) },
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
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.secondary.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_local_repo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_local_repo_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompleteStep(
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_complete_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StepHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}
