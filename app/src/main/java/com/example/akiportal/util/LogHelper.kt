package com.example.akiportal.util

import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object LogHelper {

    fun logFirebaseAction(
        userEmail: String,
        action: String,
        details: Map<String, Any>? = null
    ) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val log = hashMapOf<String, Any>(
            "userEmail" to userEmail,
            "timestamp" to sdf.format(Date()),
            "action" to action,
            "category" to resolveCategory(action)
        )
        details?.let { log["details"] = it }

        FirebaseFirestore.getInstance()
            .collection("logs")
            .add(log)
    }

    private fun resolveCategory(action: String): String {
        val lower = action.lowercase(Locale.getDefault())
        return when {
            listOf("company", "şirket").any { lower.contains(it) } -> "Company"
            listOf("machine", "makine").any { lower.contains(it) } -> "Machine"
            listOf("maintenance", "bakım").any { lower.contains(it) } -> "Maintenance"
            listOf("material", "malzeme").any { lower.contains(it) } -> "Material"
            listOf("category", "kategori").any { lower.contains(it) } -> "Category"
            listOf("user", "kullanıcı", "personel").any { lower.contains(it) } -> "User"
            else -> "Other"
        }
    }
}
