package com.example.akiportal.screen.main

import com.example.akiportal.screen.material.MaterialScreen
import android.net.Uri
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.akiportal.model.Company
import com.example.akiportal.model.Machine
import com.example.akiportal.model.User
import com.example.akiportal.ui.MainMenuScreen
import androidx.compose.ui.platform.LocalContext
import com.example.akiportal.permission.PermissionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.model.Maintenance
import com.example.akiportal.screen.auth.LogScreen
import com.example.akiportal.screen.auth.LoginScreen
import com.example.akiportal.screen.company.CompanyDetailScreen
import com.example.akiportal.screen.company.CompanyScreen
import com.example.akiportal.screen.machine.MachineDetailScreen
import com.example.akiportal.screen.calendar.PlannedMaintenanceScreen
import com.example.akiportal.screen.maintenance.MaintenanceDetailScreen
import com.example.akiportal.screen.maintenance.MaintenancePlanningScreen
import com.example.akiportal.screen.maintenance.MaintenanceCompletionScreen
import com.example.akiportal.screen.material.MaterialListByCategoryScreen
import com.example.akiportal.screen.notification.NotificationScreen
import com.example.akiportal.screen.ocr.OcrUploadScreen
import com.example.akiportal.screen.preparation.PreparationDetailScreen
import com.example.akiportal.screen.preparation.PreparationScreen
import com.example.akiportal.screen.user.SettingsScreen
import com.example.akiportal.screen.user.UsersScreen
import com.example.akiportal.viewmodel.UserViewModel
import com.example.akiportal.viewmodel.MaterialViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    isLoggedIn: Boolean,
    currentUser: User?,
    onLoginSuccess: (String) -> Unit,
    onLogout: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pm = PermissionManager.getInstance(context)

    Box {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "menu" else "login"
        ) {
            composable("login") {
                LoginScreen(onLoginSuccess = onLoginSuccess)
            }
            // Bakım Detayı, Planlandı/Hazırlandı/Tamamlandı route'ları
            composable(
                "futureMaintenance/{maintenanceJson}",
                arguments = listOf(navArgument("maintenanceJson") { type = NavType.StringType })
            ) {
                val json = it.arguments?.getString("maintenanceJson")
                val maintenance = Gson().fromJson(json, Maintenance::class.java)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { }
            }
            composable("menu") {
                currentUser?.let { user ->
                    MainMenuScreen(
                        currentUser = user,
                        onMenuItemClick = { route ->
                            // hangi route için hangi can...() metodu kullanılacak
                            val allowed = when (route) {
                                "companies" -> pm.canManageCompanies()
                                "bakimlar", "hazirliklar" -> pm.canManageMaintenance()
                                "malzemeler" -> pm.canManageMaterials()
                                "kullanicilar" -> pm.canManageUsers()
                                "ayarlar", "logs" -> pm.canManageUsers()
                                else -> true
                            }

                            if (allowed) {
                                navController.navigate(route)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Bu işlem için yetkiniz yok.")
                                }
                            }
                        },
                        onNotificationClick = {
                            navController.navigate("bildirimler")
                        }
                    )
                }
            }

            composable("companies") {
                if (pm.canManageCompanies()) {
                    CompanyScreen(
                        navController = navController,
                        onCompanyClick = { company ->
                            val json = Uri.encode(Gson().toJson(company))
                            navController.navigate("companyDetail/$json")
                        }
                    )
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Şirket görüntüleme yetkiniz yok.")
                    }
                }
            }

            composable("companyDetail/{companyJson}",
                arguments = listOf(navArgument("companyJson") { type = NavType.StringType })
            ) {
                val json = it.arguments?.getString("companyJson")
                val company = Gson().fromJson(json, Company::class.java)
                CompanyDetailScreen(company = company, navController = navController)
            }
            composable("bildirimler") {
                NotificationScreen(
                    onNavigateToDailyMaintenance = {
                        navController.navigate("dailyMaintenances/...")
                    },
                    onNavigateToMachineDetail = { machineId ->
                        val dummy = Gson().toJson(Machine(id = machineId, name = "Makine $machineId"))
                        navController.navigate("machineDetail/${Uri.encode(dummy)}")
                    }
                )
            }
            composable(
                "maintenanceDetail/{maintenanceJson}",
                arguments = listOf(navArgument("maintenanceJson") { type = NavType.StringType })
            ) {
                val encoded = it.arguments?.getString("maintenanceJson")
                val decoded = Uri.decode(encoded ?: "")
                val maintenance = Gson().fromJson(decoded, Maintenance::class.java)
                MaintenanceDetailScreen(maintenance = maintenance)
            }
            composable("malzemeler") {
                if (pm.canManageMaterials()) {
                    MaterialScreen(onCategoryClick = { category ->
                        navController.navigate("materialListByCategory/${Uri.encode(category)}")
                    })
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Malzeme görüntüleme yetkiniz yok.")
                    }
                }
            }

            composable(
                "preparationDetail/{maintenanceJson}",
                arguments = listOf(navArgument("maintenanceJson") { type = NavType.StringType })
            ) {
                val encoded = it.arguments?.getString("maintenanceJson")
                val decoded = Uri.decode(encoded ?: "")
                val maintenance = Gson().fromJson(decoded, Maintenance::class.java)
                PreparationDetailScreen(
                    maintenance = maintenance,
                    onBack = { navController.popBackStack() },
                    currentUser = currentUser!!
                )
            }
            composable(
                "materialListByCategory/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) {
                val category = it.arguments?.getString("category") ?: ""
                MaterialListByCategoryScreen(category = category)
            }
            composable("bakimlar") {
                PlannedMaintenanceScreen(
                    navController = navController,
                    onAddClick = {
                        navController.navigate("ocrScan")
                    }
                )
            }
            composable("hazirliklar") {
                if (pm.canManageMaintenance()) {
                    PreparationScreen(
                        onMaintenanceClick = { maintenance: Maintenance ->
                            val json = Uri.encode(Gson().toJson(maintenance))
                            navController.navigate("preparationDetail/$json")
                        }
                    )
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Bakım ekleme yetkiniz yok.")
                    }
                }
            }

            composable("kullanicilar") {
                if (pm.canManageUsers()) {
                    UsersScreen()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Kullanıcıları görüntüleme yetkiniz yok.")
                    }
                }
            }

            composable("ayarlar") {
                SettingsScreen(
                    onLogout = onLogout,
                    onViewLogsClick = {
                        if (pm.canManageUsers()) {
                            navController.navigate("logs")
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Log erişim yetkiniz yok.")
                            }
                        }
                    }
                )
            }

            composable("ocrScan") {
                OcrUploadScreen(
                    onExtracted = { extractedText ->
                        navController.navigate("maintenancePlanFromOCR/${Uri.encode(extractedText)}")
                    }
                )
            }
            composable(
                "completionDetail/{maintenanceJson}",
                arguments = listOf(navArgument("maintenanceJson") { type = NavType.StringType })
            ) { navBackStackEntry ->
                val json = navBackStackEntry.arguments?.getString("maintenanceJson")
                val maintenance = Gson().fromJson(json, Maintenance::class.java)
                val userViewModel: UserViewModel = viewModel()
                val materialViewModel: MaterialViewModel = viewModel()

                MaintenanceCompletionScreen(
                    maintenance = maintenance,
                    onComplete = { navController.popBackStack() },
                    materialViewModel = materialViewModel
                )
            }

            composable(
                "maintenancePlanning/{selectedMachines}",
                arguments = listOf(navArgument("selectedMachines") { type = NavType.StringType })
            ) { backStackEntry ->
                val selectedMachinesJson = backStackEntry.arguments?.getString("selectedMachines") ?: ""
                val selectedMachines = remember(selectedMachinesJson) {
                    if (selectedMachinesJson.isNotBlank()) {
                        Gson().fromJson(selectedMachinesJson, Array<Machine>::class.java).toList()
                    } else {
                        emptyList()
                    }
                }
                MaintenancePlanningScreen(
                    selectedMachines = selectedMachines,
                    bakimViewModel = viewModel(),
                    navController = navController
                )
            }
            composable(
                "machineDetail/{machineJson}",
                arguments = listOf(navArgument("machineJson") { type = NavType.StringType })
            ) {
                val encoded = it.arguments?.getString("machineJson")
                val decoded = Uri.decode(encoded ?: "")
                val machine = Gson().fromJson(decoded, Machine::class.java)
                MachineDetailScreen(machine = machine, navController = navController)
            }
            composable("logs") {
                // Log ekranı erişimi: kullanıcı yönetim izni temelinde
                if (pm.canManageUsers()) {
                    LogScreen()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Log erişim yetkiniz yok.")
                    }
                }
            }

        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
