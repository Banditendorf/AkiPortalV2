package com.example.akiportal.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.akiportal.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarMonthView(
    month: YearMonth,
    selectedDate: LocalDate?,
    events: Map<LocalDate, List<Any>>,
    getEventCountForDate: (LocalDate) -> Int?,
    onDateSelected: (LocalDate) -> Unit
) {
    val days = listOf("PZT", "SAL", "ÇAR", "PER", "CUM", "CMT", "PAZ")

    val cells = remember(month) {
        val firstDayOfMonth = month.atDay(1)
        // Düzeltilmiş: Pazartesi=1…Pazar=7 → 0…6 arası
        val dayOfWeekValue = (firstDayOfMonth.dayOfWeek.value + 6) % 7
        val totalDays = month.lengthOfMonth()

        buildList<LocalDate?> {
            repeat(dayOfWeekValue) { add(null) }
            addAll((1..totalDays).map { month.atDay(it) })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            days.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    color = LightGray,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 480.dp),
            userScrollEnabled = false
        ) {
            items(cells) { date ->
                if (date == null) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                    )
                } else {
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()
                    val eventCount = getEventCountForDate(date) ?: 0

                    val bgColor = when {
                        isSelected -> selectedColor
                        isToday -> todayColor
                        else -> dayBgColor
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(bgColor)
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = White,
                                fontSize = 20.sp
                            )
                            if (eventCount > 0) {
                                Text(
                                    text = eventCount.toString(),
                                    color = SoftBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
