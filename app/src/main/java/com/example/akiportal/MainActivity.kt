package com.example.akiportal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.akiportal.model.User
import com.example.akiportal.screen.main.AppNavigation
import com.example.akiportal.ui.theme.AkiPortalTheme
import com.example.akiportal.util.EncryptedSharedPrefHelper
import com.example.akiportal.util.NotificationHelper
import com.example.akiportal.util.NotificationWorker
import com.example.akiportal.viewmodel.UserViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Android 13+ için bildirim izni
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.d("MainActivity", "POST_NOTIFICATIONS izni verildi")
                setupNotificationSystem()
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS izni reddedildi")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Android 13+ ise izin iste, değilse direkt kurulum
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    setupNotificationSystem()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Kullanıcıya kısa bir açıklama gösterebilirsiniz
                    Log.d("MainActivity", "Bildirim izni için neden gerektiğini açıklayın")
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            setupNotificationSystem()
        }

        // --- Geri kalan başlangıç kodu ---

        // Güvenli paylaşılan tercihleri oku
        val (rememberedEmail, isRemembered) = try {
            Pair(
                EncryptedSharedPrefHelper.getSavedEmail(this),
                EncryptedSharedPrefHelper.isRemembered(this)
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "SecurePrefs okuma hatası, 'beni hatırla' devre dışı bırakıldı", e)
            Pair(null, false)
        }

        setContent {
            AkiPortalTheme {
                val navController = rememberNavController()

                var isLoggedIn by remember { mutableStateOf(isRemembered) }
                var currentUser by remember { mutableStateOf<User?>(null) }

                val userViewModel: UserViewModel = viewModel()

                // Kullanıcı oturumunu kontrol et
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        userViewModel.fetchCurrentUser()
                        userViewModel.currentUser.collect { user ->
                            currentUser = user
                        }
                    }
                }

                // UI Akışı
                if (isLoggedIn && currentUser == null) {
                    // Yükleniyor ekranı
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    // Giriş ekranı veya ana navigasyon
                    AppNavigation(
                        navController = navController,
                        isLoggedIn = isLoggedIn,
                        currentUser = currentUser,
                        onLoginSuccess = { email ->
                            isLoggedIn = true
                            try {
                                EncryptedSharedPrefHelper.saveRememberMe(
                                    this@MainActivity,
                                    email
                                )
                            } catch (e: Exception) {
                                Log.e("MainActivity", "SecurePrefs kaydetme hatası", e)
                            }
                        },
                        onLogout = {
                            isLoggedIn = false
                            try {
                                EncryptedSharedPrefHelper.clearRememberMe(this@MainActivity)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "SecurePrefs temizleme hatası", e)
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Bildirim kanalını oluşturur, test amacıyla
     * hem tek seferlik iş hem de günlük planlı iş
     */
    private fun setupNotificationSystem() {
        // NotificationChannel ve diğer init’ler
        NotificationHelper.init(applicationContext)

        // 1) Test için bir kere çalışacak iş:
        val testRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS) // 10 saniye sonra çalışacak
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "TestNotificationWork",
                ExistingWorkPolicy.REPLACE,
                testRequest
            )

        // 2) Gerçek günlük bildirim işini kur
        NotificationHelper.scheduleDailyNotificationWorker()
    }
}
