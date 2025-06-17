package com.example.akiportal.viewmodel

import androidx.lifecycle.ViewModel
import com.example.akiportal.model.LogEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LogViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    init {
        fetchLogs()
    }

    fun fetchLogs() {
        FirebaseFirestore.getInstance()
            .collection("logs")
            .orderBy("timestamp") // istersen .limit(100)
            .get()
            .addOnSuccessListener { result ->
                val entries = result.documents.mapNotNull { it.toObject(LogEntry::class.java) }
                _logs.value = entries.reversed()
            }
    }
}
