package com.example.akiportal.model

data class Maintenance(
    // -- Genel Kimlik Bilgileri --
    val id: String = "",
    val machineId: String = "",
    val machineName: String = "",
    val serialNumber: String = "",
    val companyId: String = "",
    val companyName: String = "",

    // -- Planlama Bilgileri --
    val plannedDate: String = "",
    val plannedTime: String = "",
    val workOrderNumber: String = "",
    val status: String = "planlandı", // planlandı, hazırlandı, tamamlandı, iptal

    // -- Açıklama ve Notlar --
    val description: String = "",
    val note: String = "",
    val preMaintenanceNote: String = "",
    val postMaintenanceNote: String = "",

    // -- Zaman Bilgileri --
    val startTime: String = "",
    val endTime: String = "",

    // -- Parça ve Bakım Bilgileri --
    val parts: List<SparePart> = emptyList(),
    val extraParts: List<SparePart> = emptyList(),
    val changedParts: List<String> = emptyList(),

    // -- Sorumlu Personeller --
    val preparedBy: String = "",
    val checkedBy: String = "",
    val responsibles: List<String> = emptyList(),

    // -- Yağ Bilgisi --
    val oilChanged: Boolean = false,
    val oilCode: String = "",
    val oilLiter: Double = 0.0,

    // -- Ölçüm ve Teknik Veriler --
    val voltageL1: Float? = null,
    val currentL1: Float? = null,
    val pressure: Float? = null,

    // -- Fotoğraf & Dosya Bilgisi --
    val photoFolderName: String = "",

    // -- Makine Saat Bilgisi --
    val workingHourAtMaintenance: Int? = null,  // ← Yeni alan: bakım yapılan andaki makine saati

    // -- Sonraki Bakım Bilgisi --
    val nextMaintenanceTime: String = "",

    // -- Kayıt Zamanı --
    val timestamp: Long = 0L
)
