package com.example.akiportal.model

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "",
    val isActive: Boolean = true,
    val workPhone: String = "",
    val personalPhone: String = "",
    val permissions: UserPermissions = UserPermissions()
)
