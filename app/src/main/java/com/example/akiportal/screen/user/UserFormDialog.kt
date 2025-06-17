package com.example.akiportal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.akiportal.model.User
import com.example.akiportal.model.UserPermissions
import com.example.akiportal.ui.theme.BorderGray
import com.example.akiportal.ui.theme.RedPrimary
import com.example.akiportal.ui.theme.SurfaceDark
import com.example.akiportal.ui.theme.White
import java.util.*

@Composable
fun UserFormDialog(
    user: User? = null,
    onConfirm: (User, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = user != null
    var fullName by remember { mutableStateOf(user?.fullName.orEmpty()) }
    var email by remember { mutableStateOf(user?.email.orEmpty()) }
    var role by remember { mutableStateOf(user?.role.orEmpty()) }
    var workPhone by remember { mutableStateOf(user?.workPhone.orEmpty()) }
    var personalPhone by remember { mutableStateOf(user?.personalPhone.orEmpty()) }
    var isActive by remember { mutableStateOf(user?.isActive ?: true) }
    var permissions by remember { mutableStateOf(user?.permissions ?: UserPermissions()) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        ),
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        title = {
            Text(
                text = if (isEdit) "Kullanıcıyı Güncelle" else "Yeni Kullanıcı Ekle",
                style = MaterialTheme.typography.headlineSmall,
                color = White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = BorderGray,
                    cursorColor = RedPrimary,
                    focusedLabelColor = RedPrimary,
                    unfocusedLabelColor = White,
                    disabledLabelColor = White
                )

                // Temel kullanıcı alanları
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Ad Soyad") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEdit,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))
                if (!isEdit) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Şifre") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(icon, contentDescription = null, tint = RedPrimary)
                            }
                        },
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Rol") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = workPhone,
                    onValueChange = { workPhone = it },
                    label = { Text("İş Telefonu") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = personalPhone,
                    onValueChange = { personalPhone = it },
                    label = { Text("Kişisel Telefon") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = fieldColors
                )
                Spacer(Modifier.height(8.dp))

                // Aktiflik durumu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = RedPrimary,
                            uncheckedColor = White
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Aktif",
                        color = White,
                        modifier = Modifier.clickable { isActive = !isActive }
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Yetkiler başlığı ve Tüm Yetkiler butonu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Yetkiler",
                        style = MaterialTheme.typography.titleMedium,
                        color = White
                    )
                    TextButton(onClick = {
                        permissions = UserPermissions(
                            manageUser = true,
                            manageMachine = true,
                            manageCompany = true,
                            manageMaintenance = true,
                            manageCategory = true,
                            manageMaterial = true,
                            callCustomer = true,
                            viewCompanies = true,
                            viewMaintenancePlans = true,
                            viewPreparationLists = true,
                            viewMaterialsList = true,
                            viewUsers = true,
                            viewNotifications = true
                        )
                    }) {
                        Text("Tüm Yetkiler", color = RedPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))

                // İzin checkboxları
                PermissionCheckbox("Kullanıcı Yönet", permissions.manageUser) { permissions = permissions.copy(manageUser = it) }
                PermissionCheckbox("Makine Yönet", permissions.manageMachine) { permissions = permissions.copy(manageMachine = it) }
                PermissionCheckbox("Şirket Yönet", permissions.manageCompany) { permissions = permissions.copy(manageCompany = it) }
                PermissionCheckbox("Bakım Yönet", permissions.manageMaintenance) { permissions = permissions.copy(manageMaintenance = it) }
                PermissionCheckbox("Kategori Yönet", permissions.manageCategory) { permissions = permissions.copy(manageCategory = it) }
                PermissionCheckbox("Malzeme Yönet", permissions.manageMaterial) { permissions = permissions.copy(manageMaterial = it) }
                PermissionCheckbox("Müşteri Arama", permissions.callCustomer) { permissions = permissions.copy(callCustomer = it) }
                PermissionCheckbox("Şirketleri Görüntüle", permissions.viewCompanies) { permissions = permissions.copy(viewCompanies = it) }
                PermissionCheckbox("Planlanan Bakımları Görüntüle", permissions.viewMaintenancePlans) { permissions = permissions.copy(viewMaintenancePlans = it) }
                PermissionCheckbox("Hazırlanacak Listeleri Görüntüle", permissions.viewPreparationLists) { permissions = permissions.copy(viewPreparationLists = it) }
                PermissionCheckbox("Malzemeleri Görüntüle", permissions.viewMaterialsList) { permissions = permissions.copy(viewMaterialsList = it) }
                PermissionCheckbox("Kullanıcıları Görüntüle", permissions.viewUsers) { permissions = permissions.copy(viewUsers = it) }
                PermissionCheckbox("Bildirimleri Görüntüle", permissions.viewNotifications) { permissions = permissions.copy(viewNotifications = it) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val resultUser = user?.copy(
                    fullName = fullName,
                    role = role,
                    workPhone = workPhone,
                    personalPhone = personalPhone,
                    isActive = isActive,
                    permissions = permissions
                ) ?: User(
                    uid = UUID.randomUUID().toString(),
                    email = email,
                    fullName = fullName,
                    role = role,
                    workPhone = workPhone,
                    personalPhone = personalPhone,
                    isActive = isActive,
                    permissions = permissions
                )
                onConfirm(resultUser, if (isEdit) null else password)
            }) {
                Text(
                    text = if (isEdit) "Güncelle" else "Ekle",
                    color = RedPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = White)
            }
        }
    )
}

@Composable
private fun PermissionCheckbox(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(
                checkedColor = RedPrimary,
                uncheckedColor = White
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = White)
    }
}
