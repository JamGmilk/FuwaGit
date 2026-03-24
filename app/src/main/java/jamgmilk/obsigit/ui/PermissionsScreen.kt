package jamgmilk.obsigit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras

@Composable
fun PermissionsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val rootStatus by viewModel.rootStatus.collectAsState()
    val grantedFolders by viewModel.grantedTreeUris.collectAsState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

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
                    text = "Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary
                )
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
                    Text("System Permissions", fontWeight = FontWeight.Bold, color = colors.primary)

                    ListItem(
                        headlineContent = { Text("All Files Access") },
                        supportingContent = {
                            val status = if (LocalInspectionMode.current) "Unknown" 
                                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (Environment.isExternalStorageManager()) "Granted" else "Denied"
                                } else {
                                    val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                    if (writeGranted) "Granted" else "Denied"
                                }
                            Text("Status: $status")
                        },
                        leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                        trailingContent = {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = "package:${context.packageName}".toUri()
                                        })
                                    } else {
                                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text("Grant")
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Root Access") },
                        supportingContent = {
                            Text("Status: ${
                                when (rootStatus) {
                                    RootStatus.Idle -> "Not checked"
                                    RootStatus.Checking -> "Checking..."
                                    RootStatus.Granted -> "Granted"
                                    RootStatus.Denied -> "Denied"
                                }
                            }")
                        },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                        trailingContent = {
                            Button(
                                onClick = { viewModel.checkRoot() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary)
                            ) {
                                Text("Check")
                            }
                        }
                    )
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
                    Text("Scoped Storage", fontWeight = FontWeight.Bold, color = colors.primary)
                    
                    ListItem(
                        headlineContent = { Text("Folder Grants") },
                        supportingContent = { Text("Number of folders with SAF access: ${grantedFolders.size}") },
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                    
                    Text(
                        text = "Folder grants are managed automatically when you add local repositories. You can remove them by long-pressing an item in the Repo list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
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
                    Text("Security Information", fontWeight = FontWeight.Bold, color = colors.primary)
                    ListItem(
                        headlineContent = { Text("Encryption") },
                        supportingContent = { Text("Git operations use system-level security providers.") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    ObsiGitTheme {
        PermissionsScreen(
            viewModel = AppViewModel(),
            onBack = { }
        )
    }
}
