package jamgmilk.fuwagit.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jamgmilk.fuwagit.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> stringResource(R.string.history_time_just_now)
        diff < 3_600_000 -> stringResource(R.string.history_time_minutes_ago, diff / 60_000)
        diff < 86_400_000 -> stringResource(R.string.history_time_hours_ago, diff / 3_600_000)
        diff < 604_800_000 -> stringResource(R.string.history_time_days_ago, diff / 86_400_000)
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
