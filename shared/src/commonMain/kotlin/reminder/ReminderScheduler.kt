package reminder

import data.ReminderRepository
import data.startInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Delivers reminders by sleeping until the next due deadline instead of polling on a fixed
 * interval, so a notification fires at its due moment rather than at the next tick.
 *
 * Each pass queries pending reminders, fires/silences those that are due, then sleeps until
 * the earliest remaining deadline. The sleep ends early when [ReminderRepository.changes]
 * signals that the reminder set moved (event saved, offsets edited, fired bookkeeping
 * cleared). With no deadline known it rescans every [maxIdleSleep] as a safety net.
 *
 * The due instant is always recomputed as `event.start - reminder.offset`, so editing an
 * event's start time moves its reminders with it. Reminders whose due time fell outside
 * the grace window while the app was closed are marked fired without notifying, which
 * avoids a wall of stale notifications on startup. Fire-failure retries on the next pass.
 *
 */
class ReminderScheduler(
    private val reminders: ReminderRepository,
    private val notifier: Notifier,
    private val clock: () -> Instant = { Clock.System.now() },
    private val graceWindow: Duration = 60.minutes,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val maxIdleSleep: Duration = 1.minutes,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /** Starts the deadline loop; the first pass runs immediately. Safe to call twice. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                val zone = this@ReminderScheduler.timeZone()
                // Captured before the pass: a change racing with it makes the wait below
                // return immediately, so nothing is missed.
                val version = reminders.changes.value
                val now = clock()
                val nextDeadline = pollOnce(now, zone)
                val wait = if (nextDeadline == null) {
                    maxIdleSleep
                } else {
                    (nextDeadline - now).coerceAtLeast(Duration.ZERO)
                }

                // Wake exactly at the deadline, or earlier when the reminder set changes.
                withTimeoutOrNull(wait) {
                    reminders.changes.dropWhile { it == version }.first()
                }
            }
        }
    }

    /** Cancels the deadline loop. */
    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * One pass. Fires or silences every due reminder and returns the next pending deadline
     * strictly after [now], or `null` when nothing is scheduled ahead.
     */
    suspend fun pollOnce(
        now: Instant = clock(),
        timeZone: TimeZone = this.timeZone(),
    ): Instant? {
        val pending = reminders.dueReminders()

        val upcoming = mutableListOf<Instant>()
        pending.forEach { item ->
            val dueAt = item.event.startInstant(timeZone) - item.reminder.offsetMinutes.minutes
            when {
                // Still ahead of us; the scheduler sleeps until this deadline.
                dueAt > now -> upcoming += dueAt

                // Too old to be useful: silence it without notifying.
                dueAt < now - graceWindow -> {
                    reminders.markFired(item.reminder.id, now)
                }

                else -> {
                    val notified = try {
                        notifier.notify(item.reminder, item.event)
                        true
                    } catch (_: Exception) {
                        false
                    }
                    if (notified) reminders.markFired(item.reminder.id, now)
                }
            }
        }
        return upcoming.minOrNull()
    }
}
