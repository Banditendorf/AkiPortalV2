package com.example.akiportal.screen.maintenance

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.akiportal.model.Maintenance
import com.example.akiportal.model.Machine
import com.example.akiportal.screen.machine.formatHoursToTime
import com.example.akiportal.ui.theme.*
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.util.PhotoStorageHelper
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File

@Composable
fun MaintenanceDetailScreen(
    maintenance: Maintenance
) {
    val context = LocalContext.current
    val db = Firebase.firestore

    var machine by remember { mutableStateOf<Machine?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // 1. Makineyi ve fotoğrafları çek
    LaunchedEffect(maintenance.machineId) {
        db.collection("machines").document(maintenance.machineId).get()
            .addOnSuccessListener {
                machine = it.toObject(Machine::class.java)
                isLoading = false
            }
            .addOnFailureListener {
                error = "Makine bilgisi yüklenemedi"
                isLoading = false
            }
        // FOTOĞRAFLARI ARTIK DOĞRU KLASÖR YOLUNDAN OKUYORUZ
        photoFiles = PhotoStorageHelper.listPhotosByFolderName(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.getExternalFilesDir(null)?.absolutePath + "/" + maintenance.photoFolderName
            } else {
                Environment.getExternalStorageDirectory().absolutePath + "/" + maintenance.photoFolderName
            }
        )
    }


    Column(Modifier.fillMaxSize()) {
        RedTopBar(title = "Bakım Detayı", showBackButton = false)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(16.dp)
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null -> Text("Hata: $error", color = MaterialTheme.colorScheme.error)
                else -> DetailContent(maintenance, machine, photoFiles)
            }
        }
    }
}

@Composable
fun DetailContent(
    maintenance: Maintenance,
    machine: Machine?,
    photoFiles: List<File>
) {
    var selectedFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(Color.Black, Color.Red)))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Genel Bilgiler",
                    color = White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Şirket", maintenance.companyName)
                InfoRow("Makine", maintenance.machineName)
                InfoRow("Seri No", maintenance.serialNumber)
                InfoRow("Bakım Tarihi", "${maintenance.plannedDate} ${maintenance.plannedTime}")
                InfoRow("İş Emri No", maintenance.workOrderNumber)
                machine?.estimatedHours?.let {
                    InfoRow("Tahmini Makina Saati", formatHoursToTime(it))
                }
            }
        }

        InfoCard("Açıklama") {
            Text(maintenance.description, color = LightGray)
        }

        InfoCard("Bakım Notları") {
            Text("Bakım Önü: ${maintenance.preMaintenanceNote}", color = LightGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Bakım Sonu: ${maintenance.postMaintenanceNote}", color = LightGray)
        }

        InfoCard("Zaman Bilgisi") {
            InfoRow("Başlangıç", maintenance.startTime)
            InfoRow("Bitiş", maintenance.endTime)
        }

        InfoCard("Sorumlular") {
            InfoRow("İşlem Sorumluları", maintenance.responsibles.joinToString())
            InfoRow("Hazırlayan", maintenance.preparedBy)
        }

        if (maintenance.oilChanged) {
            InfoCard("Yağ Bilgisi") {
                InfoRow("Yağ Kodu", maintenance.oilCode)
                InfoRow("Miktar", "${maintenance.oilLiter} L")
            }
        }

        if (maintenance.voltageL1 != null || maintenance.currentL1 != null || maintenance.pressure != null) {
            InfoCard("Ölçümler") {
                maintenance.voltageL1?.let { InfoRow("Voltaj L1", "$it V") }
                maintenance.currentL1?.let { InfoRow("Akım L1", "$it A") }
                maintenance.pressure?.let { InfoRow("Basınç", "$it bar") }
            }
        }

        if (maintenance.changedParts.isNotEmpty()) {
            InfoCard("Değiştirilen Parçalar") {
                val changedCodes = maintenance.changedParts.toSet()
                val allParts = maintenance.parts + maintenance.extraParts
                val changedParts = allParts.filter { it.code in changedCodes }

                if (changedParts.isEmpty()) {
                    Text("Parça bilgisi mevcut değil.", color = LightGray)
                } else {
                    changedParts.forEach {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("• ${it.name}", color = White, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Kod: ${it.code} | Adet: ${it.quantity}",
                                color = SoftBlue,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            )
                        }
                    }
                }
            }
        }

        // Fotoğraf klasörü adı gösterimi
        if (maintenance.photoFolderName.isNotBlank()) {
            InfoCard("Fotoğraf Klasörü") {
                Text(maintenance.photoFolderName, color = SoftBlue)
            }
        }

        // FOTOĞRAFLAR KARTI
        if (photoFiles.isNotEmpty()) {
            InfoCard("Bakım Fotoğrafları") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photoFiles) { file ->
                        Card(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFile = file },
                            colors = CardDefaults.cardColors(containerColor = CardDark)
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // BÜYÜK FOTO DİALOG
        if (selectedFile != null) {
            AlertDialog(
                onDismissRequest = { selectedFile = null },
                confirmButton = {
                    TextButton(onClick = { selectedFile = null }) {
                        Text("Kapat", color = RedPrimary)
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = selectedFile,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedFile?.name ?: "",
                            color = White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                containerColor = CardDark
            )
        }
    }
}

@Composable
fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                color = White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LightGray)
        Text(value, color = White, fontWeight = FontWeight.SemiBold)
    }
}
