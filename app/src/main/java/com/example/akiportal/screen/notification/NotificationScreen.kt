package com.example.akiportal.screen.notification

import android.net.Uri
import android.util.Log
import kotlin.math.abs
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

// Material swipe & icons
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon as M3Icon

// Material3 UI components
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.ui.theme.BackgroundDark
import com.example.akiportal.ui.theme.BluePrimary
import com.example.akiportal.ui.theme.RedPrimary
import com.example.akiportal.ui.theme.GreenPrimary
import com.example.akiportal.ui.theme.White
import com.example.akiportal.ui.theme.LightGray

import com.example.akiportal.model.Material
import com.example.akiportal.model.Maintenance
import com.example.akiportal.model.Machine
import com.example.akiportal.model.NotificationItem
import com.example.akiportal.model.NotificationType
import com.example.akiportal.screen.material.MaterialDetailDialog
import com.example.akiportal.viewmodel.MaterialViewModel
import com.example.akiportal.viewmodel.MaintenanceViewModel
import com.example.akiportal.viewmodel.MachineViewModel
import com.example.akiportal.viewmodel.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

private const val TAG = "NotificationScreen"

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableNotificationCard(
    item: NotificationItem,
    onDismiss: (NotificationItem) -> Unit,
    onMoveToBottom: (NotificationItem) -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberDismissState()
    val arrowRotation by animateFloatAsState(
        targetValue = if (dismissState.targetValue == DismissValue.DismissedToStart) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(dismissState.currentValue) {
        when {
            dismissState.isDismissed(DismissDirection.StartToEnd) -> onDismiss(item)
            dismissState.isDismissed(DismissDirection.EndToStart) -> onMoveToBottom(item)
        }
    }

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
        background = {
            val bg = when (dismissState.dismissDirection) {
                DismissDirection.StartToEnd -> Color.Red
                DismissDirection.EndToStart -> Color.Yellow
                else -> Color.Transparent
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(16.dp),
                contentAlignment = when (dismissState.dismissDirection) {
                    DismissDirection.StartToEnd -> Alignment.CenterStart
                    DismissDirection.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                when (dismissState.dismissDirection) {
                    DismissDirection.StartToEnd -> M3Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = Color.White
                    )
                    DismissDirection.EndToStart -> M3Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Alta At",
                        modifier = Modifier.rotate(arrowRotation),
                        tint = Color.Black
                    )
                    else -> {}
                }
            }
        },
        dismissContent = {
            NotificationCard(
                item = item,
                onDismiss = {},
                onClick = onClick
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(
    navController: NavHostController,
    notificationViewModel: NotificationsViewModel = viewModel(),
    materialViewModel: MaterialViewModel = viewModel(),
    machineViewModel: MachineViewModel = viewModel(),
    maintenanceViewModel: MaintenanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val companyId = FirebaseAuth.getInstance().currentUser?.uid
    if (companyId == null) return


    LaunchedEffect(Unit) {
        val companyId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        materialViewModel.loadMaterials(context, companyId)
        machineViewModel.loadMachines(companyId)
    }


    val allNotifications by notificationViewModel.notificationList
        .collectAsState(initial = emptyList())
    val materials by materialViewModel.materials
        .collectAsState(initial = emptyList())
    val machines by machineViewModel.machines
        .collectAsState(initial = emptyList())
    val maintenances by maintenanceViewModel.maintenances
        .collectAsState(initial = emptyList())


    var selectedMaterial by remember { mutableStateOf<Material?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<NotificationType?>(null) }

    LaunchedEffect(searchQuery, selectedType) {
        Log.d(TAG, "Filter: '$searchQuery', type=$selectedType")
    }

    val filtered = allNotifications.filter {
        (selectedType == null || it.type == selectedType) &&
                it.message.contains(searchQuery, ignoreCase = true)
    }
    val persistent = filtered.filter { it.isPersistent }
    val sorted = filtered
        .filter { !it.isPersistent }
        .sortedByDescending { it.timestamp }

    Column(
        Modifier.fillMaxSize().background(BackgroundDark)
    ) {
        RedTopBar(title = "Bildirim Geçmişi") {
            notificationViewModel.clearAll()
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Ara...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            DropdownMenuFilter(selectedType) { selectedType = it }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (persistent.isNotEmpty()) {
                stickyHeader {
                    Text("Sabit Bildirimler", color = LightGray, modifier = Modifier.padding(4.dp))
                }
                items(persistent, key = { it.id }) { item ->
                    SwipeableNotificationCard(
                        item = item,
                        onDismiss = { notificationViewModel.removeNotification(item) },
                        onMoveToBottom = { notificationViewModel.moveNotificationToBottom(item) },
                        onClick = { navigateByNotification(item, materials, machines, maintenances, navController) }
                    )
                }
            }

            if (sorted.isNotEmpty()) {
                stickyHeader {
                    Text("Diğer Bildirimler", color = LightGray, modifier = Modifier.padding(4.dp))
                }
                items(sorted, key = { it.id }) { item ->
                    SwipeableNotificationCard(
                        item = item,
                        onDismiss = { notificationViewModel.removeNotification(item) },
                        onMoveToBottom = { notificationViewModel.moveNotificationToBottom(item) },
                        onClick = { navigateByNotification(item, materials, machines, maintenances, navController) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Bildirim bulunamadı.",
                        color = LightGray,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        selectedMaterial?.let { mat ->
            MaterialDetailDialog(
                material = mat,
                onDismiss = { selectedMaterial = null },
                onSaveEdit = {
                    materialViewModel.updateMaterial(it, companyId)
                    selectedMaterial = null
                },
                onDelete = {
                    materialViewModel.deleteMaterial(it, companyId)
                    selectedMaterial = null
                }
            )
        }
    }
}

private fun navigateByNotification(
    item: NotificationItem,
    materials: List<Material>,
    machines: List<Machine>,
    maintenances: List<Maintenance>,
    navController: NavHostController
) {
    when {
        item.id.startsWith("lowstock_") -> materials.find { it.code == item.id.removePrefix("lowstock_") }?.let {
            navController.navigate("materialDetail/${Uri.encode(Gson().toJson(it))}") }
        item.id.startsWith("overdue_") || item.id.startsWith("upcoming") -> machines.find { it.id == item.id.substringAfter('_') }?.let {
            navController.navigate("machineDetail/${Uri.encode(Gson().toJson(it))}") }
        item.id.startsWith("done_") -> maintenances.find { it.id == item.id.removePrefix("done_") }?.let {
            navController.navigate("maintenanceDetail/${Uri.encode(Gson().toJson(it))}") }
        item.id.startsWith("today_") -> navController.navigate("dailyMaintenance")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon = when (item.type) {
        NotificationType.WARNING -> Icons.Default.Warning
        NotificationType.INFO -> Icons.Default.Info
        NotificationType.SUCCESS -> Icons.Default.CheckCircle
    }
    val iconColor = when (item.type) {
        NotificationType.WARNING -> RedPrimary
        NotificationType.INFO -> BluePrimary
        NotificationType.SUCCESS -> GreenPrimary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(Unit) { detectHorizontalDragGestures { _, drag -> if (abs(drag) > 50) onDismiss() } }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            M3Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(item.message, color = White)
                Text(item.timestamp, color = LightGray)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DropdownMenuFilter(selected: NotificationType?, onSelected: (NotificationType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf<NotificationType?>(null) + NotificationType.values().toList()
    OutlinedButton(onClick = { expanded = true }) {
        Text(selected?.name ?: "Tümü")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach {
            DropdownMenuItem(
                text = { Text(it?.name ?: "Tümü") },
                onClick = { onSelected(it); expanded = false }
            )
        }
    }
}
