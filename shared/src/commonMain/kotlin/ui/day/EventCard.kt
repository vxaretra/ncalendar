package ui.day

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import data.Event
import ui.formatTime

/**
 * A single event inside the day detail list.
 *
 * The card is theme-aware (colors come from the active Material 3 scheme) and tap
 * behavior is explicit: click edits, long press asks to delete. The trailing delete
 * button exposes the same delete flow for pointer users, who would not discover it
 * behind a long press on desktop.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeRange = "${formatTime(event.startTime)} – ${formatTime(event.endTime)}"

    val accessibilityLabel = buildString {
        append("Event: ${event.title}, $timeRange")
        event.description?.let { append(". $it") }
        append(". Click to edit, long press to delete.")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The clickable content is separate from the delete button so the button
            // stays reachable for accessibility (merged click semantics would swallow it).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = accessibilityLabel
                    }
                    .combinedClickable(
                        onClickLabel = "Edit event",
                        role = Role.Button,
                        onLongClickLabel = "Delete event",
                        onLongClick = onDelete,
                        onClick = onClick,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                event.description?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete event")
            }
        }
    }
}
