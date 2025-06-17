package com.example.akiportal.screen.machine

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.akiportal.model.*
import com.example.akiportal.ui.theme.*
import com.example.akiportal.ui.ui.RedTopBar
import androidx.compose.ui.platform.LocalContext
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.viewmodel.MaintenanceViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.example.akiportal.screen.machine.CompressorDialog
import com.example.akiportal.screen.machine.DryerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.google.gson.Gson

@Composable
fun MachineDetailScreen(
    machine: Machine,
    navController: NavController,
    maintenanceViewModel: MaintenanceViewModel = viewModel()
) {
    val isDryer = machine.type == "kurutucu"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context)
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    var maintenanceList by remember { mutableStateOf(listOf<Maintenance>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditMachineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(machine.id) {
        FirebaseFirestore.getInstance()
            .collection("plannedMaintenances")
            .whereEqualTo("machineId", machine.id)
            .get()
            .addOnSuccessListener { result ->
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
                maintenanceList = result.documents
                    .mapNotNull { it.toObject(Maintenance::class.java) }
                    .sortedByDescending { maintenance ->
                        try {
                            LocalDate.parse(maintenance.plannedDate, formatter)
                        } catch (e: Exception) {
                            LocalDate.MIN
                        }
                    }
            }
    }


    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        RedTopBar(title = machine.name, showMenu = true) {
            DropdownMenuItem(text = { Text("Makineyi Güncelle") }, onClick = {
                if (pm.canManageMachines()) {
                    showEditMachineDialog = true
                } else {
                    scope.launch { snackbarMessage = "Makine güncelleme yetkiniz yok." }
                }
            })
            DropdownMenuItem(text = { Text("Makineyi Sil") }, onClick = {
                if (pm.canManageMachines()) {
                    showDeleteConfirm = true
                } else {
                    scope.launch { snackbarMessage = "Makine silme yetkiniz yok." }
                }
            })
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Seri No: ${machine.serialNumber}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                if (isDryer) {
                    if (machine.dryerFilterCount > 0) {
                        InfoRow("Kurutucu Filtresi:", machine.dryerFilterCode, "${machine.dryerFilterCount} adet")
                    }
                    if (machine.oilCode == "Aktif Alümina") {
                        InfoRow("Aktif Alümina:", machine.oilCode, "${machine.oilLiter} kg")
                    }
                } else {
                    InfoRow("Yağ Filtresi:", machine.oilFilterCode, machine.oilFilterCount.toString())
                    InfoRow("Separatör:", machine.separatorCode, machine.separatorCount.toString())
                    InfoRow("Hava Filtresi:", machine.airFilterCode, machine.airFilterCount.toString())
                    InfoRow("Yağ:", machine.oilCode, "${machine.oilLiter} L")
                    InfoRow("Panel Filtresi:", machine.panelFilterSize, "")
                }

                InfoRow("Tahmini Çalışma Saati:", formatHoursToTime(machine.estimatedHours), "")
                InfoRow("Sıradaki Bakım Saati:", "${machine.nextMaintenanceHour} saat", "")

                machine.note.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Not: $it", color = Color.Black)
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // GEÇMİŞ BAŞLIĞI DAHİL BAŞLANGIÇ
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Geçmiş",
                    color = White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 0.dp) // padding(0) ile başlasın, istersen ayarla
                )
            }

            // Bakım Kartları
            items(maintenanceList) { maintenance ->
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
                val today = LocalDate.now()
                val maintenanceDate = try {
                    LocalDate.parse(maintenance.plannedDate, formatter)
                } catch (e: Exception) {
                    null
                }

                val responsibles = maintenance.responsibles.joinToString(", ").ifBlank { "-" }

                var menuExpanded by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                when (maintenance.status.trim().lowercase()) {
                                    "planlandı" -> {
                                        val json = Uri.encode(Gson().toJson(maintenance))
                                        navController.navigate("preparationDetail/$json")
                                    }
                                    "hazırlandı" -> {
                                        val json = Uri.encode(Gson().toJson(maintenance))
                                        navController.navigate("completionDetail/$json")
                                    }
                                    "tamamlandı" -> {
                                        val json = Uri.encode(Gson().toJson(maintenance))
                                        navController.navigate("maintenanceDetail/$json")
                                    }
                                }
                            },
                            onLongClick = { menuExpanded = true }
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = maintenance.description.ifBlank { "Açıklama yok" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tarih: ${maintenance.plannedDate}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sorumlular: $responsibles",
                            color = LightGray,
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Menü
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bakımı Sil") },
                                onClick = {
                                    menuExpanded = false
                                    if (pm.canManageMaintenance()) {
                                        showDeleteDialog = true
                                    } else {
                                        scope.launch { snackbarMessage = "Bakım silme yetkiniz yok." }
                                    }
                                }
                            )
                            if (maintenance.status.trim().lowercase() == "tamamlandı") {
                                DropdownMenuItem(
                                    text = { Text("Bakımı Düzenle") },
                                    onClick = {
                                        menuExpanded = false
                                        if (pm.canManageMaintenance()) {
                                            val json = Uri.encode(Gson().toJson(maintenance))
                                            navController.navigate("completionDetail/$json")
                                        } else {
                                            scope.launch { snackbarMessage = "Bakım düzenleme yetkiniz yok." }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Silme Onay Dialogu
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Bakım Silinsin mi?") },
                        text = { Text("Bu bakımı silmek istediğinizden emin misiniz?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                if (pm.canManageMaintenance()) {
                                    FirebaseFirestore.getInstance()
                                        .collection("plannedMaintenances")
                                        .document(maintenance.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            // State listesinden sil ve kullanıcıyı bilgilendir
                                            maintenanceList = maintenanceList.filter { it.id != maintenance.id }
                                            snackbarMessage = "Bakım kaydı silindi."
                                        }
                                        .addOnFailureListener {
                                            snackbarMessage = "Silme işlemi başarısız."
                                        }
                                } else {
                                    snackbarMessage = "Bakım silme yetkiniz yok."
                                }
                            }) {
                                Text("Evet")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("İptal")
                            }
                        }
                    )
                }
            }
        }
    }

    // 1) “Makineyi Sil” Onay Diyaloğu bloğunu izin kontrolü ile güncelledik
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            text = "Bu makineyi silmek istediğinize emin misiniz?",
            onConfirm = {
                showDeleteConfirm = false
                if (pm.canManageMachines()) {
                    FirebaseFirestore.getInstance()
                        .collection("machines")
                        .document(machine.id)
                        .delete()
                        .addOnSuccessListener {
                            snackbarMessage = "Makine başarıyla silindi!"
                            navController.popBackStack()
                        }
                        .addOnFailureListener {
                            snackbarMessage = "Makine silinirken hata oluştu."
                        }
                } else {
                    snackbarMessage = "Makine silme yetkiniz yok."
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

// 2) “Makineyi Güncelle” Diyaloğu bloğunu izin kontrolü ile güncelledik
    if (showEditMachineDialog) {
        if (pm.canManageMachines()) {
            when (machine.type) {
                "kompresör" -> CompressorDialog(
                    companyId   = machine.companyId,
                    companyName = machine.companyName,
                    machine     = machine,
                    onDismiss   = { showEditMachineDialog = false },
                    onCompleted = {
                        snackbarMessage      = "Makine başarıyla güncellendi!"
                        showEditMachineDialog = false
                    }
                )
                "kurutucu"   -> DryerDialog(
                    companyId   = machine.companyId,
                    companyName = machine.companyName,
                    machine     = machine,
                    onDismiss   = { showEditMachineDialog = false },
                    onCompleted = {
                        snackbarMessage      = "Makine başarıyla güncellendi!"
                        showEditMachineDialog = false
                    }
                )
                else -> showEditMachineDialog = false
            }
        } else {
            snackbarMessage      = "Makine güncelleme yetkiniz yok."
            showEditMachineDialog = false
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }
}

@Composable
fun InfoRow(label: String, center: String?, right: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Black, modifier = Modifier.weight(1f))
        Text(center ?: "-", color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(right ?: "", color = Color.DarkGray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String = "Silme Onayı",
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Evet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
        title = { Text(title) },
        text = { Text(text) }
    )
}

fun formatHoursToTime(hours: Int): String {
    val totalSeconds = hours * 3600
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
