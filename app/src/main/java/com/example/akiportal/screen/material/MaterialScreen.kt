package com.example.akiportal.screen.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.viewmodel.MaterialViewModel
import kotlinx.coroutines.launch
import com.example.akiportal.ui.ui.RedTopBar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.platform.LocalContext
import com.example.akiportal.model.Material



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialScreen(
    onCategoryClick: (String) -> Unit,
    viewModel: MaterialViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val allMaterials by viewModel.allMaterials.collectAsState()

    var sortedList by remember { mutableStateOf(categories) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context)


    var selectedMaterial by remember { mutableStateOf<Material?>(null) }

    val filteredMaterials = remember(searchQuery, allMaterials) {
        if (searchQuery.isBlank()) emptyList()
        else allMaterials.filter {
            it.code.contains(searchQuery, true) ||
                    it.category.contains(searchQuery, true) ||
                    it.description.contains(searchQuery, true)
        }
    }

    // Kategoriler güncellenince A-Z sırala
    LaunchedEffect(categories) {
        sortedList = categories.sorted()
    }

    Scaffold(
        topBar = {
            RedTopBar(
                title = "Malzeme Kategorileri",
                showMenu = true,
                menuContent = {
                    DropdownMenuItem(
                        text = { Text("Yeni Kategori Ekle") },
                        onClick = {
                            if (pm.canManageCategories()) {
                                showAddCategoryDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Bu işlem için yetkiniz yok.")
                                }
                            }
                            expandedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Kategori Sil") },
                        onClick = {
                            if (sortedList.isNotEmpty()) {
                                showMultiDeleteDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Silinecek kategori yok.")
                                }
                            }
                            expandedMenu = false
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Sırala: A-Z") },
                        onClick = {
                            sortedList = categories.sorted()
                            expandedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sırala: Z-A") },
                        onClick = {
                            sortedList = categories.sortedDescending()
                            expandedMenu = false
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Malzeme Ara (Kod, Kategori, Açıklama)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Gray)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gray,
                    unfocusedBorderColor = Gray,
                    cursorColor = Gray,
                    focusedLabelColor = Gray,
                    unfocusedLabelColor = Gray,
                    disabledLabelColor = Gray,
                    errorLabelColor = Color.Red
                )
            )

            if (searchQuery.isBlank()) {
                // Kategori listelemesi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedList, key = { it }) { categoryItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(categoryItem) },
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = categoryItem,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.Send, contentDescription = "Git", tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Arama sonucu: malzeme listelemesi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMaterials) { material ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMaterial = material },
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Kod: ${material.code}", color = White, fontWeight = FontWeight.Bold)
                                Text("Kategori: ${material.category}", color = LightGray)
                                Text("Açıklama: ${material.description}", color = White)
                            }
                        }
                    }
                }
            }
        }

        if (showMultiDeleteDialog) {
            CategoryMultiDeleteDialog(
                categories = sortedList,
                onDismiss = { showMultiDeleteDialog = false },
                onConfirmDelete = { selectedList ->
                    selectedList.forEach { viewModel.deleteCategory(it) }
                    showMultiDeleteDialog = false
                }
            )
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onAdd = { categoryName, _ ->
                    viewModel.addCategory(categoryName)
                    showAddCategoryDialog = false
                }
            )
        }

        selectedMaterial?.let { material ->
            MaterialDialog(
                material = material,
                onDismiss = { selectedMaterial = null },
                onSave = { updated ->
                    viewModel.updateMaterial(updated)
                    selectedMaterial = null
                },
                onDelete = {
                    viewModel.deleteMaterial(material)
                    selectedMaterial = null
                }
            )
        }


    }
}

@Composable
fun CategoryMultiDeleteDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirmDelete: (List<String>) -> Unit
) {
    val selectedItems = remember { mutableStateListOf<String>() }
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Silme Onayı") },
            text = { Text("Seçilen kategorileri silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmDelete(selectedItems.toList())
                    showConfirm = false
                    onDismiss()
                }) {
                    Text("Evet", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Kategori Sil") },
            text = {
                Column {
                    Text("Silmek istediğiniz kategorileri seçin:")
                    Spacer(Modifier.height(8.dp))
                    categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (selectedItems.contains(category)) {
                                        selectedItems.remove(category)
                                    } else {
                                        selectedItems.add(category)
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = selectedItems.contains(category),
                                onCheckedChange = {
                                    if (it) selectedItems.add(category)
                                    else selectedItems.remove(category)
                                }
                            )
                            Text(category)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { if (selectedItems.isNotEmpty()) showConfirm = true }
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        )
    }
}
