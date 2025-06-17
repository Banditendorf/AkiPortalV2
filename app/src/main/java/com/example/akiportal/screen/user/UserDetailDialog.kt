package com.example.akiportal.screen.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.User
import com.example.akiportal.ui.components.UserFormDialog
import com.example.akiportal.ui.theme.RedPrimary

@Composable
fun UserDetailDialog(
    user: User,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onSave: (User) -> Unit
) {
    var showEditForm by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    // Detay dialogu
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Kullanıcı Detayları", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ad Soyad: ${user.fullName}")
                Text("E-posta: ${user.email}")
                Text("Görevi: ${user.role}")
                Text("İş Telefonu: ${user.workPhone}")
                Text("Kişisel Telefon: ${user.personalPhone}")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aktif")
                    Switch(
                        checked = user.isActive,
                        onCheckedChange = { onToggleActive(user.uid, it) }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Düzenle butonu
                TextButton(onClick = { showEditForm = true }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Düzenle")
                }
                // Silme butonu
                TextButton(
                    onClick = { showConfirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RedPrimary
                    )
                ) {
                    Text("Sil")
                }
                // Kapat butonu
                TextButton(onClick = onDismiss) {
                    Text("Kapat")
                }
            }
        }
    )

    // Silme onayı
    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Silme Onayı") },
            text = { Text("Bu kullanıcıyı silmek istediğinizden emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(user.uid)
                    showConfirmDelete = false
                    onDismiss()
                }) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Tek form dialog: hem alanlar hem permissions
    if (showEditForm) {
        UserFormDialog(
            user = user,
            onConfirm = { updatedUser, _ ->
                onSave(updatedUser)
                showEditForm = false
                onDismiss()
            },
            onDismiss = { showEditForm = false }
        )
    }
}
