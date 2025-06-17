package com.example.akiportal.screen.material


import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.model.Material
import com.example.akiportal.ui.theme.*
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.viewmodel.MaterialViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.akiportal.permission.PermissionManager
import kotlinx.coroutines.launch

@Composable
fun MaterialListByCategoryScreen(
    category: String,
    viewModel: MaterialViewModel = viewModel()
) {
    val materials by viewModel.materials.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context)
    var showAddDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<Material?>(null) }
    var showEditDialog by remember { mutableStateOf<Material?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Material?>(null) }
    var expandedMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        RedTopBar(title = category, showMenu = true) {
            DropdownMenuItem(text = { Text("Malzeme Ekle") }, onClick = {
                if (pm.canManageMaterials()) {
                    showAddDialog = true
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Malzeme ekleme yetkiniz yok.") }
                }
            })
        }

        LazyColumn(Modifier.padding(12.dp)) {
            items(
                materials
                    .filter { it.category == category }
                    .sortedBy { it.code.lowercase() } // küçük harfe çevirerek sıralar
            ) { material ->

            var stock by remember { mutableStateOf(material.stock) }

                val isCritical = stock == 0 || stock <= material.kritikStok

                val borderColor = when {
                    stock == 0 -> RedPrimary
                    stock <= material.kritikStok -> Yellow
                    else -> Color.Transparent
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { showDetailDialog = material }, // Kart komple tıklanır
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(1.dp) // İçeri biraz boşluk veriyoruz ki border üstüne binmesin
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (material.code != "-" && material.code.isNotBlank()) material.code else material.description,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                modifier = Modifier.weight(1f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(160.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    RedPrimary,
                                                    RedPrimary.copy(alpha = 0.3f),
                                                    Color.Transparent,
                                                    RedPrimary.copy(alpha = 0.3f),
                                                    RedPrimary
                                                )
                                            ),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .border(
                                            1.dp,
                                            RedPrimary.copy(alpha = 0.6f),
                                            shape = MaterialTheme.shapes.medium
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    if (!pm.canManageMaterials()) {
                                                        scope.launch { snackbarHostState.showSnackbar("Yetkisiz işlem!") }
                                                    } else if (stock > 0) {
                                                        stock--
                                                        viewModel.updateStock(material.category, material.code, stock)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "-",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$stock",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    if (!pm.canManageMaterials()) {
                                                        scope.launch { snackbarHostState.showSnackbar("Yetkisiz işlem!") }
                                                    } else {
                                                        stock++
                                                        viewModel.updateStock(material.category, material.code, stock)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "+",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)

        // 🔥 Malzeme Detay Görüntüleme
        showDetailDialog?.let { selectedMaterial ->
            MaterialDetailDialog(
                material = selectedMaterial,
                onDismiss = { showDetailDialog = null },
                onSaveEdit = { updatedMaterial ->
                    viewModel.updateMaterial(updatedMaterial)
                    showDetailDialog = null
                },
                onDelete = { materialToDelete ->
                    viewModel.deleteMaterial(materialToDelete)
                    showDetailDialog = null
                }
            )
        }



        // 🔥 Yeni Malzeme Ekleme (showAddDialog true olunca)
        if (showAddDialog) {
            MaterialDialog(
                material = null,
                category = category,
                onDismiss = { showAddDialog = false },
                onSave = { material ->
                    viewModel.addMaterial(material)
                    showAddDialog = false
                }
            )
        }

        // 🔥 Mevcut Malzeme Düzenleme (showEditDialog doluysa)
        showEditDialog?.let { materialToEdit ->
            MaterialDialog(
                material = materialToEdit,
                category = materialToEdit.category,
                onDismiss = { showEditDialog = null },
                onSave = { updatedMaterial ->
                    viewModel.updateMaterial(updatedMaterial)
                    showEditDialog = null
                },
                onDelete = {
                    viewModel.deleteMaterial(materialToEdit)
                    showEditDialog = null
                }
            )
        }
    }
}