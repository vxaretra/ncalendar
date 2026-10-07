package ui.layout

/** How the calendar and the day detail are arranged for the current window width. */
enum class LayoutMode {
    /** Calendar on top, day detail below (narrow windows). */
    Stacked,

    /** Calendar on the left, day detail on the right. */
    SideBySide,
}

/**
 * Result of [layoutForWidth].
 *
 * [calendarWeight] and [detailWeight] are fractions of the available space along the
 * main axis of the chosen [mode] (a row for [LayoutMode.SideBySide], a column for
 * [LayoutMode.Stacked]).
 */
data class LayoutSpec(
    val mode: LayoutMode,
    val calendarWeight: Float,
    val detailWeight: Float,
)

/**
 * Pure layout decision for the app shell. Contains no Compose UI dependencies so it can
 * be reasoned about (and exercised) on its own; callers pass `maxWidth.value` from
 * [androidx.compose.foundation.layout.BoxWithConstraints].
 *
 * Rules:
 *  - width < 600dp            -> stacked: calendar row on top, day detail below
 *  - 600dp <= width < 900dp   -> side-by-side 45 / 55
 *  - width >= 900dp           -> side-by-side 40 / 60 (desktop default)
 */
fun layoutForWidth(maxWidthDp: Float): LayoutSpec = when {
    maxWidthDp < 600f -> LayoutSpec(
        mode = LayoutMode.Stacked,
        calendarWeight = 0.55f,
        detailWeight = 0.45f,
    )

    maxWidthDp < 900f -> LayoutSpec(
        mode = LayoutMode.SideBySide,
        calendarWeight = 0.45f,
        detailWeight = 0.55f,
    )

    else -> LayoutSpec(
        mode = LayoutMode.SideBySide,
        calendarWeight = 0.40f,
        detailWeight = 0.60f,
    )
}
