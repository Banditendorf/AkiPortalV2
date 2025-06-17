package com.example.akiportal.screen.ocr

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.core.content.FileProvider
import java.io.File


@Composable
fun OcrUploadScreen(
    onExtracted: (String) -> Unit
) {
    val context = LocalContext.current
    val outputDirectory = context.cacheDir
    var extractedText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Kameraya kaydedilecek geçici dosya
    val photoFile = remember {
        File.createTempFile("ocr_image_", ".jpg", outputDirectory)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraLauncher.launch(imageUri!!)

        }) {
            Text("Kamerayı Aç")
        }

        imageUri?.let { uri ->
            val bitmap = remember(uri) {
                val stream = context.contentResolver.openInputStream(uri)
                stream?.use { BitmapFactory.decodeStream(it) }
            }

            bitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = null)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val image = InputImage.fromBitmap(it, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(image)
                        .addOnSuccessListener { result ->
                            extractedText = result.text
                            onExtracted(result.text)
                        }
                        .addOnFailureListener {
                            extractedText = "Hata: ${it.localizedMessage}"
                        }
                }) {
                    Text("OCR Taramasını Başlat")
                }
            }
        }

        if (extractedText.isNotBlank()) {
            Text("OCR Sonucu:", style = MaterialTheme.typography.titleMedium)
            Text(extractedText)
        }
    }
}
