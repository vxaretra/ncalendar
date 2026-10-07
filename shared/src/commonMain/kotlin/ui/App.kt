package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import data.EventRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun App(repository: EventRepository) {
    val today = remember {
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }
    var selectedDate by remember { mutableStateOf(today) }

    val events = remember(selectedDate) {
        repository.getEventsForDate(selectedDate)
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize()) {

            CalendarSection(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
            )

            HorizontalDivider()

            DayDetailSection(
                date = selectedDate,
                events = events,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
            )
        }
    }
}
