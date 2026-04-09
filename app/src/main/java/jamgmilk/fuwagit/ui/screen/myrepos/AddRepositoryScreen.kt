package jamgmilk.fuwagit.ui.screen.myrepos

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import jamgmilk.fuwagit.ui.screen.credentials.CredentialSelectDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType
import jamgmilk.fuwagit.ui.theme.AppShapes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

sealed class AddRepoTab {
    data object Clone : AddRepoTab()
    data object Local : AddRepoTab()
}

enum class UrlProtocol {
    NONE, HTTPS, SSH
}

data class UrlValidationResult(
    val isValid: Boolean,
    val protocol: UrlProtocol,
    val errorMessage: String? = null
)

@Composable
fun AddRepositoryScreen(
    onBack: () -> Unit,
    onCloneComplete: (String) -> Unit,
    onAddRepository: (path: String, alias: String?) -> Unit,
    modifier: Modifier = Modifier,
    myReposViewModel: MyReposViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf<AddRepoTab>(AddRepoTab.Clone) }
    
    // Pre-fetch strings for use in non-composable contexts
    val strRepositoryAdded = stringResource(R.string.myrepos_repository_added)

    SubSettingsTemplate(
        title = stringResource(R.string.add_repo_screen_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AddRepoTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "add_repo_tab_transition"
            ) { tab ->
                when (tab) {
                    is AddRepoTab.Clone -> {
                        CloneContent(
                            myReposViewModel = myReposViewModel,
                            onCloneComplete = onCloneComplete
                        )
                    }
                    is AddRepoTab.Local -> {
                        LocalContent(
                            myReposViewModel = myReposViewModel,
                            onAddRepository = { path, alias ->
                                onAddRepository(path, alias)
                                // TODO: Toast 不好看啊
                                Toast.makeText(context, strRepositoryAdded, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRepoTabSelector(
    selectedTab: AddRepoTab,
    onTabSelected: (AddRepoTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AddRepoTabChip(
            label = stringResource(R.string.add_repo_tab_clone),
            icon = Icons.Default.CloudDownload,
            selected = selectedTab is AddRepoTab.Clone,
            onClick = { onTabSelected(AddRepoTab.Clone) },
            modifier = Modifier.weight(1f)
        )

        AddRepoTabChip(
            label = stringResource(R.string.add_repo_tab_local),
            icon = Icons.Default.Folder,
            selected = selectedTab is AddRepoTab.Local,
            onClick = { onTabSelected(AddRepoTab.Local) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AddRepoTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .clip(AppShapes.small)
            .clickable(onClick = onClick),
        shape = AppShapes.small,
        color = if (selected) colors.primaryContainer.copy(alpha = 0.3f) else colors.surfaceContainerLow,
        border = if (selected) {
            BorderStroke(2.dp, colors.primary)
        } else {
            BorderStroke(1.dp, colors.outlineVariant)
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
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface
            )
        }
    }
}

fun validateUrl(url: String): UrlValidationResult {
    if (url.isBlank()) {
        return UrlValidationResult(false, UrlProtocol.NONE)
    }

    return when {
        url.startsWith("https://") || url.startsWith("http://") -> {
            if (url.contains(" ") || !url.contains(".") || url.length < 10) {
                UrlValidationResult(false, UrlProtocol.HTTPS, "Invalid HTTPS URL format")
            } else {
                UrlValidationResult(true, UrlProtocol.HTTPS)
            }
        }
        url.startsWith("git@") -> {
            val gitHostPattern = Regex("^git@[a-zA-Z0-9.-]+:[a-zA-Z0-9._/-]+$")
            if (gitHostPattern.matches(url)) {
                UrlValidationResult(true, UrlProtocol.SSH)
            } else {
                UrlValidationResult(false, UrlProtocol.SSH, "Invalid SSH format (expected: git@host:path)")
            }
        }
        url.startsWith("ssh://") -> {
            UrlValidationResult(true, UrlProtocol.SSH)
        }
        else -> {
            UrlValidationResult(false, UrlProtocol.NONE, "URL must start with https://, http://, git@, or ssh://")
        }
    }
}

fun extractRepoName(url: String): String {
    return url
        .substringAfterLast("/")
        .substringBefore(".git")
        .substringBefore("?")
        .ifBlank { "repository" }
}
