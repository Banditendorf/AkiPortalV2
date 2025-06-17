package com.example.akiportal.model

import androidx.annotation.Keep

@Keep
data class Machine(
    val id: String = "",
    val companyId: String = "",
    val companyName: String = "", // 🔥 yeni alan
    val name: String = "",
    val serialNumber: String = "",
    val note: String = "",
    val estimatedHours: Int = 0,
    val nextMaintenanceHour: Int = 0,

    val airFilterCode: String = "",
    val airFilterCount: Int = 0,

    val oilFilterCode: String = "",
    val oilFilterCount: Int = 0,

    val separatorCode: String = "",
    val separatorCount: Int = 0,

    val dryerFilterCode: String = "",
    val dryerFilterCount: Int = 0,

    val panelFilterSize: String = "",

    val oilCode: String = "",
    val oilLiter: Double = 0.0,

    val type: String = ""
)
