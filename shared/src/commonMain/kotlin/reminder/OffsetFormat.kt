package reminder

import kotlinx.datetime.LocalTime
import ui.formatTime

/** Input unit for the reminder editor. */
enum class ReminderUnit(val label: String, val minutes: Int) {
    MINUTES("minutes", 1),
    HOURS("hours", 60),
    DAYS("days", 1_440),
}

/** Soft cap the reminder UI enforces per event. */
const val MAX_REMINDERS_PER_EVENT = 3

/**
 * Pure helpers for turning reminder offsets into text and back.
 *
 * Offsets are minutes *before* the event start, so a larger offset means an earlier
 * notification ("1 day before" fires before "10 min before").
 */
object OffsetFormat {

    /** Row label inside the reminder list, e.g. `10 min before`, `1 hour before`, `1 day before`. */
    fun format(offsetMinutes: Int): String = when {
        offsetMinutes <= 0 -> "At start"
        offsetMinutes % MINUTES_PER_DAY == 0 -> unitText(offsetMinutes / MINUTES_PER_DAY, "day") + " before"
        offsetMinutes % 60 == 0 -> unitText(offsetMinutes / 60, "hour") + " before"
        else -> "$offsetMinutes min before"
    }

    /**
     * Notification body. Reads naturally for whole days, whole hours and minutes under an
     * hour; anything else (e.g. 90 minutes) falls back to the event start time.
     */
    fun notificationBody(offsetMinutes: Int, startTime: LocalTime): String = when {
        offsetMinutes <= 0 -> "Starting now"
        offsetMinutes % MINUTES_PER_DAY == 0 -> "In " + unitText(offsetMinutes / MINUTES_PER_DAY, "day")
        offsetMinutes % 60 == 0 && offsetMinutes < MINUTES_PER_DAY -> "In " + unitText(offsetMinutes / 60, "hour")
        offsetMinutes < 60 -> "In " + unitText(offsetMinutes, "minute")
        else -> "Starts at ${formatTime(startTime)}"
    }

    private fun unitText(count: Int, unit: String): String =
        if (count == 1) "1 $unit" else "$count ${unit}s"

    private const val MINUTES_PER_DAY = 1_440
}

/** Parses the editor's number field and unit into an offset, or `null` when invalid. */
fun parseOffsetMinutes(text: String, unit: ReminderUnit): Int? {
    val count = text.trim().toIntOrNull() ?: return null
    if (count <= 0) return null
    val total = count.toLong() * unit.minutes
    return if (total in 1..Int.MAX_VALUE.toLong()) total.toInt() else null
}

/**
 * Adds an offset to a reminder list, deduping silently and refusing anything past the cap.
 * The returned list is sorted furthest-out first.
 */
fun addReminderOffset(existing: List<Int>, offsetMinutes: Int): List<Int> {
    if (offsetMinutes <= 0) return existing
    if (existing.size >= MAX_REMINDERS_PER_EVENT) return existing
    if (offsetMinutes in existing) return existing
    return (existing + offsetMinutes).sortedDescending()
}

/** Normalizes a persisted reminder list: positive, deduped, capped, furthest-out first. */
fun normalizeReminderOffsets(offsets: Iterable<Int>): List<Int> =
    offsets.filter { it > 0 }
        .distinct()
        .sortedDescending()
        .take(MAX_REMINDERS_PER_EVENT)
