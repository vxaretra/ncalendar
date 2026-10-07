package ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.Event
import data.EventRepository
import data.generateId
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import ui.day.AddEventDialog
import ui.day.DayDetailSection
import ui.form.EventFormState
import ui.layout.LayoutMode
import ui.layout.layoutForWidth
import ui.shortcuts.AppShortcut
import ui.shortcuts.AppShortcutHandler
import ui.theme.AppTheme

@Composable
fun App(
    shortcutHandler: AppShortcutHandler = remember { AppShortcutHandler() },
) {
    val repository = koinInject<EventRepository>()

    val today = remember {
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    // selectedDate is the single source of truth for both the calendar and the detail panel.
    var selectedDate by remember { mutableStateOf(today) }

    val allEvents by repository.events.collectAsState()

    // Dates that have events, used for the calendar dots. Re-read from the repository
    // whenever the collected event list changes.
    val eventDates = remember(allEvents) { repository.allEventDates() }
    val eventsForSelectedDate = remember(allEvents, selectedDate) {
        allEvents.filter { it.date == selectedDate }
    }

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

    // Keyboard shortcuts: the decision logic lives in commonMain (ui.shortcuts), while
    // the desktop entry point forwards window-level key events to this handler.
    SideEffect {
        shortcutHandler.onShortcut = { shortcut ->
            when (shortcut) {
                AppShortcut.PreviousDay ->
                    selectedDate = selectedDate.minus(1, DateTimeUnit.DAY)

                AppShortcut.NextDay ->
                    selectedDate = selectedDate.plus(1, DateTimeUnit.DAY)

                AppShortcut.AddEvent -> openCreateForm()
            }
        }
        // While a dialog is open, shortcuts must not fire: typing, Enter and Escape
        // belong to the dialog.
        shortcutHandler.dialogsOpen = showForm || pendingDelete != null
    }

    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val layout = layoutForWidth(maxWidth.value)

                if (layout.mode == LayoutMode.Stacked) {
                    Column(Modifier.fillMaxSize()) {
                        CalendarSection(
                            selectedDate = selectedDate,
                            eventDates = eventDates,
                            onDateSelected = { selectedDate = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(layout.calendarWeight)
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                        DayDetailSection(
                            date = selectedDate,
                            events = eventsForSelectedDate,
                            onAddEvent = { openCreateForm() },
                            onEditEvent = { openEditForm(it) },
                            onDeleteEvent = { pendingDelete = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(layout.detailWeight),
                            compact = true
                        )
                    }
                } else {
                    Row(Modifier.fillMaxSize()) {
                        CalendarSection(
                            selectedDate = selectedDate,
                            eventDates = eventDates,
                            onDateSelected = { selectedDate = it },
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(layout.calendarWeight)
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
                                .weight(layout.detailWeight)
                        )
                    }
                }
            }
        }

        if (showForm) {
            AddEventDialog(
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
