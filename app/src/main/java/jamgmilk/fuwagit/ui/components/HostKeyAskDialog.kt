package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.data.jgit.HostKeyAskHelper
import jamgmilk.fuwagit.ui.theme.ButtonShapes
import kotlinx.coroutines.delay

@Composable
fun HostKeyAskDialog(
    host: String,
    keyType: String,
    fingerprint: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val timeoutSeconds = (HostKeyAskHelper.HOST_KEY_ASK_TIMEOUT_MS / 1000).toInt()
    var remainingSeconds by remember { mutableIntStateOf(timeoutSeconds) }
    var userResponded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        if (!userResponded) onReject()
    }

    DialogWithIcon(
        onDismiss = {},
        icon = Icons.Default.Shield,
        title = stringResource(R.string.hostkey_dialog_title),
        confirmButton = {
            Button(
                onClick = {
                    userResponded = true
                    onAccept()
                },
                shape = ButtonShapes
            ) {
                Text(stringResource(R.string.hostkey_dialog_accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    userResponded = true
                    onReject()
                },
                shape = ButtonShapes
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.hostkey_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HostKeyInfoRow(
                        label = stringResource(R.string.hostkey_dialog_host),
                        value = host
                    )
                    HostKeyInfoRow(
                        label = stringResource(R.string.hostkey_dialog_key_type),
                        value = keyType
                    )
                    HostKeyInfoRow(
                        label = stringResource(R.string.hostkey_dialog_fingerprint),
                        value = fingerprint
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.secondary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hostkey_dialog_security_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.secondary
                    )
                    Text(
                        text = stringResource(R.string.hostkey_dialog_security_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(R.string.hostkey_dialog_timeout, remainingSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HostKeyInfoRow(
    label: String,
    value: String
) {
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = colors.primary
        )
    }
}
