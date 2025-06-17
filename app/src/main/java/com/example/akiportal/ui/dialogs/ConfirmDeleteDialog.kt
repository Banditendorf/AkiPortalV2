package com.example.akiportal.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.akiportal.model.Material

@Composable
fun ConfirmDeleteDialog(
    material: Material,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Silme Onayı") },
        text = { Text("“${material.code}” kodlu malzemeyi silmek istediğinize emin misiniz?") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm() // 🔴 Silme burada tetiklenecek
            }) {
                Text("Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
