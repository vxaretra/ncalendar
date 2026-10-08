package reminder

import data.ReminderRepository
import data.startInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Polls pending reminders on a fixed interval and hands due ones to the [Notifier].
 *
 * The due instant is always recomputed as `event.start - reminder.offset`, so editing an
 * event's start time moves its reminders with it. Reminders whose due time fell outside
 * the grace window while the app was closed are marked fired without notifying, which
 * avoids a wall of stale notifications on startup. Fire-failure retries on the next tick.
 *
 * Every poll tick and every detected reminder is logged through [log] (stdout by default).
 */
class ReminderScheduler(
    private val reminders: ReminderRepository,
    private val notifier: Notifier,
    private val clock: () -> Instant = { Clock.System.now() },
    private val pollInterval: Duration = 10.seconds,
    private val graceWindow: Duration = 60.minutes,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val log: (String) -> Unit = { println(it) },
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /** Starts the poll loop; the first tick runs immediately. Safe to call twice. */
    fun start() {
        if (job?.isActive == true) return
        log("[ReminderScheduler] Started - polling every $pollInterval (grace window $graceWindow)")
        job = scope.launch {
            while (isActive) {
                pollOnce()
                delay(pollInterval)
            }
        }
    }

    /** Cancels the poll loop. */
    fun stop() {
        job?.cancel()
        job = null
        log("[ReminderScheduler] Stopped")
    }

    /** One poll pass. Public so callers can trigger a deterministic tick (tests, manual refresh). */
    suspend fun pollOnce(
        now: Instant = clock(),
        timeZone: TimeZone = this.timeZone(),
    ) {
        log("[ReminderScheduler] Poll tick at ${formatTimestamp(now, timeZone)}")

        val pending = reminders.dueReminders()
        if (pending.isNotEmpty()) {
            val summary = pending.joinToString { item ->
                "#${item.reminder.id} \"${item.event.title}\" (${OffsetFormat.format(item.reminder.offsetMinutes)})"
            }
            log("[ReminderScheduler] Detected ${pending.size} pending reminder(s): $summary")
        }

        pending.forEach { item ->
            val dueAt = item.event.startInstant(timeZone) - item.reminder.offsetMinutes.minutes
            when {
                // Still ahead of us; the next ticks will pick it up.
                dueAt > now -> Unit

                // Too old to be useful: silence it without notifying.
                dueAt < now - graceWindow -> {
                    log(
                        "[ReminderScheduler] Silencing stale reminder #${item.reminder.id} " +
                            "\"${item.event.title}\" - due ${formatTimestamp(dueAt, timeZone)}, " +
                            "outside the $graceWindow grace window"
                    )
                    reminders.markFired(item.reminder.id, now)
                }

                else -> {
                    val notified = try {
                        log(
                            "[ReminderScheduler] Firing reminder #${item.reminder.id} " +
                                "\"${item.event.title}\" (${OffsetFormat.format(item.reminder.offsetMinutes)}), " +
                                "event starts ${formatTimestamp(item.event.startInstant(timeZone), timeZone)}"
                        )
                        notifier.notify(item.reminder, item.event)
                        true
                    } catch (error: Exception) {
                        log(
                            "[ReminderScheduler] Reminder #${item.reminder.id} could not be delivered " +
                                "(${error.message}); will retry on the next tick"
                        )
                        false
                    }
                    if (notified) reminders.markFired(item.reminder.id, now)
                }
            }
        }
    }

    /** Local `yyyy-MM-dd HH:mm` timestamp for logs. */
    private fun formatTimestamp(instant: Instant, timeZone: TimeZone): String {
        val dateTime = instant.toLocalDateTime(timeZone)
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "${dateTime.date} $hour:$minute"
    }
}
