package com.example.akiportal.screen.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.model.User
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.ui.components.UserFormDialog
import com.example.akiportal.ui.theme.RedPrimary
import com.example.akiportal.ui.theme.White
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun UsersScreen(
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context)

    // Dialog & Form State
    var editingUser by remember { mutableStateOf<User?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Search State
    var searchQuery by remember { mutableStateOf("") }

    // Users state from ViewModel
    val allUsers by userViewModel.allUsers.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { RedTopBar(title = "Kullanıcılar") },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF212121))
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Kullanıcı Ara", color = White) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = White)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    // Metin rengi için:
                    focusedTextColor     = White,
                    unfocusedTextColor   = White,

                    // Sınır renkleri:
                    focusedBorderColor   = RedPrimary,
                    unfocusedBorderColor = Color.Gray,

                    // İmleç rengi:
                    cursorColor          = RedPrimary,

                    // Label renkleri:
                    focusedLabelColor    = RedPrimary,
                    unfocusedLabelColor  = White
                )
            )


            // New User Button
            Button(
                onClick = {
                    if (pm.canManageUsers()) {
                        editingUser = null
                        showFormDialog = true
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Yetkiniz yok.")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = White)
                Spacer(Modifier.width(8.dp))
                Text("Yeni", color = White)
            }

            Spacer(Modifier.height(12.dp))

            // Filtered User List
            val filtered = allUsers.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (pm.canManageUsers()) {
                                    editingUser = user
                                    showFormDialog = true
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Yetkiniz yok.")
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(user.fullName, color = White, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Mail:${user.email}", color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Görev: ${user.role}", color = White)
                            Spacer(Modifier.height(4.dp))
                            Text("İş Tel: ${user.workPhone}", color = White)
                            Spacer(Modifier.height(4.dp))
                            Text("Kiş. Tel: ${user.personalPhone}", color = White)
                            Spacer(Modifier.height(4.dp))
                            Text("Aktif: ${if (user.isActive) "Evet" else "Hayır"}", color = White)
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showFormDialog) {
        UserFormDialog(
            user = editingUser,
            onConfirm = { user, password ->
                val validation = when {
                    !user.email.endsWith("@akiendüstri.com") -> "E-posta @akiendüstri.com ile bitmeli"
                    password != null && password.length < 6 -> "Şifre en az 6 karakter olmalı"
                    else -> null
                }
                if (validation != null) {
                    errorMessage = validation
                } else {
                    userViewModel.saveUser(
                        user = user,
                        password = password,
                        onError = { errorMessage = it },
                        onSuccess = {
                            showFormDialog = false
                            errorMessage = null
                        }
                    )
                }
            },
            onDismiss = {
                showFormDialog = false
                errorMessage = null
            }
        )
        errorMessage?.let { msg ->
            LaunchedEffect(msg) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }
}
