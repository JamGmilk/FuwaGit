package jamgmilk.obsigit.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.theme.ObsiGitTheme

@Composable
fun VaultScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val folders by viewModel.vaultItems.collectAsState()
    val selectedTarget by viewModel.targetPath.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshVaultItems(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vault",
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            if (folders.isEmpty()) {
                // TODO: 这里换个显示样式，中间那种灰色大图标，然后下方配上文字
                Text(
                    text = "No folders loaded yet. Manage scans in Settings > Path Scans.",
                    modifier = Modifier.padding(16.dp),
                    color = colors.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tap a folder row to set it as Git target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                            //.background(CatNight),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(folders, key = { it.path }) { item ->
                            VaultItemRow(
                                item = item,
                                isSelectedTarget = item.localPath != null && item.localPath == selectedTarget,
                                onSetTarget = {
                                    item.localPath?.let { path ->
                                        viewModel.setTargetPath(path)
                                        viewModel.switchPage(AppPage.GitTerminal)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultItemRow(
    item: VaultFolderItem,
    isSelectedTarget: Boolean,
    onSetTarget: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val canSetTarget = item.localPath != null
    ElevatedCard(
        onClick = onSetTarget,
        enabled = canSetTarget,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelectedTarget) colors.primaryContainer else colors.surface.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(item.name, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                // TODO: 这里的图标有时候小小的，要修一下喵~
                if (item.isGitRepo) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = AppVaultOps.shortDisplayPath(item.path),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Owner: ${item.owner}", // TODO: 这里后面要补上 UID 属于哪个包名喵~ 不过这点确实用不上了
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = if (item.isGitRepo) "Git repository: yes" else "Git repository: no",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isGitRepo) colors.tertiary else colors.onSurfaceVariant
            )

        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun VaultScreenPreview() {
    ObsiGitTheme {
        VaultScreen(viewModel = AppViewModel())
    }
}