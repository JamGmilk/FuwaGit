package jamgmilk.obsigit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.theme.ObsiGitTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun RootCheckScreen(viewModel: RootViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.status.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Root Check",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = DeepSakura,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.elevatedCardColors(
                // CHANGED: Lower alpha (0.3f) makes it transparent/glassy
                containerColor = Color.White.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = status,
                    label = "root_state",
                    transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) }
                ) { state ->
                    when (state) {
                        is RootStatus.Idle -> Text("Waiting for your command...", color = Color.Gray)
                        is RootStatus.Checking -> CircularProgressIndicator(color = DeepSakura)
                        is RootStatus.Granted ->
                            Text(
                                text = "Root Access Granted",
                                color = Color(0xFF66BB6A),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        is RootStatus.Denied ->
                            Text(
                                text = "Access Denied",
                                color = Color(0xFFEF5350),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PinkActionButton(
            text = if (status == RootStatus.Checking) "Checking..." else "Verify Root Permissions",
            onClick = { viewModel.checkRoot() }
        )
    }
}



@Composable
fun FolderInfoScreen(viewModel: RootViewModel, modifier: Modifier = Modifier) {
    val owner by viewModel.folderOwner.collectAsState()
    val obsDir = "/storage/emulated/0/Android/data/md.obsidian"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally, // Centers the title and button
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Folder Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DeepSakura
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                // Thinner, softer border
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                // FIXED: Glassy transparent background
                containerColor = Color.White.copy(alpha = 0.3f)
            ),
            // FIXED: Removes the "ugly gray" shadow
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Target Path:", style = MaterialTheme.typography.labelMedium, color = DeepSakura)
                Text(
                    obsDir,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = SakuraPink.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Owner:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                    if (owner == "Loading...") {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepSakura)
                    } else {
                        Surface(
                            color = SakuraPink.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                owner,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepSakura
                            )
                        }
                    }
                }
            }
        }

        PinkActionButton(
            text = "Refresh",
            onClick = { viewModel.refreshOwner(obsDir) }
        )
    }
}






