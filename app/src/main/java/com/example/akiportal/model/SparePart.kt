package com.example.akiportal.model

data class SparePart(
    val id: String = "",         // Firestore ID (gerekirse)
    val code: String = "",        // Parça kodu
    val name: String = "",        // Parça adı
    val category: String = "",    // Kategori (örn: Yağ Filtresi, Hava Filtresi)
    val shelf: String = "",       // Raf kodu (örn: A3, B2)
    val brand: String = "",       // Marka (örn: Mann Filter)
    val quantity: Int = 1,        // Adet
    val prepared: Boolean? = null
)
