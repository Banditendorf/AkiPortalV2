package com.example.akiportal.screen.user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.ui.theme.*
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.util.NotificationHelper
import com.example.akiportal.viewmodel.AuthViewModel
import com.example.akiportal.viewmodel.CompanyViewModel
import com.example.akiportal.viewmodel.MaterialViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red as RedColor

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit,
    onViewLogsClick: () -> Unit
) {
    // NotificationHelper'ı kompozisyon başladığında init et
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        NotificationHelper.init(context)
    }

    val user by authViewModel.currentUser.collectAsState()
    val companyViewModel: CompanyViewModel = viewModel()
    val materialViewModel: MaterialViewModel = viewModel()

    var feedback by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pm = PermissionManager.getInstance(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        RedTopBar(title = "Ayarlar")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feedback bölümü
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Geri Bildirim", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        label = { Text("Görüş veya önerinizi yazın") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                            val formattedDate = sdf.format(Date())
                            val data = hashMapOf(
                                "message" to feedback,
                                "timestamp" to formattedDate,
                                "userEmail" to (user?.email ?: "bilinmiyor")
                            )
                            FirebaseFirestore.getInstance()
                                .collection("feedback")
                                .add(data)
                                .addOnSuccessListener {
                                    sent = true
                                    feedback = ""
                                }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Gönder") }
                    if (sent) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Teşekkürler, mesajınız iletildi.", color = Green)
                    }
                }
            }

            // Şifre değiştirme bölümü
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Şifre Değiştir", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Mevcut Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Yeni Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val userEmail = user?.email
                            if (!userEmail.isNullOrBlank()) {
                                val auth = FirebaseAuth.getInstance()
                                val credential = EmailAuthProvider
                                    .getCredential(userEmail, currentPassword)
                                auth.currentUser
                                    ?.reauthenticate(credential)
                                    ?.addOnSuccessListener {
                                        auth.currentUser
                                            ?.updatePassword(newPassword)
                                            ?.addOnSuccessListener {
                                                passwordChangeMessage = "Şifre başarıyla güncellendi."
                                                currentPassword = ""
                                                newPassword = ""
                                            }
                                            ?.addOnFailureListener {
                                                passwordChangeMessage = "Yeni şifre belirlenemedi: ${it.localizedMessage}"
                                            }
                                    }
                                    ?.addOnFailureListener {
                                        passwordChangeMessage = "Mevcut şifre hatalı."
                                    }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Değiştir") }
                    passwordChangeMessage?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = if (it.contains("başarıyla")) Green else RedColor)
                    }
                }
            }

            Button(
                onClick = {
                    if (pm.canManageUsers()) onViewLogsClick()
                    else scope.launch {
                        snackbarHostState.showSnackbar("Bu işlem için yetkiniz yok.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Logları Görüntüle", color = Color.White)
            }


            // Çıkış yap butonu
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Çıkış Yap", color = White) }
            

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Uygulama Sürümü: 1.2.0",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "© 2025 AKİ Endüstri. Tüm hakları saklıdır.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
