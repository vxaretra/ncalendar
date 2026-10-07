package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.Event
import data.EventRepository
import data.generateId
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.form.EventFormDialog
import ui.form.EventFormState

@Composable
fun App(repository: EventRepository) {
    val today = remember {
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }
    var selectedDate by remember { mutableStateOf(today) }

    val allEvents by repository.events.collectAsState()
    val eventsForSelectedDate = allEvents.filter { it.date == selectedDate }

    var showForm by remember { mutableStateOf(false) }
    var formState by remember { mutableStateOf(EventFormState()) }
    var pendingDelete by remember { mutableStateOf<Event?>(null) }

    val scope = rememberCoroutineScope()

    fun openCreateForm() {
        formState = EventFormState()
        showForm = true
    }

    fun openEditForm(event: Event) {
        formState = EventFormState(
            id = event.id,
            title = event.title,
            startTime = event.startTime,
            endTime = event.endTime,
            description = event.description ?: ""
        )
        showForm = true
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
                events = eventsForSelectedDate,
                onAddEvent = { openCreateForm() },
                onEditEvent = { openEditForm(it) },
                onDeleteEvent = { pendingDelete = it },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
            )
        }

        if (showForm) {
            EventFormDialog(
                state = formState,
                onStateChange = { formState = it },
                onConfirm = {
                    val validated = formState.validate()
                    if (validated.isValid) {
                        val event = Event(
                            id = formState.id ?: generateId(),
                            title = formState.title.trim(),
                            date = selectedDate,
                            startTime = formState.startTime,
                            endTime = formState.endTime,
                            description = formState.description.ifBlank { null }
                        )
                        scope.launch {
                            if (formState.isEditing) repository.updateEvent(event)
                            else repository.addEvent(event)
                        }
                        showForm = false
                    } else {
                        formState = validated
                    }
                },
                onDismiss = { showForm = false }
            )
        }

        pendingDelete?.let { event ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete event?") },
                text = { Text("Delete \"${event.title}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            repository.deleteEvent(event.id)
                        }
                        pendingDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}
