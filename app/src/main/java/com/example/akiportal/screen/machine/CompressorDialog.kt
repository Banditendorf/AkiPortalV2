package com.example.akiportal.screen.machine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.Machine
import com.example.akiportal.model.Material
import com.example.akiportal.model.SparePart
import com.example.akiportal.ui.ui.SuperDropdown
import com.google.firebase.firestore.FirebaseFirestore
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressorDialog(
    companyId: String,
    companyName: String,
    machine: Machine? = null,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val isEditing = machine != null

    var name by remember { mutableStateOf(machine?.name ?: "") }
    var serial by remember { mutableStateOf(machine?.serialNumber ?: "") }
    var note by remember { mutableStateOf(machine?.note ?: "") }

    var airCode by remember { mutableStateOf(machine?.airFilterCode ?: "") }
    var airCount by remember { mutableStateOf(machine?.airFilterCount?.toString() ?: "1") }

    var oilCode by remember { mutableStateOf(machine?.oilFilterCode ?: "") }
    var oilCount by remember { mutableStateOf(machine?.oilFilterCount?.toString() ?: "1") }

    var sepCode by remember { mutableStateOf(machine?.separatorCode ?: "") }
    var sepCount by remember { mutableStateOf(machine?.separatorCount?.toString() ?: "1") }

    val panelSizeOptions = listOf("Küçük", "Orta", "Büyük", "Yok")
    var selectedPanelSize by remember { mutableStateOf(machine?.panelFilterSize ?: panelSizeOptions.first()) }
    var panelExpanded by remember { mutableStateOf(false) }

    var oilType by remember { mutableStateOf(machine?.oilCode ?: "") }
    var oilLiter by remember { mutableStateOf(machine?.oilLiter?.toString() ?: "0.0") }

    var estimatedHours by remember { mutableStateOf(machine?.estimatedHours?.toString() ?: "0") }
    var nextMaintenanceHour by remember { mutableStateOf(machine?.nextMaintenanceHour?.toString() ?: "0") }

    var allMaterials by remember { mutableStateOf(listOf<Material>()) }

    LaunchedEffect(Unit) {
        db.collection("materials").get().addOnSuccessListener { snap ->
            val list = snap.documents.flatMap { doc ->
                val cat = doc.id
                val content = doc.get("icerik") as? Map<*, *> ?: return@flatMap emptyList()
                content.mapNotNull { (_, value) ->
                    val map = value as? Map<*, *> ?: return@mapNotNull null
                    Material(
                        code = map["code"] as? String ?: return@mapNotNull null,
                        shelf = map["shelf"] as? String ?: "",
                        category = cat,
                        stock = (map["stock"] as? Long)?.toInt() ?: 0,
                        kritikStok = (map["kritikStok"] as? Long)?.toInt() ?: 0
                    )
                }
            }
            allMaterials = list
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val data = Machine(
                    id = machine?.id ?: "",
                    type = "kompresör",
                    companyId = companyId,
                    companyName = companyName,
                    name = name,
                    serialNumber = serial,
                    note = note,
                    airFilterCode = airCode,
                    airFilterCount = airCount.toIntOrNull() ?: 0,
                    oilFilterCode = oilCode,
                    oilFilterCount = oilCount.toIntOrNull() ?: 0,
                    separatorCode = sepCode,
                    separatorCount = sepCount.toIntOrNull() ?: 0,
                    dryerFilterCode = "",
                    dryerFilterCount = 0,
                    panelFilterSize = selectedPanelSize,
                    oilCode = oilType,
                    oilLiter = oilLiter.toDoubleOrNull() ?: 0.0,
                    estimatedHours = estimatedHours.toIntOrNull() ?: 0,
                    nextMaintenanceHour = nextMaintenanceHour.toIntOrNull() ?: 0
                )

                if (isEditing) {
                    db.collection("machines")
                        .document(machine!!.id)
                        .set(data)
                        .addOnSuccessListener { onCompleted() }
                } else {
                    db.collection("machines")
                        .add(data)
                        .addOnSuccessListener { onCompleted() }
                }
            }) {
                Text(if (isEditing) "Cihazı Güncelle" else "Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
        title = { Text(if (isEditing) "Kompresör Güncelle" else "Yeni Kompresör Ekle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Makine Adı") })
                OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("Seri Numarası") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Not") })

                Divider()

                FilterEditRow("Hava Filtresi", airCode, airCount, onChange = { c, a -> airCode = c; airCount = a }, allMaterials = allMaterials)
                FilterEditRow("Yağ Filtresi", oilCode, oilCount, onChange = { c, a -> oilCode = c; oilCount = a }, allMaterials = allMaterials)
                FilterEditRow("Separatör", sepCode, sepCount, onChange = { c, a -> sepCode = c; sepCount = a }, allMaterials = allMaterials)

                Text("Panel Filtresi", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = panelExpanded,
                    onExpandedChange = { panelExpanded = !panelExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPanelSize,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Panel Tipi") },
                        trailingIcon = {
                            Icon(
                                if (panelExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = panelExpanded,
                        onDismissRequest = { panelExpanded = false }
                    ) {
                        panelSizeOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    selectedPanelSize = it
                                    panelExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                Text("Yağ", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = oilType,
                        onValueChange = { oilType = it },
                        label = { Text("Yağ") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = oilLiter,
                        onValueChange = { oilLiter = it },
                        label = { Text("Yağ (Litre)") },
                        modifier = Modifier.width(120.dp)
                    )
                }

                Divider()

                Text("Çalışma Saati ve Bakım Planı", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = estimatedHours,
                        onValueChange = { estimatedHours = it },
                        label = { Text("Tahmini Toplam Saat") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nextMaintenanceHour,
                        onValueChange = { nextMaintenanceHour = it },
                        label = { Text("Sıradaki Bakım Saati") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}


@Composable
fun FilterEditRow(
    label: String,
    code: String,
    count: String,
    onChange: (String, String) -> Unit,
    allMaterials: List<Material>
) {
    val codeList = allMaterials.mapNotNull { it.code }.distinct()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)

        SuperDropdown(
            label = "Kod",
            options = codeList,
            selectedValue = code,
            onValueChange = { selectedCode ->
                onChange(selectedCode, count)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val newCount = (count.toIntOrNull() ?: 1) - 1
                    onChange(code, newCount.coerceAtLeast(0).toString())
                }
            ) { Text("-") }

            Spacer(modifier = Modifier.width(8.dp))

            Text(count, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val newCount = (count.toIntOrNull() ?: 0) + 1
                    onChange(code, newCount.toString())
                }
            ) { Text("+") }
        }
    }
}
