package com.example.akiportal.screen.material

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.Material
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun MaterialDialog(
    material: Material? = null,
    category: String = "",
    onDismiss: () -> Unit,
    onSave: (Material) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var code by remember { mutableStateOf(material?.code ?: "") }
    var shelf by remember { mutableStateOf(material?.shelf ?: "") }
    var stock by remember { mutableStateOf(material?.stock?.toString() ?: "") }
    var kritikStok by remember { mutableStateOf(material?.kritikStok?.toString() ?: "") }
    var description by remember { mutableStateOf(material?.description ?: "") }

    val isEditing = material != null
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Snackbar tetikleyici
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Box {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (isEditing) "Malzemeyi Güncelle" else "Yeni Malzeme Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Kod*") })
                    OutlinedTextField(value = shelf, onValueChange = { shelf = it }, label = { Text("Raf*") })
                    OutlinedTextField(value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } }, label = { Text("Stok Adedi*") })
                    OutlinedTextField(value = kritikStok, onValueChange = { kritikStok = it.filter { c -> c.isDigit() } }, label = { Text("Kritik Stok*") })
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Açıklama (İsteğe Bağlı)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val stockInt = stock.toIntOrNull() ?: -1
                    val kritikInt = kritikStok.toIntOrNull() ?: -1
                    val firestore = FirebaseFirestore.getInstance()

                    if (code.isBlank() || shelf.isBlank() || stock.isBlank() || kritikStok.isBlank() || stockInt < 0 || kritikInt < 0) {
                        snackbarMessage = "Lütfen * işaretli alanları doğru doldurun."
                        return@TextButton
                    }

                    val newMaterial = Material(
                        code = code.trim(),
                        shelf = shelf.trim(),
                        category = material?.category ?: category,
                        stock = stockInt,
                        kritikStok = kritikInt,
                        description = description.trim()
                    )

                    val matData = mapOf(
                        "code" to newMaterial.code,
                        "shelf" to newMaterial.shelf,
                        "stock" to newMaterial.stock,
                        "kritikStok" to newMaterial.kritikStok,
                        "description" to newMaterial.description
                    )

                    if (isEditing && material != null) {
                        // Güncelleme işlemi
                        firestore.collection("materials")
                            .document(material.category)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val icerik = snapshot.get("icerik") as? Map<*, *> ?: return@addOnSuccessListener
                                val matchingKey = icerik.entries.find {
                                    (it.value as? Map<*, *>)?.get("code") == material.code
                                }?.key as? String ?: return@addOnSuccessListener

                                firestore.collection("materials")
                                    .document(material.category)
                                    .update("icerik.$matchingKey", matData)
                                    .addOnSuccessListener {
                                        onSave(newMaterial)
                                        onDismiss()
                                    }
                            }
                    } else {
                        // Yeni ekleme işlemi
                        firestore.collection("materials")
                            .document(newMaterial.category)
                            .get()
                            .addOnSuccessListener { doc ->
                                val existing = doc.get("icerik") as? Map<*, *> ?: emptyMap<String, Any>()
                                val alreadyExists = existing.values.any {
                                    (it as? Map<*, *>)?.get("code") == newMaterial.code
                                }

                                if (alreadyExists) {
                                    snackbarMessage = "Bu kodla kayıtlı bir malzeme zaten var."
                                } else {
                                    val uuid = UUID.randomUUID().toString()
                                    firestore.collection("materials")
                                        .document(newMaterial.category)
                                        .update("icerik.$uuid", matData)
                                        .addOnSuccessListener {
                                            onSave(newMaterial)
                                            onDismiss()
                                        }
                                        .addOnFailureListener {
                                            firestore.collection("materials")
                                                .document(newMaterial.category)
                                                .set(mapOf("icerik" to mapOf(uuid to matData)))
                                                .addOnSuccessListener {
                                                    onSave(newMaterial)
                                                    onDismiss()
                                                }
                                        }
                                }
                            }
                    }
                }) {
                    Text(if (isEditing) "Kaydet" else "Ekle")
                }
            },
            dismissButton = {
                Row {
                    if (isEditing && onDelete != null) {
                        TextButton(onClick = onDelete) { Text("Sil") }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = onDismiss) { Text("İptal") }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
