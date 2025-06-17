package com.example.akiportal.screen.maintenance

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.akiportal.model.Maintenance
import com.example.akiportal.model.Material
import com.example.akiportal.model.SparePart
import com.example.akiportal.model.User
import com.example.akiportal.ui.theme.*
import com.example.akiportal.ui.ui.RedTopBar
import com.example.akiportal.viewmodel.MaintenanceViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.model.Machine
import com.example.akiportal.ui.theme.SoftBlue
import com.example.akiportal.ui.ui.HyperDropdown
import com.example.akiportal.ui.ui.SuperDropdown


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenancePlanningScreen(
    selectedMachines: List<Machine>,
    bakimViewModel: MaintenanceViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val firestore = FirebaseFirestore.getInstance()
    val userList = remember { mutableStateListOf<User>() }
    val allMaterials = remember { mutableStateListOf<Material>() }
    val visibleParts = remember { mutableStateListOf<DisplayPart>() }

    LaunchedEffect(Unit) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val users = result.mapNotNull { it.toObject(User::class.java) }
                userList.clear()
                userList.addAll(users)
            }
    }
    LaunchedEffect(Unit) {
        firestore.collection("materials")
            .get()
            .addOnSuccessListener { result ->
                allMaterials.clear()
                allMaterials.addAll(result.mapNotNull { it.toObject(Material::class.java) })
            }
    }


    val loadedMaterials = remember { mutableStateListOf<Material>() }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isLoaded) {
            FirebaseFirestore.getInstance().collection("materials")
                .get()
                .addOnSuccessListener { documents ->
                    val tempList = mutableListOf<Material>()
                    for (doc in documents) {
                        val category = doc.id
                        val icerik = doc.get("icerik") as? Map<*, *>
                        icerik?.forEach { (_, data) ->
                            if (data is Map<*, *>) {
                                val code = data["code"] as? String ?: return@forEach
                                val description = data["description"] as? String ?: ""
                                val shelf = data["shelf"] as? String ?: ""
                                val stock = (data["stock"] as? Long)?.toInt() ?: 0
                                val kritikStok = (data["kritikStok"] as? Long)?.toInt() ?: 0

                                tempList.add(
                                    Material(
                                        code = code,
                                        description = description,
                                        shelf = shelf,
                                        stock = stock,
                                        kritikStok = kritikStok,
                                        category = category
                                    )
                                )
                            }
                        }
                    }
                    loadedMaterials.clear()
                    loadedMaterials.addAll(tempList)
                    isLoaded = true
                }
        }
    }



    var globalDate by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val maintenanceData = remember {
        mutableStateListOf<MaintenanceFormData>().apply {
            selectedMachines.forEach { machine ->
                val defaultParts = if (machine.type == "kurutucu") {
                    listOf(
                        SparePart(
                            name = "Kurutucu Filtresi",
                            code = machine.dryerFilterCode,
                            quantity = machine.dryerFilterCount
                        )
                    )
                } else {
                    listOf(
                        SparePart(
                            name = "Hava Filtresi",
                            code = machine.airFilterCode,
                            quantity = machine.airFilterCount
                        ),
                        SparePart(
                            name = "Yağ Filtresi",
                            code = machine.oilFilterCode,
                            quantity = machine.oilFilterCount
                        ),
                        SparePart(
                            name = "Separatör",
                            code = machine.separatorCode,
                            quantity = machine.separatorCount
                        )
                    )
                }
                add(
                    MaintenanceFormData(
                        machine = machine,
                        oilCode = machine.oilCode,
                        oilLiter = machine.oilLiter,
                        parts = defaultParts.toMutableList()
                    )
                )

            }
        }
    }

    Scaffold(
        topBar = { RedTopBar(title = "Bakım Planla") },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            if (maintenanceData.size > 1) {
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                globalDate = "%02d.%02d.%04d".format(day, month + 1, year)
                                maintenanceData.forEachIndexed { index, form ->
                                    // Sadece boş olanlara uygula
                                    if (form.date.isBlank()) {
                                        maintenanceData[index] = form.copy(date = globalDate)
                                    }
                                }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(RedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Ortak Tarih Seç", color = White)
                }
            }

            // Birden fazla ortak sorumlu tutmak için liste
            val selectedResponsibles = remember { mutableStateListOf<User>() }
            var responsibleSearch by remember { mutableStateOf("") }
            var responsibleExpanded by remember { mutableStateOf(false) }

            if (maintenanceData.size > 1) {
                // Dropdown arama alanı
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
                        label = { Text("Ortak Sorumlu Kişi Ekle") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = responsibleExpanded)
                        }
                    )

                    val filtered = userList.filter {
                        it.fullName.contains(responsibleSearch, ignoreCase = true) ||
                                it.email.contains(responsibleSearch, ignoreCase = true)
                    }.filterNot { user ->
                        selectedResponsibles.any { it.fullName == user.fullName }
                    }

                    ExposedDropdownMenu(
                        expanded = responsibleExpanded,
                        onDismissRequest = { responsibleExpanded = false }
                    ) {
                        filtered.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.fullName) },
                                onClick = {
                                    selectedResponsibles.add(user)
                                    responsibleSearch = "" // temizle
                                    responsibleExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Seçilen kişiler listesi
                Column {
                    selectedResponsibles.forEach { user ->
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
                                    text = user.fullName,
                                    color = White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )

                                IconButton(onClick = {
                                    selectedResponsibles.remove(user)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Listeden Kaldır",
                                        tint = RedPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (selectedResponsibles.isEmpty()) {
                        Text(
                            text = "Henüz ortak sorumlu seçilmedi.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ortak sorumlu uygula butonu
                Button(
                    onClick = {
                        selectedResponsibles.forEach { user ->
                            maintenanceData.forEachIndexed { i, form ->
                                val updated = form.responsibles.toMutableList()
                                if (user.fullName !in updated) {
                                    updated.add(user.fullName)
                                    maintenanceData[i] = form.copy(responsibles = updated)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Ortak Sorumluları Uygula", color = White)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }


            Divider(
                color = Color.Gray,
                thickness = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            maintenanceData.forEachIndexed { index, form ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(58.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    RedPrimary,
                                    RedPrimary.copy(alpha = 0.7f),
                                    RedPrimary.copy(alpha = 0.5f),
                                    RedPrimary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = form.machine.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }



                OutlinedTextField(
                    value = form.description,
                    onValueChange = { maintenanceData[index] = form.copy(description = it) },
                    label = { Text("Açıklama") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val displayDate = if (form.date.isNotBlank()) form.date else globalDate
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                maintenanceData[index] =
                                    form.copy(date = "%02d.%02d.%04d".format(day, month + 1, year))
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(RedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (form.date.isNotBlank()) form.date else globalDate.ifBlank { "Tarih Seç" },
                        color = White
                    )
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
                ){}

                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {

                    // 🔍 Ekstra malzeme autocomplete alanı
                    ExtraMaterialInputRow(
                        onAdd = { sparePart, category ->

                            val alreadyInMainParts = form.parts.any { it.code == sparePart.code }
                            val alreadyInExtras = form.extraParts.any { it.code == sparePart.code }

                            if (!alreadyInMainParts && !alreadyInExtras) {
                                val updated = form.extraParts.toMutableList()
                                updated.add(sparePart)
                                maintenanceData[index] = form.copy(extraParts = updated)
                            }
                        }
                    )

                    val buttonSize = 15.dp
                    val iconSize = 30.dp

// Ana malzeme kartları
                    form.parts.forEachIndexed { partIndex, part ->
                        val categoryLabel = part.name

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(IntrinsicSize.Min),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = categoryLabel,
                                        color = White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = part.name,
                                        color = White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = part.code,
                                    color = LightGray,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val updated = form.parts.toMutableList()
                                            if (part.quantity > 1) {
                                                updated[partIndex] = part.copy(quantity = part.quantity - 1)
                                            } else {
                                                updated.removeAt(partIndex)
                                            }
                                            maintenanceData[index] = form.copy(parts = updated)
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .background(RedPrimary, shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (part.quantity > 1) Icons.Default.Remove else Icons.Default.Delete,
                                            contentDescription = if (part.quantity > 1) "Azalt" else "Sil",
                                            tint = White,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color.Transparent)
                                            .padding(horizontal = 20.dp)
                                            .height(buttonSize)
                                            .wrapContentWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${part.quantity}",
                                            color = White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }


                                    IconButton(
                                        onClick = {
                                            val updated = form.parts.toMutableList()
                                            updated[partIndex] = part.copy(quantity = part.quantity + 1)
                                            maintenanceData[index] = form.copy(parts = updated)
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .background(RedPrimary, shape = CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Artır",
                                            tint = White,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }
                                }
                            }
                        }
                    }// 🔥 Sadece kompresörlerde yağ bilgisi göster
                    if (form.machine.type == "kompresör") {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = "Yağ",
                                        color = White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = form.oilCode,
                                        color = White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = "${String.format("%.1f", form.oilLiter)} L",
                                    color = LightGray,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val isLow = form.oilLiter <= 5.0

                                    IconButton(
                                        onClick = {
                                            maintenanceData[index] = form.copy(
                                                oilLiter = if (isLow) 0.0 else (form.oilLiter - 5.0).coerceAtLeast(0.0)
                                            )
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .background(RedPrimary, shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isLow) Icons.Default.Delete else Icons.Default.Remove,
                                            contentDescription = if (isLow) "Sıfırla" else "Azalt",
                                            tint = White,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color.Transparent)
                                            .padding(horizontal = 20.dp)
                                            .height(buttonSize)
                                            .wrapContentWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f", form.oilLiter)}",
                                            color = White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            maintenanceData[index] = form.copy(
                                                oilLiter = form.oilLiter + 5.0
                                            )
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .background(RedPrimary, shape = CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Artır",
                                            tint = White,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }
                                }
                            }
                        }
                    }



// Ekstra malzeme kartları
                    form.extraParts.forEachIndexed { extraIndex, part ->
                        val matchedMaterial = loadedMaterials.firstOrNull { it.code == part.code }
                        val categoryLabel = matchedMaterial?.category ?: "Serbest Malzeme"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = categoryLabel,
                                        color = White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = part.name,
                                        color = White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = part.code,
                                    color = LightGray,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val updated = form.extraParts.toMutableList()
                                            if (part.quantity > 1) {
                                                updated[extraIndex] = part.copy(quantity = part.quantity - 1)
                                            } else {
                                                updated.removeAt(extraIndex)
                                            }
                                            maintenanceData[index] = form.copy(extraParts = updated)
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .background(RedPrimary, shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (part.quantity > 1) Icons.Default.Remove else Icons.Default.Delete,
                                            contentDescription = "Azalt veya Sil",
                                            tint = White,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color.Transparent)
                                            .padding(horizontal = 20.dp)
                                            .height(buttonSize)
                                            .wrapContentWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${part.quantity}",
                                            color = White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val updated = form.extraParts.toMutableList()
                                            updated[extraIndex] = part.copy(quantity = part.quantity + 1)
                                            maintenanceData[index] = form.copy(extraParts = updated)
                                        },
                                        modifier = Modifier
                                            .size(buttonSize)
                                            .background(RedPrimary, shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Artır",
                                            tint = White,
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }
                                }
                            }
                        }
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
                ){}

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    form.preNote,
                    { maintenanceData[index] = form.copy(preNote = it) },
                    label = { Text("Bakım Önü Not") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sorumlu Kişiler",
                    color = White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 🔽 Her form (makine) için yerel sorumlu kişi ekleme alanı
                var localResponsibleSearch by remember { mutableStateOf("") }
                var localResponsibleExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = localResponsibleExpanded,
                    onExpandedChange = { localResponsibleExpanded = !localResponsibleExpanded }
                ) {
                    OutlinedTextField(
                        value = localResponsibleSearch,
                        onValueChange = {
                            localResponsibleSearch = it
                            localResponsibleExpanded = true
                        },
                        label = { Text("Yerel Sorumlu Kişi Ekle") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = localResponsibleExpanded)
                        }
                    )

                    val filtered = userList.filter {
                        it.fullName.contains(localResponsibleSearch, ignoreCase = true) ||
                                it.email.contains(localResponsibleSearch, ignoreCase = true)
                    }.filterNot { user ->
                        user.fullName in form.responsibles
                    }

                    ExposedDropdownMenu(
                        expanded = localResponsibleExpanded,
                        onDismissRequest = { localResponsibleExpanded = false }
                    ) {
                        filtered.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.fullName) },
                                onClick = {
                                    val updated = form.responsibles.toMutableList()
                                    updated.add(user.fullName)
                                    maintenanceData[index] = form.copy(responsibles = updated)

                                    localResponsibleSearch = ""
                                    localResponsibleExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

// 🔽 Liste: Eklenen yerel sorumlular
                Column {
                    form.responsibles.forEach { responsibleName ->
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
                                        val updated = form.responsibles.toMutableList()
                                        updated.remove(responsibleName)
                                        maintenanceData[index] = form.copy(responsibles = updated)
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

                    if (form.responsibles.isEmpty()) {
                        Text(
                            text = "Henüz yerel sorumlu eklenmedi.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showError) {
                Text(
                    "Lütfen tüm gerekli alanları doldurun!",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (maintenanceData.any { it.description.isBlank() || it.date.isBlank() }) {
                        showError = true
                    } else {
                        maintenanceData.forEach { form ->
                            val maintenance = Maintenance(
                                machineId = form.machine.id,
                                machineName = form.machine.name,
                                companyId = form.machine.companyId,
                                companyName = form.machine.companyName,
                                plannedDate = form.date,
                                description = form.description,
                                preMaintenanceNote = form.preNote,
                                parts = form.parts,
                                oilCode = form.oilCode,
                                oilLiter = form.oilLiter,
                                extraParts = form.extraParts,
                                status = "planlandı",
                                responsibles = form.responsibles
                            )

                            bakimViewModel.planla(maintenance) { }
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(SoftBlue)
            ) {
                Text("Bakımları Planla", color = White)
            }
        }
    }
}

data class DisplayPart(
    val part: SparePart,
    val isExtra: Boolean = false,
    val category: String? = null
)
data class MaintenanceFormData(
    val machine: Machine,
    val description: String = "",
    val date: String = "",
    val oilCode: String = "",
    val oilLiter: Double = 0.0,
    val parts: MutableList<SparePart> = mutableListOf(),
    val extraParts: MutableList<SparePart> = mutableListOf(),
    val preNote: String = "",
    val responsibles: MutableList<String> = mutableListOf()


)@Composable
fun ExtraMaterialInputRow(
    onAdd: (SparePart, category: String?) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val loadedMaterials = remember { mutableStateListOf<Material>() }
    var isLoaded by remember { mutableStateOf(false) }

    var newCode by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }

    Column {
        // 🔄 İlk veri çekme işlemi
        if (!isLoaded) {
            LaunchedEffect(true) {
                firestore.collection("materials")
                    .get()
                    .addOnSuccessListener { documents ->
                        val tempList = mutableListOf<Material>()
                        for (doc in documents) {
                            val category = doc.id
                            val icerik = doc.get("icerik") as? Map<*, *>
                            icerik?.forEach { (_, data) ->
                                if (data is Map<*, *>) {
                                    val code = data["code"] as? String ?: return@forEach
                                    val description = data["description"] as? String ?: ""
                                    val shelf = data["shelf"] as? String ?: ""
                                    val stock = (data["stock"] as? Long)?.toInt() ?: 0
                                    val kritikStok = (data["kritikStok"] as? Long)?.toInt() ?: 0

                                    tempList.add(
                                        Material(
                                            code = code,
                                            description = description,
                                            shelf = shelf,
                                            stock = stock,
                                            kritikStok = kritikStok,
                                            category = category
                                        )
                                    )
                                }
                            }
                        }
                        loadedMaterials.clear()
                        loadedMaterials.addAll(tempList)
                        isLoaded = true
                    }
            }
        }

        // 🔽 Kod seçimi alanı
        HyperDropdown(
            label = "Malzeme Kodu",
            options = loadedMaterials.map { it.code },
            selectedValue = newCode,
            onValueChange = { selected -> newCode = selected },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 🔢 Adet ve Ekleme butonu
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newQty,
                onValueChange = { newQty = it },
                label = { Text("Adet") },
                modifier = Modifier.width(100.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                colors = ButtonDefaults.buttonColors(RedPrimary),
                onClick = {
                    if (newCode.isNotBlank()) {
                        val selectedMaterial = loadedMaterials.firstOrNull {
                            it.code.trim().equals(newCode.trim(), ignoreCase = true)
                        }

                        val sparePart = SparePart(
                            name = selectedMaterial?.description ?: "Tanımsız Malzeme",
                            code = newCode.trim(),
                            quantity = newQty.toIntOrNull() ?: 1
                        )

                        val category = selectedMaterial?.category ?: "Serbest Malzeme"

                        onAdd(sparePart, category)

                        // 🔄 Temizleme
                        newCode = ""
                        newQty = "1"
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = White)
                Text("Ekle", color = White)
            }
        }
    }
}

