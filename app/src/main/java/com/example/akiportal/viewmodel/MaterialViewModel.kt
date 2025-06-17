package com.example.akiportal.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.akiportal.model.Material
import com.example.akiportal.model.NotificationItem
import com.example.akiportal.model.NotificationType
import com.example.akiportal.util.NotificationHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class MaterialViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private lateinit var companyId: String
    fun setCompanyId(id: String) {
        companyId = id
    }

    private val _materials = MutableStateFlow(emptyList<Material>())
    val materials: StateFlow<List<Material>> = _materials

    private val _categories = MutableStateFlow(emptyList<String>())
    val categories: StateFlow<List<String>> = _categories

    private val _notificationList = MutableStateFlow(emptyList<NotificationItem>())
    val notificationList: StateFlow<List<NotificationItem>> = _notificationList

    private val _lowStockMaterials = MutableStateFlow(emptyList<Material>())
    val lowStockMaterials: StateFlow<List<Material>> = _lowStockMaterials

    private val shownNotifications = mutableSetOf<String>()

    fun loadMaterials(context: Context? = null) {
        db.collection("materials").get().addOnSuccessListener { result ->
            val allMaterials = mutableListOf<Material>()
            val allCategories = mutableListOf<String>()

            for (doc in result.documents) {
                val categoryName = doc.id
                allCategories.add(categoryName)

                val content = doc.get("icerik") as? Map<*, *> ?: continue
                for ((_, rawAny) in content) {
                    val raw = rawAny as? Map<*, *> ?: continue
                    val code = raw["code"] as? String ?: continue
                    val shelf = raw["shelf"] as? String ?: ""
                    val stock = (raw["stock"] as? Long)?.toInt() ?: 0
                    val kritik = (raw["kritikStok"] as? Long)?.toInt() ?: 0
                    val desc = raw["description"] as? String ?: ""

                    val mat = Material(code, shelf, categoryName, stock, kritik, desc)
                    allMaterials.add(mat)
                }
            }

            _materials.value = allMaterials
            _categories.value = allCategories

            val lowStockList = allMaterials.filter { it.stock <= it.kritikStok }
            _lowStockMaterials.value = lowStockList

            val now = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault()).format(Date())
            val newNotifications = lowStockList
                .filter { !shownNotifications.contains(it.code) }
                .map {
                    NotificationItem(
                        id = "lowstock_${it.code}",
                        message = "${it.code} stoğu kritik seviyede (${it.stock} adet)",
                        type = NotificationType.WARNING,
                        timestamp = now
                    )
                }

            _notificationList.value += newNotifications

            newNotifications.forEach { notif ->
                shownNotifications.add(notif.id)
                context?.let {
                    NotificationHelper.showNotification(
                        id = notif.id.hashCode(),
                        title = "Stok Uyarısı",
                        message = notif.message
                    )
                }
            }
        }
    }

    val allMaterials: StateFlow<List<Material>> get() = materials

    fun updateStock(category: String, code: String, newStock: Int) {
        val docRef = db.collection("materials").document(category)
        docRef.get().addOnSuccessListener { snapshot ->
            val content = snapshot.get("icerik") as? Map<String, Map<String, Any?>> ?: return@addOnSuccessListener
            val updated = content.mapValues { (key, value) ->
                if (value["code"] == code) {
                    value.toMutableMap().apply {
                        this["stock"] = newStock
                    }
                } else value
            }
            docRef.set(mapOf("icerik" to updated), SetOptions.merge())
                .addOnSuccessListener { loadMaterials() }
        }
    }

    fun addMaterial(material: Material) {
        val docRef = db.collection("materials").document(material.category)
        docRef.get().addOnSuccessListener { snapshot ->
            val content = snapshot.get("icerik") as? Map<String, Any?> ?: mapOf()
            val updatedMap = content.toMutableMap()

            val newId = UUID.randomUUID().toString()
            updatedMap[newId] = mapOf(
                "code" to material.code,
                "shelf" to material.shelf,
                "category" to material.category,
                "stock" to material.stock,
                "kritikStok" to material.kritikStok,
                "description" to material.description
            )

            docRef.set(mapOf("icerik" to updatedMap), SetOptions.merge())
                .addOnSuccessListener { loadMaterials() }
        }
    }

    fun updateMaterial(material: Material) {
        val docRef = db.collection("materials").document(material.category)
        docRef.get().addOnSuccessListener { snapshot ->
            val content = snapshot.get("icerik") as? Map<String, Map<String, Any?>> ?: return@addOnSuccessListener
            val updated = content.mapValues { (key, value) ->
                if (value["code"] == material.code) {
                    mapOf(
                        "code" to material.code,
                        "shelf" to material.shelf,
                        "category" to material.category,
                        "stock" to material.stock,
                        "kritikStok" to material.kritikStok,
                        "description" to material.description
                    )
                } else value
            }

            docRef.set(mapOf("icerik" to updated), SetOptions.merge())
                .addOnSuccessListener { loadMaterials() }
        }
    }

    fun deleteMaterial(material: Material) {
        val docRef = db.collection("materials").document(material.category)
        docRef.get().addOnSuccessListener { snapshot ->
            val content = snapshot.get("icerik") as? Map<String, Map<String, Any?>> ?: return@addOnSuccessListener
            val updated = content.filterValues {
                !(it["code"]?.toString()?.equals(material.code, ignoreCase = true) == true &&
                        it["shelf"]?.toString()?.equals(material.shelf, ignoreCase = true) == true)
            }

            docRef.set(mapOf("icerik" to updated), SetOptions.merge())
                .addOnSuccessListener { loadMaterials() }
        }
    }

    fun decreaseStock(code: String, quantity: Int) {
        db.collection("materials").get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach { doc ->
                val content = doc.get("icerik") as? Map<String, Map<String, Any?>> ?: return@forEach
                content.forEach { (key, value) ->
                    if (value["code"] == code) {
                        db.collection("materials")
                            .document(doc.id)
                            .update("icerik.$key.stock", FieldValue.increment(-quantity.toLong()))
                        return@forEach
                    }
                }
            }
        }
    }

    fun addCategory(name: String) {
        db.collection("materials").document(name)
            .set(mapOf("icerik" to mapOf<String, Any>()))
            .addOnSuccessListener { loadMaterials() }
    }

    fun deleteCategory(name: String) {
        db.collection("materials").document(name)
            .delete()
            .addOnSuccessListener { loadMaterials() }
    }

    fun sortCategoriesByName(ascending: Boolean = true) {
        _categories.value = if (ascending)
            _categories.value.sorted()
        else
            _categories.value.sortedDescending()
    }
}
