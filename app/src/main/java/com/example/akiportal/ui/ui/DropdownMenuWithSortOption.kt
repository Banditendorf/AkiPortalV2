package com.example.akiportal.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.ui.Alignment


@Composable
fun DropdownMenuWithSortOption(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Sırala: $selectedOption")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("A-Z", "Z-A", "En Son Eklenen").forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onOptionSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperDropdown(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var inputValue by remember { mutableStateOf(selectedValue) }

    val filteredOptions = if (inputValue.length >= 3) {
        options.filter { it.contains(inputValue, ignoreCase = true) }
    } else emptyList()

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty(),
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = {
                inputValue = it
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded && filteredOptions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = (5 * 48).dp)
        ) {
            filteredOptions.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        inputValue = item
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }

    if (inputValue.isNotBlank() && inputValue !in options) {
        Text(
            text = "Yalnızca sistemde kayıtlı öğeleri seçebilirsiniz.",
            color = Color.Red,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperDropdown(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var inputValue by remember { mutableStateOf(selectedValue) }

    val filteredOptions = if (inputValue.length >= 3) {
        options.filter { it.contains(inputValue, ignoreCase = true) }
    } else emptyList()

    val showWarning = inputValue.isNotBlank() && inputValue !in options

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty(),
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = {
                inputValue = it
                onValueChange(it) // Her yazılan geçerli sayılır
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = (5 * 48).dp)
                    .fillMaxWidth()
            ) {
                filteredOptions.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            inputValue = item
                            onValueChange(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

