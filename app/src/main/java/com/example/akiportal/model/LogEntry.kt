package com.example.akiportal.model

data class LogEntry(
    val userEmail: String = "",
    val timestamp: String = "",
    val action: String = "",
    val details: Map<String, Any>? = null
)
