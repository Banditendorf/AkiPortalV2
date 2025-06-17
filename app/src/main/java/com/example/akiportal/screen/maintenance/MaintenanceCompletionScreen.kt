package com.example.akiportal.screen.maintenance

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.akiportal.model.Maintenance
import com.example.akiportal.model.SparePart
import com.example.akiportal.model.User
import com.example.akiportal.ui.theme.*
import com.example.akiportal.ui.ui.RedTopBar
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Add
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.akiportal.util.PhotoStorageHelper
import com.example.akiportal.viewmodel.MaterialViewModel
import androidx.compose.material.icons.filled.Remove
import com.example.akiportal.ui.ui.HyperDropdown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceCompletionScreen(
    maintenance: Maintenance,
    onComplete: () -> Unit,
    materialViewModel: MaterialViewModel  // Parametreyle gelen ViewModel’i kullanıyoruz
) {
    // Context ve Firestore
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // 1️⃣ Bakım’ın nullable alanları
    var description by remember { mutableStateOf(maintenance.description.orEmpty()) }
    var endNote by remember { mutableStateOf(maintenance.postMaintenanceNote.orEmpty()) }
    var workOrderNumber by remember { mutableStateOf(maintenance.workOrderNumber.orEmpty()) }
    var startTime by remember { mutableStateOf(maintenance.startTime.orEmpty()) }
    var endTime by remember { mutableStateOf(maintenance.endTime.orEmpty()) }
    var workingHour by remember { mutableStateOf(maintenance.workingHourAtMaintenance?.toString() ?: "") }
    var nextMaintenanceHour by remember { mutableStateOf(maintenance.nextMaintenanceTime.orEmpty()) }

    // 2️⃣ Fotoğraf URI’ları
    val photoUris = remember { mutableStateListOf<Uri>() }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        photoUris.clear()
        photoUris.addAll(uris)
    }

    // 3️⃣ Firestore’daki malzemelerden kod ve kategori bilgisi
    val materials by materialViewModel.materials.collectAsState()
    val materialCodes = remember(materials) { materials.map { it.code } }
    val categoryMap = remember(materials) { materials.associate { it.code to it.category } }

    // 4️⃣ Yağ parçası (planlamada quantity yoksa 0)
    val oilPart = SparePart(
        name = "${maintenance.oilCode} ${maintenance.oilLiter}",
        code = maintenance.oilCode,
        quantity = 0
    )

    // 5️⃣ Tüm parçalar — planlama, ekstra ve yağ
    val allParts = remember {
        (maintenance.parts + maintenance.extraParts + listOf(oilPart))
            .toMutableStateList()
    }

    // 6️⃣ Yeni parça ekleme girdisi
    var newPartName by remember { mutableStateOf("") }

    // 7️⃣ Değiştirilen parçalar haritası (başlangıçta hepsi seçili)
    val changedParts = remember {
        mutableStateMapOf<SparePart, Boolean>().apply {
            allParts.forEach { put(it, true) }
        }
    }

    // 8️⃣ Parça adetleri haritası (planlamadakiler kadar, fazlası 0)
    val partCounts = remember {
        mutableStateMapOf<SparePart, Int>().apply {
            allParts.forEach { put(it, it.quantity) }
        }
    }

    Scaffold(
        topBar = {
            RedTopBar(title = "Bakım Tamamlama")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // <-- Bunu ekle
                .padding(innerPadding)
                .padding(16.dp)
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
                        "Şirket: ${maintenance.companyName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Makine: ${maintenance.machineName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Seri No: ${maintenance.serialNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = White),
                colors = textFieldColors(),
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                SoftBlue,
                                SoftBlue.copy(alpha = 0.7f),
                                SoftBlue.copy(alpha = 0.5f),
                                SoftBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {}

            Spacer(modifier = Modifier.height(8.dp))


            // Changed Parts
            // “Değiştirilen Parçalar” Card bloğu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, BorderGray, MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column {
                    // ─── Başlık ───
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGray)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Değiştirilen Parçalar",
                            color = White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // ─── Parça Listesi ───
                    allParts.forEach { part ->
                        val isChecked = changedParts[part] == true
                        val count = partCounts[part] ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardDark)
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    changedParts[part] = checked
                                    if (!checked) partCounts[part] = 0
                                    else if (partCounts[part] == 0) partCounts[part] = 1
                                },
                                colors = CheckboxDefaults.colors(checkedColor = RedPrimary)
                            )

                            // 2️⃣ Şimdi kategori adını gösteriyoruz:
                            val displayText = if (part == oilPart) {
                                part.name
                            } else {
                                val cat = categoryMap[part.code] ?: part.name
                                "$cat (${part.code})"
                            }

                            Text(
                                text = displayText,
                                color = if (isChecked) LightGray else LightGray.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 8.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // – / + butonları yalnızca işaretliyse
                            if (isChecked) {
                                IconButton(onClick = {
                                    if (count > 1) partCounts[part] = count - 1
                                    else {
                                        changedParts[part] = false
                                        partCounts[part] = 0
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (count > 1) Icons.Default.Remove else Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = LightGray
                                    )
                                }

                                Text(
                                    text = "$count",
                                    color = LightGray,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(onClick = {
                                    partCounts[part] = count + 1
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = LightGray)
                                }
                            }
                        }
                        Divider(color = BorderGray, thickness = 0.5.dp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ─── Yeni parça ekleme satırı ───
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HyperDropdown(
                            label = "Yeni Parça Kodu",
                            options = materialCodes,
                            selectedValue = newPartName,
                            onValueChange = { newPartName = it },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (newPartName.isNotBlank()) {
                                    // seçilen koda karşılık modelden açıklamayı bul (veya kodu isme çevir)
                                    val desc = materials.firstOrNull { it.code == newPartName }?.description ?: newPartName
                                    val p = SparePart(name = desc, code = newPartName, quantity = 1)
                                    allParts += p
                                    changedParts[p] = true
                                    partCounts[p] = 1
                                    newPartName = ""
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Parça Ekle", tint = RedPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // End Note & Work Order
            OutlinedTextField(
                value = endNote,
                onValueChange = { endNote = it },
                label = { Text("Bakım Sonu Not") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = White),
                colors = textFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = workOrderNumber,
                onValueChange = { workOrderNumber = it },
                label = { Text("İş Emri Numarası") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = White),
                colors = textFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                SoftBlue,
                                SoftBlue.copy(alpha = 0.7f),
                                SoftBlue.copy(alpha = 0.5f),
                                SoftBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {}
            // Saat ve saatlerle ilgili alanlar:
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TimeField(
                        label = "Başlangıç Saati",
                        value = startTime,
                        onTimeSelected = { startTime = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = workingHour,
                        onValueChange = { workingHour = it.filter { c -> c.isDigit() } },
                        label = { Text("Makina Çalışma Saati", color = LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = White),
                        colors = textFieldColors()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    TimeField(
                        label = "Bitiş Saati",
                        value = endTime,
                        onTimeSelected = { endTime = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nextMaintenanceHour,
                        onValueChange = { nextMaintenanceHour = it.filter { c -> c.isDigit() } },
                        label = { Text("Sıradaki Bakım Saati", color = LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = White),
                        colors = textFieldColors()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                SoftBlue,
                                SoftBlue.copy(alpha = 0.7f),
                                SoftBlue.copy(alpha = 0.5f),
                                SoftBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {}
// State
            // State
            var responsibles by remember { mutableStateOf(maintenance.responsibles.toMutableList()) }
            var responsibleSearch by remember { mutableStateOf("") }
            var responsibleExpanded by remember { mutableStateOf(false) }
            var userList by remember { mutableStateOf(listOf<User>()) }

// Firestore'dan userList çekme
            LaunchedEffect(Unit) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("active", true) // aktif kullanıcılar gelsin
                    .get()
                    .addOnSuccessListener { snapshot ->
                        userList = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                    }
            }

// Dropdown + TextField
            ExposedDropdownMenuBox(
                expanded = responsibleExpanded,
                onExpandedChange = { responsibleExpanded = !responsibleExpanded }
            ) {
                OutlinedTextField(
                    value = responsibleSearch,
                    onValueChange = {
                        responsibleSearch = it
                        responsibleExpanded = true
                    },
                    label = { Text("Sorumlu Kişi Ekle") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = responsibleExpanded)
                    }
                )
                val filtered = userList.filter { user ->
                    user.fullName.contains(responsibleSearch, ignoreCase = true) ||
                            user.email.contains(responsibleSearch, ignoreCase = true)
                }.filterNot { user ->
                    user.fullName in responsibles
                }

                ExposedDropdownMenu(
                    expanded = responsibleExpanded && filtered.isNotEmpty(),
                    onDismissRequest = { responsibleExpanded = false }
                ) {
                    filtered.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.fullName) },
                            onClick = {
                                responsibles.add(user.fullName)
                                responsibleSearch = ""
                                responsibleExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

// Liste: Eklenen sorumlular
            Column {
                responsibles.forEach { responsibleName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = responsibleName,
                                color = White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = {
                                    responsibles.remove(responsibleName)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Sorumlu Kaldır",
                                    tint = RedPrimary
                                )
                            }
                        }
                    }
                }
                if (responsibles.isEmpty()) {
                    Text(
                        text = "Henüz sorumlu kişi eklenmedi.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }



            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                SoftBlue,
                                SoftBlue.copy(alpha = 0.7f),
                                SoftBlue.copy(alpha = 0.5f),
                                SoftBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {}
            // Electrical Measurements (3 faz için virgülle ayır)
            var voltagePhases by remember { mutableStateOf("") }
            OutlinedTextField(
                value = voltagePhases,
                onValueChange = { input -> voltagePhases = input.replace(" ", ",") },
                label = { Text("Gerilim (L1, L2, L3)", color = LightGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))


            var currentPhases by remember { mutableStateOf("") }

            OutlinedTextField(
                value = currentPhases,
                onValueChange = { input -> currentPhases = input.replace(" ", ",") },
                label = { Text("Akım (L1, L2, L3)", color = LightGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Fotoğraf Ekle", color = White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val chunks = photoUris.chunked(2)

                    chunks.forEach { rowUris ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowUris.forEach { uri ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    IconButton(
                                        onClick = {
                                            photoUris.remove(uri)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(28.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(50)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = RedPrimary
                                        )
                                    }
                                }
                            }

                            if (rowUris.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (photoUris.isEmpty()) {
                        Text(
                            text = "Henüz fotoğraf eklenmedi.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Button(
                onClick = {
                    // 1️⃣ Fotoğrafları kaydetme
                    val savedFiles = PhotoStorageHelper.saveUrisToMaintenanceDir(
                        context = context,
                        uris = photoUris,
                        machineId = maintenance.machineId,
                        maintenanceId = maintenance.id
                    )
                    val photoFolderName = "akiportal/${maintenance.machineId}/${maintenance.id}"

                    // 2️⃣ Kullanılan parçaları (seçili ve adet > 0) filtrele
                    val usedParts = partCounts
                        .filter { (part, qty) -> changedParts[part] == true && qty > 0 }
                        .map { (part, qty) -> part to qty }

                    // 3️⃣ Stokları parçanın kodu ve adedi kadar düşür
                    usedParts.forEach { (part, qty) ->
                        materialViewModel.decreaseStock(part.code, qty)
                    }

                    // 4️⃣ “Tamamlanan” bakımı Firestore’a yaz
                    val workingHourInt = workingHour.toIntOrNull() ?: 0
                    val nextMaintenanceHourInt = nextMaintenanceHour.toIntOrNull() ?: 0
                    val updated = maintenance.copy(
                        description       = description,
                        postMaintenanceNote = endNote,
                        workOrderNumber   = workOrderNumber,
                        startTime         = startTime,
                        endTime           = endTime,
                        changedParts      = usedParts.map { it.first.code },
                        oilChanged        = usedParts.any { it.first.code == oilPart.code },
                        oilCode           = maintenance.oilCode,
                        oilLiter          = maintenance.oilLiter,
                        timestamp         = System.currentTimeMillis(),
                        status            = "tamamlandı",
                        responsibles      = responsibles,
                        photoFolderName   = photoFolderName
                    )

                    firestore.collection("plannedMaintenances")
                        .document(maintenance.id)
                        .set(updated)
                        .addOnSuccessListener {
                            // 5️⃣ Makine dökümünü güncelle
                            firestore.collection("machines")
                                .document(maintenance.machineId)
                                .update(
                                    mapOf(
                                        "estimatedHours" to workingHourInt,
                                        "nextMaintenanceHour" to nextMaintenanceHourInt
                                    )
                                )
                                .addOnSuccessListener {
                                    onComplete()
                                }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Text("Bakımı Tamamla", color = White)
            }
        }
    }
}



@Composable
fun TimeField(
            label: String,
            value: String,
            onTimeSelected: (String) -> Unit,
            modifier: Modifier = Modifier
) {
            val context = LocalContext.current
            val calendar = Calendar.getInstance()
            Button(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onTimeSelected("%02d:%02d".format(h, m)) },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                modifier = modifier,
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Text(text = if (value.isBlank()) label else "${label}: ${value}", color = White)
            }
        }

@Composable
        fun textFieldColors() = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = White,
            unfocusedBorderColor = LightGray,
            cursorColor = LightGray,
            focusedLabelColor = White,
            unfocusedLabelColor = LightGray,
            focusedTextColor = White,
            unfocusedTextColor = White,
            disabledTextColor = LightGray,
            errorTextColor = Color.Red
        )
