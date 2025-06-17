package com.example.akiportal.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarHelper {

    // Bugünkü tarih
    fun today(): LocalDate = LocalDate.now()

    // Şu anki yıl-ay
    fun currentMonth(): YearMonth = YearMonth.now()

    // Ay kaç gün çeker
    fun getDaysInMonth(month: YearMonth): Int = month.lengthOfMonth()

    // Ayın 1’i haftanın hangi gününe denk geliyor (0 = Pazartesi, 6 = Pazar)
    fun getStartDayOfWeek(month: YearMonth): Int {
        val firstDay = month.atDay(1)
        return (firstDay.dayOfWeek.value + 6) % 7  // Pazartesi → 0, Pazar → 6
    }

    // Ay adı ("Nisan 2025" şeklinde)
    fun getMonthLabel(month: YearMonth): String {
        val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("tr"))
        return month.atDay(1).format(formatter).replaceFirstChar { it.uppercase() }
    }

    // Gün hücrelerini hesaplar (baştaki boşluklar dahil)
    fun getCalendarCells(month: YearMonth): List<LocalDate?> {
        val startDay = getStartDayOfWeek(month)
        val totalDays = getDaysInMonth(month)
        val totalCells = startDay + totalDays

        val fullCells = if (totalCells % 7 == 0) totalCells else totalCells + (7 - totalCells % 7)

        return (0 until fullCells).map { index ->
            if (index < startDay || index >= startDay + totalDays) null
            else month.atDay(index - startDay + 1)
        }
    }

    // Tarihi "gg.aa.yyyy" formatına çevir
    fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    // Kaydırılabilir ay listesi üret
    fun generateMonthRange(
        centerMonth: YearMonth = currentMonth(),
        past: Int = 12,
        future: Int = 12
    ): List<YearMonth> {
        return (-past..future).map { offset -> centerMonth.plusMonths(offset.toLong()) }
    }

    // Bugün mü?
    fun isToday(date: LocalDate?): Boolean = date == today()

    // Aynı gün mü?
    fun isSameDay(a: LocalDate?, b: LocalDate?): Boolean = a != null && b != null && a == b

    // Aynı ay mı?
    fun isSameMonth(a: YearMonth, b: YearMonth): Boolean =
        a.year == b.year && a.month == b.month
}
