package com.example.akiportal.screen.company

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.akiportal.model.Company
import com.example.akiportal.model.User
import com.example.akiportal.ui.ui.DropdownMenuWithSortOption
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.ui.theme.BackgroundDark
import com.example.akiportal.ui.theme.DarkGray
import com.example.akiportal.ui.theme.White
import androidx.compose.ui.platform.LocalContext
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.viewmodel.CompanyViewModel
import com.example.akiportal.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.launch

@Composable
fun CompanyScreen(
    navController: NavHostController,
    onCompanyClick: (Company) -> Unit,
    companyViewModel: CompanyViewModel = viewModel()
) {
    val userViewModel: UserViewModel = viewModel()
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context)
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("A-Z") }
    var selectedCompanies by remember { mutableStateOf(setOf<String>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userViewModel.fetchCurrentUser()
        userViewModel.currentUser.collect { currentUser = it }
    }

    LaunchedEffect(Unit) {
        companyViewModel.loadCompanies()
        companyViewModel.companies.collect { companies = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        RedTopBar(title = "Şirketler", showMenu = true, menuContent = {
            DropdownMenuItem(
                text = { Text("Şirket Ekle") },
                onClick = {
                    if (pm.canManageCompanies()) {
                        showDialog = true
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Şirket ekleme yetkiniz yok.") }
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Şirket Sil") },
                onClick = {
                    if (pm.canManageCompanies()) {
                        showDeleteDialog = true
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Şirket silme yetkiniz yok.") }
                    }
                }
            )
        })

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Şirket Ara") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f)
            )
            DropdownMenuWithSortOption(selectedOption = sortOption) {
                sortOption = it
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val sortedFiltered = companies
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(
                when (sortOption) {
                    "A-Z" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    "Z-A" -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
                    "En Son Eklenen" -> compareByDescending { it.id }
                    else -> compareBy { it.name }
                }
            )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedFiltered) { company ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val json = Uri.encode(Gson().toJson(company))
                            navController.navigate("companyDetail/$json")
                        },
                    colors = CardDefaults.cardColors(containerColor = DarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = company.name, color = White, style = MaterialTheme.typography.titleMedium)
                        company.contactPerson?.let {
                            Text(text = "Yetkili: $it", color = Color.LightGray)
                        }
                        company.contactNumber?.let {
                            Text(text = "Telefon: $it", color = Color.LightGray)
                        }
                    }
                }
            }
        }

        if (showDialog) {
            CompanyDialog(
                navController = navController,
                company = null,
                onDismiss = { showDialog = false },
                onSave = { newCompany ->
                    FirebaseFirestore.getInstance()
                        .collection("companies")
                        .document(newCompany.id)
                        .set(newCompany)
                        .addOnSuccessListener {
                            companyViewModel.loadCompanies()
                            showDialog = false
                        }
                }
            )
        }


        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    selectedCompanies = emptySet()
                },
                confirmButton = {
                    TextButton(onClick = {
                        val db = FirebaseFirestore.getInstance()
                        selectedCompanies.forEach { id ->
                            db.collection("companies").document(id).delete()
                        }
                        showDeleteDialog = false
                        selectedCompanies = emptySet()
                        companyViewModel.loadCompanies()
                    }) {
                        Text("Seçilenleri Sil")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        selectedCompanies = emptySet()
                    }) {
                        Text("İptal")
                    }
                },
                title = { Text("Şirket Sil") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Silmek istediğiniz şirketleri seçin:")
                        companies.forEach { company ->
                            val isSelected = selectedCompanies.contains(company.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCompanies = if (isSelected)
                                            selectedCompanies - company.id
                                        else
                                            selectedCompanies + company.id
                                    }
                                    .background(if (isSelected) Color.LightGray else Color.Transparent)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(company.name)
                                Text(company.contactPerson ?: "")
                            }
                        }
                    }
                }
            )
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
