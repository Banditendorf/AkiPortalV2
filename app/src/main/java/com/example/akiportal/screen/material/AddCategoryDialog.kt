package com.example.akiportal.screen.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, ImageVector) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(availableIcons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (category.isNotBlank()) {
                        onAdd(category.trim(), selectedIcon)
                        onDismiss()
                    }
                }
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
        title = { Text("Yeni Kategori Ekle") },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori Adı") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("İkon Seç", style = MaterialTheme.typography.labelLarge)

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableIcons.forEach { icon ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (icon == selectedIcon) MaterialTheme.colorScheme.primary
                                    else Color.LightGray.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = icon }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (icon == selectedIcon) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    )
}

// Mevcut ikon listesi
val availableIcons = listOf(
    Icons.Default.Category,
    Icons.Default.Build,
    Icons.Default.Settings,
    Icons.Default.Star,
    Icons.Default.Favorite,
    Icons.Default.Home,
    Icons.Default.Handyman
)
