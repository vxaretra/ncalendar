package ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.Event
import kotlinx.datetime.LocalDate
import ui.components.FabAction
import ui.components.FabMenu
import ui.formatDate

/**
 * Day detail panel: the selected day's header, its events (or an explanatory empty
 * state) and the floating add-event / jump-to-today actions.
 *
 * The "Today" action is disabled while [date] is already [today].
 *
 * [compact] is used by the stacked layout for tighter paddings.
 */
@Composable
fun DayDetailSection(
    date: LocalDate,
    today: LocalDate,
    events: List<Event>,
    onAddEvent: () -> Unit,
    onTodaySelected: () -> Unit,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val horizontalPadding = if (compact) 16.dp else 24.dp
    val verticalPadding = if (compact) 12.dp else 16.dp

    val isToday = date == today

    val todayAction = FabAction(
        id = "today",
        icon = Icons.Default.Today,
        contentDescription = if (isToday) "Already showing today" else "Jump to today",
        enabled = !isToday,
        onClick = onTodaySelected,
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Text(
                text = formatDate(date),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (events.isEmpty()) {
                    EmptyDayState(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                onClick = { onEditEvent(event) },
                                onLongClick = { onDeleteEvent(event) }
                            )
                        }
                    }
                }
            }
        }

        FabMenu(
            actions = listOf(todayAction),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(if (compact) 12.dp else 16.dp),
        ) {
            FloatingActionButton(onClick = onAddEvent) {
                Icon(Icons.Default.Add, contentDescription = "Add event")
            }
        }
    }
}

@Composable
private fun EmptyDayState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nothing scheduled for this day",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Press Enter or use the + button to add an event.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}
