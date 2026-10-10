package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import data.Event
import data.EventRepository
import data.ReminderRepository
import data.generateId
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import reminder.InAppNotifier
import reminder.InAppReminderNotification
import reminder.ReminderActivationBus
import ui.chat.ChatDialog
import ui.chat.ChatMessage
import ui.chat.EchoChatEngine
import ui.chat.Role
import ui.chat.newChatMessageId
import ui.day.AddEventDialog
import ui.day.DayDetailSection
import ui.form.EventFormState
import ui.layout.LayoutMode
import ui.layout.layoutForWidth
import ui.theme.AppTheme

@Composable
fun App() {
    val repository = koinInject<EventRepository>()
    val reminderRepository = koinInject<ReminderRepository>()
    val inAppNotifier = koinInject<InAppNotifier>()
    val activationBus = koinInject<ReminderActivationBus>()

    val today = remember {
        kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    // selectedDate is the single source of truth for both the calendar and the detail panel.
    var selectedDate by remember { mutableStateOf(today) }

    // Tray notification clicks (or plain tray clicks) land here: jump to the event's day.
    LaunchedEffect(Unit) {
        activationBus.activations.collect { event ->
            event?.let { selectedDate = it.date }
        }
    }

    // In-app fallback banners, shown when the platform has no usable system tray.
    val reminderBanners by inAppNotifier.notifications.collectAsState()

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

    // AI chat. Messages and the pending flag are hoisted here, and replies run in this
    // scope, so closing the popup mid-reply does not lose the bot's answer.
    var chatOpen by remember { mutableStateOf(false) }
    val chatEngine = remember { EchoChatEngine() }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var chatAwaitingReply by remember { mutableStateOf(false) }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || chatAwaitingReply) return
        chatMessages += ChatMessage(
            id = newChatMessageId(),
            role = Role.User,
            text = trimmed,
            timestamp = kotlin.time.Clock.System.now(),
        )
        chatAwaitingReply = true
        val history = chatMessages.toList()
        scope.launch {
            val reply = chatEngine.reply(history)
            chatMessages += ChatMessage(
                id = newChatMessageId(),
                role = Role.Bot,
                text = reply,
                timestamp = kotlin.time.Clock.System.now(),
            )
            chatAwaitingReply = false
        }
    }

    fun openCreateForm() {
        // The chat popup is modal. Keyboard focus can still wander behind its scrim (this
        // Compose version has no focus trap), so background actions must not open another
        // surface while it is up.
        if (chatOpen) return
        formState = EventFormState(date = selectedDate)
        showForm = true
    }

    fun openEditForm(event: Event) {
        if (chatOpen) return
        formState = EventFormState(
            id = event.id,
            title = event.title,
            date = event.date,
            startTime = event.startTime,
            endTime = event.endTime,
            description = event.description ?: ""
        )
        showForm = true
        // Reminders load asynchronously; apply them unless the user already started editing.
        scope.launch {
            val offsets = reminderRepository.remindersForEvent(event.id).map { it.offsetMinutes }
            if (formState.id == event.id && formState.reminderOffsets.isEmpty()) {
                formState = formState.copy(reminderOffsets = offsets)
            }
        }
    }

    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(Modifier.fillMaxSize()) {
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
                                today = today,
                                events = eventsForSelectedDate,
                                onAddEvent = { openCreateForm() },
                                onTodaySelected = { selectedDate = today },
                                onOpenChat = { chatOpen = true },
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
                                today = today,
                                events = eventsForSelectedDate,
                                onAddEvent = { openCreateForm() },
                                onTodaySelected = { selectedDate = today },
                                onOpenChat = { chatOpen = true },
                                onEditEvent = { openEditForm(it) },
                                onDeleteEvent = { pendingDelete = it },
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(layout.detailWeight)
                            )
                        }
                    }
                }

                // In-app reminder banners: tap selects the event's day, the X dismisses.
                if (reminderBanners.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .widthIn(max = 520.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        reminderBanners.forEach { banner ->
                            ReminderBanner(
                                notification = banner,
                                onClick = {
                                    selectedDate = banner.event.date
                                    inAppNotifier.dismiss(banner.reminderId)
                                },
                                onDismiss = { inAppNotifier.dismiss(banner.reminderId) },
                            )
                        }
                    }
                }

                // AI chat modal: rendered last inside this Box so it sits above the
                // calendar and the FABs - its scrim and card occlude them for pointer
                // input (hover included), so the FabMenu collapses behind it.
                if (chatOpen) {
                    ChatDialog(
                        messages = chatMessages,
                        awaitingReply = chatAwaitingReply,
                        onSend = { text -> sendChatMessage(text) },
                        onDismiss = { chatOpen = false },
                    )
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
                            date = formState.date,
                            startTime = formState.startTime,
                            endTime = formState.endTime,
                            description = formState.description.ifBlank { null }
                        )
                        val original = formState.id?.let { id -> allEvents.firstOrNull { it.id == id } }
                        val startChanged = original != null &&
                            (original.date != event.date || original.startTime != event.startTime)
                        val offsets = formState.reminderOffsets
                        scope.launch {
                            if (formState.isEditing) {
                                repository.updateEvent(event)
                                // Start/date moved: let the reminders fire again at the new time.
                                if (startChanged) reminderRepository.clearFiredForEvent(event.id)
                            } else {
                                repository.addEvent(event)
                            }
                            // Must run after clearFiredForEvent: the sync re-silences offsets
                            // whose due time is already in the past.
                            reminderRepository.setRemindersForEvent(event, offsets)
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

/**
 * In-app reminder banner used when system notifications are unavailable. Shows the same
 * content as the tray notification: event title plus the offset read-out.
 */
@Composable
private fun ReminderBanner(
    notification: InAppReminderNotification,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "Reminder: ${notification.title}. ${notification.body}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss reminder")
            }
        }
    }
}
