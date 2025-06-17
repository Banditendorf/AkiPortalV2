package com.example.akiportal.model

data class UserPermissions(
    val manageUser: Boolean = false,           // Kullanıcı ekle/sil/güncelle
    val manageMachine: Boolean = false,        // Makine ekle/sil/güncelle
    val manageCompany: Boolean = false,        // Şirket ekle/sil/güncelle
    val manageMaintenance: Boolean = false,    // Bakım kaydı ekle/sil/güncelle
    val manageCategory: Boolean = false,       // Kategori ekle/sil/güncelle
    val manageMaterial: Boolean = false,       // Malzeme ekle/sil/güncelle
    val callCustomer: Boolean = false,         // Müşteri arama / arama ekranına erişim
    val viewCompanies: Boolean = false,            // Şirketleri görüntüleme izni
    val viewMaintenancePlans: Boolean = false,     // Planlanan bakımları görüntüleme izni
    val viewPreparationLists: Boolean = false,     // Hazırlanacak listeleri görüntüleme izni
    val viewMaterialsList: Boolean = false,        // Malzemeleri görüntüleme izni
    val viewUsers: Boolean = false,                // Kullanıcıları görüntüleme izni
    val viewNotifications: Boolean = false         // Bildirimleri görüntüleme izni
)
