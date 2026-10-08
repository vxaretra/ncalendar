package reminder

import data.Event
import data.Reminder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A reminder shown as an in-app banner when system notifications are unavailable. */
data class InAppReminderNotification(
    val reminderId: Long,
    val event: Event,
    val title: String,
    val body: String,
)

/**
 * Compose-friendly fallback for environments without a system tray.
 *
 * The desktop module binds [Notifier] to this implementation when the tray is unavailable;
 * [ui.App] collects [notifications] and renders them as banners.
 */
class InAppNotifier : Notifier {

    private val _notifications = MutableStateFlow<List<InAppReminderNotification>>(emptyList())
    val notifications: StateFlow<List<InAppReminderNotification>> = _notifications.asStateFlow()

    override fun notify(reminder: Reminder, event: Event) {
        if (_notifications.value.any { it.reminderId == reminder.id }) return
        _notifications.update { current ->
            current + InAppReminderNotification(
                reminderId = reminder.id,
                event = event,
                title = event.title,
                body = OffsetFormat.notificationBody(reminder.offsetMinutes, event.startTime),
            )
        }
    }

    /** Removes a banner, either because the user dismissed it or followed it. */
    fun dismiss(reminderId: Long) {
        _notifications.update { current -> current.filterNot { it.reminderId == reminderId } }
    }
}

/**
 * Broadcast channel for notification clicks.
 *
 * A click can come from the system tray (desktopMain) or an in-app banner. App.kt selects
 * the event's day; the desktop window also raises itself. An emission with a `null` event
 * is a plain "bring the window back" request.
 */
class ReminderActivationBus {

    private val _activations = MutableSharedFlow<Event?>(extraBufferCapacity = 8)
    val activations: SharedFlow<Event?> = _activations.asSharedFlow()

    fun activate(event: Event?) {
        _activations.tryEmit(event)
    }
}
