package com.example.akiportal.screen.material

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.Material
import com.example.akiportal.ui.dialogs.ConfirmDeleteDialog
import com.example.akiportal.ui.theme.RedPrimary
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MaterialDetailDialog(
    material: Material,
    onDismiss: () -> Unit,
    onDelete: (Material) -> Unit,
    onSaveEdit: (Material) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    when {
        isEditing -> {
            MaterialDialog(
                material = material,
                category = material.category,
                onDismiss = { isEditing = false },
                onSave = {
                    onSaveEdit(it)
                    isEditing = false
                },
                onDelete = {
                    showConfirmDelete = true
                }
            )
        }

        else -> {
            val isAlarmActive = material.stock == 0 || material.stock <= material.kritikStok

            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Column {
                        Text(
                            text = "Malzeme Detayı",
                            style = MaterialTheme.typography.titleLarge,
                            color = RedPrimary
                        )
                        if (isAlarmActive) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Kritik stok seviyesinde!",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Red
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Kod", material.code)
                        DetailRow("Raf", material.shelf)
                        DetailRow("Stok", material.stock.toString())
                        DetailRow("Kritik Stok", material.kritikStok.toString())
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Açıklama", material.description.ifBlank { "Açıklama girilmemiş." })
                    }
                },
                confirmButton = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Düzenle")
                            }
                            OutlinedButton(
                                onClick = { showConfirmDelete = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sil")
                            }
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kapat")
                        }
                    }
                },
                dismissButton = {}
            )
        }
    }
    fun deleteMaterial(material: Material) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("materials")
            .document(material.category)
            .get()
            .addOnSuccessListener { snapshot ->
                val icerik = snapshot.get("icerik") as? Map<*, *> ?: return@addOnSuccessListener

                val matchingKey = icerik.entries.find {
                    (it.value as? Map<*, *>)?.get("code") == material.code
                }?.key as? String ?: return@addOnSuccessListener

                firestore.collection("materials")
                    .document(material.category)
                    .update("icerik.$matchingKey", FieldValue.delete())
            }
    }


    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            material = material,
            onDismiss = { showConfirmDelete = false },
            onConfirm = {
                deleteMaterial(material) // 🔴 Firestore'dan sil
                onDelete(material)       // 🧼 ViewModel'den/State'ten kaldır
                showConfirmDelete = false
            }
        )
    }

}
@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
