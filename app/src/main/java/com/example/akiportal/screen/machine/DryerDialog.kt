package com.example.akiportal.screen.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.Machine
import com.example.akiportal.model.Material
import com.example.akiportal.ui.ui.SuperDropdown
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryerDialog(
    companyId: String,
    companyName: String,
    machine: Machine? = null, // null = ekle, dolu = düzenle
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val isEditing = machine != null
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf(machine?.name ?: "") }
    var serialNumber by remember { mutableStateOf(machine?.serialNumber ?: "") }
    var note by remember { mutableStateOf(machine?.note ?: "") }

    val isAlumina = machine?.oilCode == "Aktif Alümina"
    var isFilterType by remember { mutableStateOf(!isAlumina) }

    var dryerFilterCode by remember { mutableStateOf(machine?.dryerFilterCode ?: "") }
    var dryerCount by remember { mutableStateOf(machine?.dryerFilterCount ?: 1) }

    var aluminaKg by remember { mutableStateOf(machine?.oilLiter?.toString() ?: "0.0") }

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
                    id = machine?.id ?: UUID.randomUUID().toString(),
                    type = "kurutucu",
                    companyId = companyId,
                    companyName = companyName, // ✅ Burada eklendi
                    name = name,
                    serialNumber = serialNumber,
                    note = note,
                    dryerFilterCode = if (isFilterType) dryerFilterCode else "",
                    dryerFilterCount = if (isFilterType) dryerCount else 0,
                    oilCode = if (!isFilterType) "Aktif Alümina" else "",
                    oilLiter = if (!isFilterType) aluminaKg.toDoubleOrNull() ?: 0.0 else 0.0,
                    airFilterCode = "",
                    airFilterCount = 0,
                    oilFilterCode = "",
                    oilFilterCount = 0,
                    separatorCode = "",
                    separatorCount = 0,
                    panelFilterSize = "",
                    estimatedHours = 0,
                    nextMaintenanceHour = 0
                )

                db.collection("machines")
                    .document(data.id)
                    .set(data)
                    .addOnSuccessListener { onCompleted() }
            }) {
                Text(if (isEditing) "Kurutucuyu Güncelle" else "Kurutucu Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        },
        title = { Text(if (isEditing) "Kurutucu Güncelle" else "Yeni Kurutucu Ekle") },
        text = {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Makine Adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("Seri No") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Not") }, modifier = Modifier.fillMaxWidth())

                Divider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tür: ")
                    Spacer(Modifier.width(12.dp))
                    Row {
                        RadioButton(selected = isFilterType, onClick = { isFilterType = true })
                        Text("Kurutucu Filtresi")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = !isFilterType, onClick = { isFilterType = false })
                        Text("Aktif Alümina")
                    }
                }

                if (isFilterType) {
                    SuperDropdown(
                        label = "Filtre Kodu",
                        options = allMaterials.map { it.code },
                        selectedValue = dryerFilterCode,
                        onValueChange = { dryerFilterCode = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Adet: ")
                        Spacer(Modifier.width(8.dp))

                        Button(onClick = { dryerCount = (dryerCount - 1).coerceAtLeast(1) }) { Text("-") }
                        Spacer(Modifier.width(8.dp))
                        Text(dryerCount.toString())
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { dryerCount++ }) { Text("+") }
                    }
                } else {
                    OutlinedTextField(
                        value = aluminaKg,
                        onValueChange = { aluminaKg = it },
                        label = { Text("Aktif Alümina (kg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}
