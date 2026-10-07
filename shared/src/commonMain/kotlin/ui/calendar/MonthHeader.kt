package ui.calendar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.core.CalendarMonth

@Composable
fun MonthHeader(month: CalendarMonth) {
    val monthName = month.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val year = month.yearMonth.year
    Text(
        text = "$monthName $year",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .semantics { heading() },
        textAlign = TextAlign.Center
    )
}
