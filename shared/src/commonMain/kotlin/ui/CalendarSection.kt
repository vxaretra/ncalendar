package ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private fun YearMonth.minus(period: DatePeriod): YearMonth =
    minus(period.years * 12 + period.months, DateTimeUnit.MONTH)

private fun YearMonth.plus(period: DatePeriod): YearMonth =
    plus(period.years * 12 + period.months, DateTimeUnit.MONTH)

@Composable
fun CalendarSection(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember(selectedDate) {
        YearMonth(selectedDate.year, selectedDate.month)
    }

    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minus(DatePeriod(months = 12)),
        endMonth = currentMonth.plus(DatePeriod(months = 12)),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = DayOfWeek.MONDAY
    )

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
                onClick = { onDateSelected(day.date) }
            )
        },
        monthHeader = { month ->
            MonthHeader(month = month)
        }
    )
}
