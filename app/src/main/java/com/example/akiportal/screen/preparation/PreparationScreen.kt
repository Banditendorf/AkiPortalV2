package com.example.akiportal.screen.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.model.*
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.ui.theme.*
import com.example.akiportal.viewmodel.MaintenanceViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.akiportal.model.NotificationType
import com.example.akiportal.model.NotificationItem
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun PreparationScreen(
    maintenanceViewModel: MaintenanceViewModel = viewModel(),
    onMaintenanceClick: (Maintenance) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    var maintenanceList by remember { mutableStateOf(emptyList<Maintenance>()) }
    val selectedToDelete = remember { mutableStateOf<Maintenance?>(null) }
    val showManualAddDialog = remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun refreshData() {
        val today = LocalDate.now()
        val firestore = FirebaseFirestore.getInstance()

        val snapshot = firestore.collection("plannedMaintenances")
            .get().await()

        val allMaintenances = snapshot.mapNotNull { it.toObject(Maintenance::class.java) }

        val filtered = allMaintenances.filter {
            try {
                it.plannedDate.isNotBlank() &&
                        LocalDate.parse(it.plannedDate, formatter).isAfter(today.minusDays(1))
            } catch (e: Exception) {
                false
            }
        }.sortedWith(
            compareByDescending<Maintenance> { it.parts.any { p -> p.prepared == false || p.prepared == null } }
                .thenByDescending { it.plannedDate }
        )

        maintenanceList = filtered
    }

    LaunchedEffect(true) {
        coroutineScope.launch { refreshData() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RedTopBar("Malzeme Hazırlıkları")

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menü", tint = White)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Listeyi Sil") },
                        onClick = {
                            expanded = false
                            selectedToDelete.value = maintenanceList.firstOrNull()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Manuel Liste Ekle") },
                        onClick = {
                            expanded = false
                            showManualAddDialog.value = true
                        }
                    )
                }
            }
        }

        if (maintenanceList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hazırlanacak liste yok", fontSize = 20.sp, color = White)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (notPrepared, prepared) = maintenanceList.partition { it.status == "planlandı" }

                notPrepared.forEachIndexed { index, maintenance ->
                    item {
                        MaintenanceCard(maintenance = maintenance, onClick = { onMaintenanceClick(maintenance) })
                    }
                    item {
                        Divider(color = Gray.copy(alpha = 0.2f), thickness = 1.dp)
                    }
                }

                if (prepared.isNotEmpty()) {
                    item {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Green,
                            thickness = 1.dp
                        )
                    }
                }

                prepared.forEach { maintenance ->
                    item {
                        MaintenanceCard(
                            maintenance = maintenance,
                            onClick = { onMaintenanceClick(maintenance) },
                            highlight = true
                        )
                    }
                }
            }
        }
    }

    selectedToDelete.value?.let { itemToDelete ->
        AlertDialog(
            onDismissRequest = { selectedToDelete.value = null },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseFirestore.getInstance()
                        .collection("plannedMaintenances")
                        .document(itemToDelete.id)
                        .delete()
                        .addOnSuccessListener {
                            selectedToDelete.value = null
                            coroutineScope.launch { refreshData() }
                        }
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { selectedToDelete.value = null }) { Text("İptal") }
            },
            title = { Text("Listeyi Sil") },
            text = { Text("Bu bakım planını silmek istediğinize emin misiniz?") }
        )
    }

    if (showManualAddDialog.value) {
        ManualAddDialog(onDismiss = { showManualAddDialog.value = false })
    }
}

@Composable
fun MaintenanceCard(
    maintenance: Maintenance,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Green.copy(alpha = 0.15f) else CardDark
        ),
        border = if (highlight) BorderStroke(1.dp, Green) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Şirket: ${maintenance.companyName}", color = White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Makine: ${maintenance.machineName}", color = LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Seri No: ${maintenance.serialNumber}", color = LightGray, fontSize = 13.sp)

            if (highlight && maintenance.note.contains("Hazırlayan")) {
                val hazirlayan = maintenance.note.split("Hazırlayan:").getOrNull(1)?.trim() ?: "-"
                Spacer(modifier = Modifier.height(4.dp))
                Text("Hazırlayan: $hazirlayan", color = LightGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ManualAddDialog(onDismiss: () -> Unit) {
    var machineName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    val viewModel: MaintenanceViewModel = viewModel()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val now = SimpleDateFormat("dd.MM.yyyy - HH:mm").format(Date())
                val id = UUID.randomUUID().toString()

                val newNotification = NotificationItem(
                    id = "malzeme_listesi_$id",
                    message = "Yeni bir malzeme hazırlık listesi yayınlandı",
                    type = NotificationType.INFO,
                    timestamp = now,
                    isPersistent = true
                )

                viewModel.addNotification(newNotification)

                val dummyMaintenance = Maintenance(
                    id = id,
                    machineId = "",
                    machineName = machineName,
                    plannedDate = date,
                    parts = emptyList(),
                    note = "",
                    status = "planlandı",
                    companyId = "",
                    companyName = "",
                    serialNumber = ""
                )

                FirebaseFirestore.getInstance().collection("plannedMaintenances")
                    .document(id)
                    .set(dummyMaintenance)
                    .addOnSuccessListener {
                        onDismiss()
                    }
            }) { Text("Ekle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
        title = { Text("Manuel Liste Ekle") },
        text = {
            Column {
                OutlinedTextField(
                    value = machineName,
                    onValueChange = { machineName = it },
                    label = { Text("Makine Adı") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tarih (dd.MM.yyyy)") }
                )
            }
        }
    )
}