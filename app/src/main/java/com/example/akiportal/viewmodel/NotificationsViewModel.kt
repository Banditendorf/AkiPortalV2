package com.example.akiportal.viewmodel

import androidx.lifecycle.ViewModel
import com.example.akiportal.model.NotificationItem
import com.example.akiportal.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _notificationList = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationList: StateFlow<List<NotificationItem>> = _notificationList

    init {
        listenAllNotifications()
    }

    private fun listenAllNotifications() {
        db.collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: return@mapNotNull null
                    val message = doc.getString("message") ?: ""
                    val typeStr = doc.getString("type") ?: "INFO"
                    val timestamp = doc.getString("timestamp") ?: ""
                    val persistent = doc.getBoolean("isPersistent") ?: false

                    val type = try {
                        NotificationType.valueOf(typeStr)
                    } catch (e: Exception) {
                        NotificationType.INFO
                    }

                    NotificationItem(id, message, type, timestamp, persistent)
                }

                _notificationList.value = list.sortedByDescending { it.timestamp }
            }
    }

    /** Eğer UI’dan manuel olarak tetiklenecekse */
    fun loadNotifications() {
        listenAllNotifications()
    }

    /** Belirli bir bildirimi listeden çıkarır */
    fun removeNotification(item: NotificationItem) {
        _notificationList.value = _notificationList.value.filterNot { it.id == item.id }
    }

    /** Belirli bir bildirimi listenin sonuna taşır */
    fun moveNotificationToBottom(item: NotificationItem) {
        val current = _notificationList.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(item)
        _notificationList.value = current
    }

    /** Tüm bildirimleri temizler */
    fun clearAll() {
        _notificationList.value = emptyList()
    }
}
