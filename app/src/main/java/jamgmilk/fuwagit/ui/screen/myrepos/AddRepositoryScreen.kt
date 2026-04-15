package jamgmilk.fuwagit.ui.screen.myrepos

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.core.util.UrlUtils
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import jamgmilk.fuwagit.ui.navigation.AddRepoTab
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreViewModel
import jamgmilk.fuwagit.ui.theme.AppShapes

@Composable
fun AddRepositoryScreen(
    onBack: () -> Unit,
    onCloneComplete: (String) -> Unit,
    onAddRepository: (path: String, alias: String?) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AddRepoTab = AddRepoTab.Clone,
    myReposViewModel: MyReposViewModel = hiltViewModel(),
    credentialsViewModel: CredentialStoreViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(selectedTab) }

    val strRepositoryAdded = stringResource(R.string.myrepos_repository_added)

    SubSettingsTemplate(
        title = stringResource(R.string.add_repo_screen_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AddRepoTabSelector(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "add_repo_tab_transition"
            ) { tab ->
                when (tab) {
                    AddRepoTab.Clone -> {
                        CloneContent(
                            myReposViewModel = myReposViewModel,
                            credentialsViewModel = credentialsViewModel,
                            snackbarHostState = snackbarHostState,
                            onCloneComplete = onCloneComplete
                        )
                    }
                    AddRepoTab.Local -> {
                        LocalContent(
                            myReposViewModel = myReposViewModel,
                            onAddRepository = { path, alias ->
                                onAddRepository(path, alias)
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
            selected = selectedTab == AddRepoTab.Clone,
            onClick = { onTabSelected(AddRepoTab.Clone) },
            modifier = Modifier.weight(1f)
        )

        AddRepoTabChip(
            label = stringResource(R.string.add_repo_tab_local),
            icon = Icons.Default.Folder,
            selected = selectedTab == AddRepoTab.Local,
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
