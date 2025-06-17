package com.example.akiportal.screen.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.Machine


@Composable
fun MachineSelectionDialog(
    machines: List<Machine>,
    onDismiss: () -> Unit,
    onConfirm: (List<Machine>) -> Unit
) {
    var selectedMachines by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = machines.filter { selectedMachines.contains(it.id) }
                    onConfirm(selected) // burada navController yok sadece geri bildirim var
                }
            ) {
                Text("Devam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
        title = { Text("Makine Seçimi") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(machines) { machine ->
                    val isSelected = selectedMachines.contains(machine.id)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMachines = if (isSelected) {
                                    selectedMachines - machine.id
                                } else {
                                    selectedMachines + machine.id
                                }
                            },
                        tonalElevation = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(machine.name)
                                Text(
                                    machine.serialNumber ?: "-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedMachines = if (checked) {
                                        selectedMachines + machine.id
                                    } else {
                                        selectedMachines - machine.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
