package com.example.akiportal.model

data class Material(
    val code: String = "",
    val shelf: String = "",
    val category: String = "",
    val stock: Int = 0,
    val kritikStok: Int = 0,
    val description: String = "", // Açıklama isteğe bağlı olarak kalıyor
    val lastUsedTimestamp: Long? = null,
    val updatedAt: Long? = null

)
