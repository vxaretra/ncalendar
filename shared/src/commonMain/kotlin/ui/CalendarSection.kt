package ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.ContentHeightMode
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import ui.calendar.DayCell
import ui.calendar.MonthHeader

/** How many months before/after the selection the calendar starts with. */
private const val MONTHS_AROUND_SELECTION = 12

private fun YearMonth.minus(period: DatePeriod): YearMonth =
    minus(period.years * 12 + period.months, DateTimeUnit.MONTH)

private fun YearMonth.plus(period: DatePeriod): YearMonth =
    plus(period.years * 12 + period.months, DateTimeUnit.MONTH)

/**
 * Vertical month calendar with event dots.
 *
 * The calendar state is created once (it is no longer rebuilt for every selected date);
 * when [selectedDate] moves to another month, the effect below animates the scroll to
 * that month. The effect is keyed to the *year-month* so moving the selection inside the
 * visible month does not trigger repeated scrolling.
 */
@Composable
fun CalendarSection(
    selectedDate: LocalDate,
    eventDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialMonth = remember { YearMonth(selectedDate.year, selectedDate.month) }

    val calendarState = rememberCalendarState(
        startMonth = initialMonth.minus(DatePeriod(months = MONTHS_AROUND_SELECTION)),
        endMonth = initialMonth.plus(DatePeriod(months = MONTHS_AROUND_SELECTION)),
        firstVisibleMonth = initialMonth,
        firstDayOfWeek = DayOfWeek.MONDAY
    )

    val selectedMonth = YearMonth(selectedDate.year, selectedDate.month)

    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(selectedMonth) {
        if (isInitialComposition) {
            // The state is already positioned on the initial month.
            isInitialComposition = false
            return@LaunchedEffect
        }
        // Widen the scrollable range if the selection walks past the initial window,
        // then bring its month to the top of the viewport.
        if (selectedMonth < calendarState.startMonth) calendarState.startMonth = selectedMonth
        if (selectedMonth > calendarState.endMonth) calendarState.endMonth = selectedMonth
        calendarState.animateScrollToMonth(selectedMonth)
    }

    VerticalCalendar(
        state = calendarState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentHeightMode = ContentHeightMode.Wrap,
        dayContent = { day ->
            DayCell(
                day = day,
                isSelected = day.date == selectedDate,
                hasEvents = day.date in eventDates,
                onClick = { onDateSelected(day.date) }
            )
        },
        monthHeader = { month ->
            MonthHeader(month = month)
        }
    )
}
