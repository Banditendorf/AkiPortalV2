package com.example.akiportal.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.akiportal.model.*
import com.example.akiportal.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class MaintenanceViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _notificationList = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationList: StateFlow<List<NotificationItem>> = _notificationList

    private val _maintenances = MutableStateFlow<List<Maintenance>>(emptyList())
    val maintenances: StateFlow<List<Maintenance>> = _maintenances

    val materials = MutableStateFlow<List<SparePart>>(emptyList())
    val userList = MutableStateFlow<List<User>>(emptyList())

    fun loadMaintenances(companyId: String) {
        db.collection("plannedMaintenances")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Maintenance::class.java) }
                _maintenances.value = list
                checkTodayMaintenances(list)
            }
    }

    fun planla(bakim: Maintenance, onComplete: () -> Unit) {
        val id = if (bakim.id.isBlank()) UUID.randomUUID().toString() else bakim.id

        db.collection("machines").document(bakim.machineId).get().addOnSuccessListener { snapshot ->
            val serial = snapshot.getString("serialNumber") ?: ""
            val companyId = snapshot.getString("companyId") ?: ""
            val companyName = snapshot.getString("companyName") ?: ""
            val machineName = snapshot.getString("name") ?: bakim.machineId

            val updatedBakim = bakim.copy(
                id = id,
                serialNumber = serial,
                companyId = companyId,
                companyName = companyName,
                machineName = machineName
            )

            val now = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault()).format(Date())
            val noti = NotificationItem(
                id = "bakim_${bakim.machineId}_$now",
                message = "Yeni bir bakım planlandı (${updatedBakim.machineName})",
                type = NotificationType.INFO,
                timestamp = now
            )

            _notificationList.value += noti

            db.collection("plannedMaintenances")
                .document(id)
                .set(updatedBakim)
                .addOnSuccessListener { onComplete() }
        }
    }

    fun updateMaintenance(maintenance: Maintenance, onComplete: () -> Unit) {
        db.collection("plannedMaintenances")
            .document(maintenance.id)
            .set(maintenance)
            .addOnSuccessListener { onComplete() }
    }

    fun updatePreparation(bakimId: String, parts: List<SparePart>) {
        db.collection("plannedMaintenances")
            .document(bakimId)
            .update("parts", parts)
    }

    fun getPlannedList(onResult: (List<Maintenance>) -> Unit) {
        db.collection("maintenances")
            .whereEqualTo("status", "planlandı") // opsiyonel filtre
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject(Maintenance::class.java) }
                onResult(list)
            }
    }

    fun tamamlandiOlarakIsaretle(context: Context, bakim: Maintenance) {
        db.collection("plannedMaintenances")
            .document(bakim.id)
            .update("status", "tamamlandı")
            .addOnSuccessListener {
                val now = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault()).format(Date())

                val message =
                    "${bakim.companyName} şirketin ${bakim.serialNumber} seri numaralı ${bakim.machineName} makinasının \"${bakim.description}\" işlemi tamamlandı."

                val notification = NotificationItem(
                    id = "tamam_${bakim.id}",
                    message = message,
                    type = NotificationType.SUCCESS,
                    timestamp = now
                )

                _notificationList.value += notification

                NotificationHelper.showNotification(
                    id = notification.id.hashCode(),
                    title = "Bakım Tamamlandı",
                    message = message
                )
            }
    }

    fun checkTodayMaintenances(allMaintenances: List<Maintenance>) {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("tr"))
        val today = LocalDate.now().format(formatter)

        val todayList = allMaintenances.filter { it.plannedDate == today }
        val now = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault()).format(Date())

        val newNotifications = todayList.mapNotNull { bakim ->
            NotificationItem(
                id = "today_${bakim.id}",
                message = "Bugün planlı bakım var (${bakim.machineName} - ${bakim.companyName})",
                type = NotificationType.INFO,
                timestamp = now
            )
        }.filterNot { newItem -> _notificationList.value.any { it.id == newItem.id } }

        _notificationList.value += newNotifications
    }

    fun decreaseStock(code: String, quantity: Int) {
        db.collection("materials").document(code).get().addOnSuccessListener { snapshot ->
            val currentStock = snapshot.getLong("stock") ?: 0L
            val newStock = (currentStock - quantity).coerceAtLeast(0)
            db.collection("materials").document(code).update("stock", newStock)
        }
    }
}
