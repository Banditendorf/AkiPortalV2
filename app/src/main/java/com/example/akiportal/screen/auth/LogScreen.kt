package com.example.akiportal.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.ui.theme.*
import com.example.akiportal.viewmodel.LogViewModel
import androidx. compose. ui. text. font.FontWeight

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val logs by viewModel.logs.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tümü") }

    val categories = listOf("Tümü", "Makine", "Kullanıcı", "Bakım")

    val filteredLogs = logs.filter {
        val matchesSearch = it.userEmail.contains(searchText, true) || it.action.contains(searchText, true)
        val matchesCategory = selectedCategory == "Tümü" || it.action.contains(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        RedTopBar(title = "İşlem Logları")

        // 🔍 Arama ve kategori filtre satırı
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Kullanıcı veya işlem ara") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 📜 Log listesi
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.userEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(log.timestamp, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.action)

                        log.details?.let { details ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Detaylar:", fontWeight = FontWeight.SemiBold)
                            details.forEach { (key, value) ->
                                Text("• $key: $value", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
