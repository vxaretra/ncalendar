package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.EventRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun App(repository: EventRepository) {
    val today = remember {
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }
    var selectedDate by remember { mutableStateOf(today) }

    val events = remember(selectedDate) {
        repository.getEventsForDate(selectedDate)
    }

    MaterialTheme {
        Row(Modifier.fillMaxSize()) {
            CalendarSection(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            )

            DayDetailSection(
                date = selectedDate,
                events = events,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
            )
        }
    }
}
