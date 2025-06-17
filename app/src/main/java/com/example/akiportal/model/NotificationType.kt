package com.example.akiportal.model

enum class NotificationType {
    WARNING, INFO, SUCCESS
}

data class NotificationItem(
    val id: String,                         // Eşsiz ID
    val message: String,                   // Gösterilecek metin
    val type: NotificationType,            // Uyarı türü (kırmızı/sarı/yeşil)
    val timestamp: String,                 // Oluşturulma zamanı
    val isPersistent: Boolean = false      // Sabit mi (örn: malzeme listesi gibi)
)
