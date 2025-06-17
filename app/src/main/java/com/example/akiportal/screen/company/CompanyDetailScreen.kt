package com.example.akiportal.screen.company

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.akiportal.model.Company
import com.example.akiportal.model.Machine
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.ui.theme.BackgroundDark
import com.example.akiportal.ui.theme.RedPrimary
import com.example.akiportal.ui.theme.White
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.viewmodel.MachineViewModel
import com.example.akiportal.viewmodel.MaterialViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.akiportal.screen.machine.CompressorDialog
import com.example.akiportal.screen.machine.DryerDialog
import com.example.akiportal.screen.maintenance.MachineSelectionDialog
import androidx.compose.ui.platform.LocalContext

@Composable
fun CompanyDetailScreen(
    company: Company,
    navController: NavHostController,
    machineViewModel: MachineViewModel = viewModel(),
    materialViewModel: MaterialViewModel = viewModel()
) {
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context) // pm tanımlandı
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var machines by remember { mutableStateOf<List<Machine>>(emptyList()) }
    var dialogState by remember { mutableStateOf<CompanyDialogState>(CompanyDialogState.None) }
    var showMachineSelectionDialog by remember { mutableStateOf(false) }
    var selectedMachines by remember { mutableStateOf<List<Machine>>(emptyList()) }

    LaunchedEffect(Unit) {
        machineViewModel.loadMachines(company.id)
        machineViewModel.machines.collect { machines = it }
    }

    Column(
        Modifier.fillMaxSize().background(BackgroundDark)
    ) {
        RedTopBar(title = company.name, showMenu = true) {
            // pm parametresi eklendi
            CompanyMenuItems(
                dialogStateSetter = { dialogState = it },
                scope = scope,
                snackbarHostState = snackbarHostState,
                pm = pm
            )
        }

        CompanyInfoCard(company, context)

        Button(
            onClick = {
                if (pm.canManageMaintenance()) {
                    showMachineSelectionDialog = true
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Bakım ekleme yetkiniz yok.") }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
        ) {
            Text("Bakım veya Arıza Ekle", color = White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Makineler", color = Color.White, modifier = Modifier.padding(start = 16.dp))

        MachineList(machines, navController) { selectedMachine ->
            dialogState = CompanyDialogState.EditMachine(selectedMachine)
        }

        Dialogs(
            dialogState = dialogState,
            onDismiss = { dialogState = CompanyDialogState.None },
            company = company,
            machineViewModel = machineViewModel,
            materialViewModel = materialViewModel,
            navController = navController
        )
    }

    // Makine Seçim Diyaloğu Açılır
    if (showMachineSelectionDialog) {
        MachineSelectionDialog(
            machines = machines,
            onDismiss = { showMachineSelectionDialog = false },
            onConfirm = { selectedMachines ->
                showMachineSelectionDialog = false
                val machinesJson = Uri.encode(Gson().toJson(selectedMachines))
                navController.navigate("maintenancePlanning/$machinesJson")
            }
        )

    }

    SnackbarHost(hostState = snackbarHostState)
}


@Composable
private fun CompanyMenuItems(
    dialogStateSetter: (CompanyDialogState) -> Unit,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    pm: PermissionManager // parametre eklendi
) {
    val context = LocalContext.current
    DropdownMenuItem(text = { Text("Şirketi Düzenle") }, onClick = {
        if (pm.canManageCompanies()) dialogStateSetter(CompanyDialogState.EditCompany)
        else scope.launch { snackbarHostState.showSnackbar("Şirket düzenleme yetkiniz yok.") }
    })
    DropdownMenuItem(text = { Text("Makine Sil") }, onClick = {
        if (pm.canManageMachines()) dialogStateSetter(CompanyDialogState.DeleteMachine)
        else scope.launch { snackbarHostState.showSnackbar("Makine silme yetkiniz yok.") }
    })
    DropdownMenuItem(text = { Text("Kurutucu Ekle") }, onClick = {
        if (pm.canManageMachines()) dialogStateSetter(CompanyDialogState.AddDryer)
        else scope.launch { snackbarHostState.showSnackbar("Kurutucu ekleme yetkiniz yok.") }
    })
    DropdownMenuItem(text = { Text("Kompresör Ekle") }, onClick = {
        if (pm.canManageMachines()) dialogStateSetter(CompanyDialogState.AddMachine)
        else scope.launch { snackbarHostState.showSnackbar("Kompresör ekleme yetkiniz yok.") }
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MachineList(machines: List<Machine>, navController: NavHostController, onEdit: (Machine) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(machines) { machine ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val json = Uri.encode(Gson().toJson(machine))
                            navController.navigate("machineDetail/$json")
                        },
                        onLongClick = { onEdit(machine) }
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Ad: ${machine.name}", color = Color.White)
                    Text("Seri No: ${machine.serialNumber}", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Dialogs(
    dialogState: CompanyDialogState,
    onDismiss: () -> Unit,
    company: Company,
    machineViewModel: MachineViewModel,
    materialViewModel: MaterialViewModel,
    navController: NavHostController
) {
    when (dialogState) {
        CompanyDialogState.AddMachine -> CompressorDialog(
            companyId = company.id,
            companyName = company.name,
            onDismiss = {
                onDismiss()
                machineViewModel.loadMachines(company.id)
            },
            onCompleted = {
                onDismiss()
                machineViewModel.loadMachines(company.id)
            }
        )

        CompanyDialogState.AddDryer -> DryerDialog(
            companyId = company.id,
            companyName = company.name,
            onDismiss = {
                onDismiss()
                machineViewModel.loadMachines(company.id)
            },
            onCompleted = {
                onDismiss()
                machineViewModel.loadMachines(company.id)
            }
        )

        is CompanyDialogState.EditMachine -> {
            when (dialogState.machine.type) {
                "kompresör" -> CompressorDialog(
                    companyId = company.id,
                    companyName = company.name,
                    machine = dialogState.machine,
                    onDismiss = onDismiss,
                    onCompleted = {
                        onDismiss()
                        machineViewModel.loadMachines(company.id)
                    }
                )
                "kurutucu" -> DryerDialog(
                    companyId = company.id,
                    companyName = company.name,
                    machine = dialogState.machine,
                    onDismiss = onDismiss,
                    onCompleted = {
                        onDismiss()
                        machineViewModel.loadMachines(company.id)
                    }
                )
            }
        }

        CompanyDialogState.EditCompany -> CompanyDialog(
            navController = navController,
            company = company,
            onDismiss = onDismiss,
            onSave = { updatedCompany ->
                FirebaseFirestore.getInstance()
                    .collection("companies")
                    .document(updatedCompany.id)
                    .set(updatedCompany)
                    .addOnSuccessListener { onDismiss() }
            }
        )

        CompanyDialogState.DeleteMachine -> DeleteMachinesDialog(
            machines = machineViewModel.machines.value,
            companyId = company.id,
            onDismiss = onDismiss,
            onMachinesDeleted = { machineViewModel.loadMachines(company.id) }
        )

        CompanyDialogState.None -> {}
    }
}

private sealed class CompanyDialogState {
    object None : CompanyDialogState()
    object AddMachine : CompanyDialogState()
    object AddDryer : CompanyDialogState()
    object EditCompany : CompanyDialogState()
    object DeleteMachine : CompanyDialogState()
    data class EditMachine(val machine: Machine) : CompanyDialogState()
}

@Composable
fun CompanyInfoCard(company: Company, context: Context) {
    val pm = PermissionManager.getInstance(context)
    val hasCallPermission = pm.canCallCustomer()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Yetkili: ${company.contactPerson}", fontSize = 16.sp, color = Color.Black)
            Text("Görevi: ${company.role.orEmpty()}", fontSize = 14.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "Telefon", tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (hasCallPermission) company.contactNumber ?: "-"
                    else "***********",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = "Konum", tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(company.location ?: "-", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Şirket Notu:", fontSize = 14.sp, color = Color.Black)
            Text(company.note ?: "-", fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = if (company.latitude != null && company.longitude != null) {
                            val lat = company.latitude
                            val lng = company.longitude
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${company.name})"))
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${company.location}"))
                        }
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Haritada Göster", color = RedPrimary)
                }

                OutlinedButton(
                    onClick = {
                        if (hasCallPermission && company.contactNumber != null) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${company.contactNumber}")
                            }
                            context.startActivity(intent)
                        }
                    },
                    enabled = hasCallPermission
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = RedPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ara", color = RedPrimary)
                }
            }
        }
    }
}



@Composable
fun DeleteMachinesDialog(
    machines: List<Machine>,
    companyId: String,
    onDismiss: () -> Unit,
    onMachinesDeleted: () -> Unit
) {
    var selectedMachines by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val db = FirebaseFirestore.getInstance()
                selectedMachines.forEach { machineId ->
                    db.collection("machines").document(machineId).delete()
                }
                onDismiss()
                onMachinesDeleted()
            }) {
                Text("Seçilenleri Sil", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                selectedMachines = emptySet()
                onDismiss()
            }) {
                Text("İptal")
            }
        },
        title = { Text("Makine Sil", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Silmek istediğiniz makineleri seçin:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                machines.forEach { machine ->
                    val isSelected = selectedMachines.contains(machine.id)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMachines = if (isSelected)
                                    selectedMachines - machine.id
                                else
                                    selectedMachines + machine.id
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 2.dp else 0.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(machine.name, fontWeight = FontWeight.SemiBold)
                                Text(machine.serialNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    )
}

