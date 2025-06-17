package com.example.akiportal.screen.calendar

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.akiportal.model.Maintenance
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.ui.theme.*
import com.example.akiportal.util.CalendarHelper
import com.example.akiportal.util.CalendarMonthView
import com.example.akiportal.viewmodel.MaintenanceViewModel
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.gson.Gson
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PlannedMaintenanceScreen(
    navController: NavController,
    viewModel: MaintenanceViewModel = viewModel(),
    onAddClick: () -> Unit
) {
    var plannedList by remember { mutableStateOf(emptyList<Maintenance>()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getPlannedList { plannedList = it }
    }

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val eventsByDate = plannedList
        .filter { it.plannedDate.isNotBlank() }
        .groupBy { LocalDate.parse(it.plannedDate, formatter) }

    val getEventCountForDate: (LocalDate) -> Int? = { date ->
        eventsByDate[date]?.size
    }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        val context = LocalContext.current

        RedTopBar(
            title = CalendarHelper.getMonthLabel(currentMonth),
            showMenu = true,
            menuContent = {
                DropdownMenuItem(
                    text = { Text("Manuel Bakım / Arıza Ekle") },
                    onClick = { navController.navigate("manualMaintenanceScreen") }
                )
                DropdownMenuItem(
                    text = { Text("Tarihe Git") },
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val sel = LocalDate.of(year, month + 1, day)
                                currentMonth = YearMonth.of(sel.year, sel.monthValue)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )
            }
        )

        CalendarPagerWrapper(
            currentMonth = currentMonth,
            selectedDate = selectedDate ?: LocalDate.now(),
            eventsByDate = eventsByDate,
            getEventCountForDate = getEventCountForDate,
            onDateClick = { selectedDate = it },
            onMonthChange = { currentMonth = it }
        )

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            thickness = 1.dp,
            color = LightGray
        )

        selectedDate?.let { date ->
            val formatted = date.format(formatter)
            val dailyList = plannedList.filter { it.plannedDate == formatted }
            val selectionMode = remember { mutableStateOf(false) }
            val selectedItems = remember { mutableStateListOf<Maintenance>() }

            Column {
                if (selectionMode.value && selectedItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            val json = Uri.encode(Gson().toJson(selectedItems))
                            navController.navigate("bulkCompletion/$json")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Seçilenleri Tamamla")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(dailyList) { maintenance ->
                        val json = Uri.encode(Gson().toJson(maintenance))
                        val isClickable = maintenance.status
                            .trim()
                            .lowercase() in listOf("planlandı", "hazırlandı", "tamamlandı")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = {
                                        selectionMode.value = true
                                        if (!selectedItems.contains(maintenance))
                                            selectedItems.add(maintenance)
                                    })
                                }
                                .then(
                                    if (isClickable) Modifier.clickable {
                                        if (selectionMode.value) {
                                            if (selectedItems.contains(maintenance)) {
                                                selectedItems.remove(maintenance)
                                                if (selectedItems.isEmpty())
                                                    selectionMode.value = false
                                            } else selectedItems.add(maintenance)
                                        } else {
                                            when (maintenance.status.trim().lowercase()) {
                                                "planlandı" ->
                                                    navController.navigate("preparationDetail/$json")
                                                "hazırlandı" ->
                                                    navController.navigate("completionDetail/$json")
                                                "tamamlandı" -> // ← burada MaintenanceDetailScreen'e yönlendir
                                                    navController.navigate("maintenanceDetail/$json")
                                            }
                                        }
                                    } else Modifier
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedItems.contains(maintenance)) SoftBlue else CardDark
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Şirket: ${maintenance.companyName}", color = White)
                                Text("Makina: ${maintenance.machineName}", color = White)
                                Text("Seri No: ${maintenance.serialNumber}", color = White)
                                Text("Açıklama: ${maintenance.description}", color = LightGray)
                                Text("Ön Not: ${maintenance.preMaintenanceNote}", color = LightGray)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarPagerWrapper(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<Any>>,
    getEventCountForDate: (LocalDate) -> Int?,
    onDateClick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    // Sabit referans ay
    val baseMonth = remember { currentMonth }

    // 12 ay geriye ve ileri
    val monthRange = remember {
        CalendarHelper.generateMonthRange(centerMonth = baseMonth, past = 12, future = 12)
    }

    // Pager başlangıç sayfası
    val initialPage = monthRange.indexOf(baseMonth).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage)

    // Sadece sayfa index’i gerçekten değiştiğinde onMonthChange’i tetikle
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .map { monthRange[it] }
            .distinctUntilChanged()
            .collect { onMonthChange(it) }
    }

    HorizontalPager(
        count = monthRange.size,
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) { page ->
        val pagerMonth = monthRange[page]
        val monthEvents = eventsByDate.filterKeys {
            it.year == pagerMonth.year && it.month == pagerMonth.month
        }

        CalendarMonthView(
            month = pagerMonth,
            selectedDate = selectedDate,
            events = monthEvents,
            getEventCountForDate = getEventCountForDate,
            onDateSelected = onDateClick
        )
    }
}
