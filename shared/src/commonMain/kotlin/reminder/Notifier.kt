package reminder

import data.Event
import data.Reminder

/**
 * Sink for due reminders.
 *
 * Implemented per platform: commonMain only knows this interface, so the scheduler has no
 * dependency on the system tray or any other display mechanism.
 */
fun interface Notifier {
    fun notify(reminder: Reminder, event: Event)
}
