package com.example.akiportal.model

data class Company(
    val id: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val contactNumber: String = "",
    val role: String = "",
    val location: String? = null,
    val note: String? = null,
    val latitude: Double? = null, // 🔥 Yeni
    val longitude: Double? = null // 🔥 Yeni
)
