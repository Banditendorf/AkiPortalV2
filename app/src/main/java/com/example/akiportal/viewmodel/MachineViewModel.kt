package com.example.akiportal.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.akiportal.model.Machine
import com.example.akiportal.util.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MachineViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _machines = MutableStateFlow<List<Machine>>(emptyList())
    val machines: StateFlow<List<Machine>> = _machines

    // Şirkete ait makineleri yükle
    fun loadMachines(companyId: String) {
        db.collection("machines")
            .whereEqualTo("companyId", companyId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    doc.toObject(Machine::class.java)?.copy(id = doc.id)
                }
                _machines.value = list
            }
            .addOnFailureListener {
                _machines.value = emptyList()
            }
    }

    // Yeni makine ekle
    fun addMachine(machine: Machine, onComplete: () -> Unit) {
        db.collection("machines")
            .add(machine)
            .addOnCompleteListener { onComplete() }
    }

    // Mevcut makineyi güncelle
    fun updateMachine(machine: Machine, onComplete: () -> Unit) {
        if (machine.id.isBlank()) return
        db.collection("machines").document(machine.id)
            .set(machine)
            .addOnCompleteListener { onComplete() }
    }

    // Makine sil
    fun deleteMachine(machineId: String, onComplete: () -> Unit) {
        db.collection("machines").document(machineId)
            .delete()
            .addOnCompleteListener { onComplete() }
    }

    // Bakım zamanı yaklaşan makineleri getir (varsayılan: 48 saat içinde)
    fun checkUpcomingMaintenances(threshold: Int = 48, onResult: (List<Machine>) -> Unit) {
        db.collection("machines").get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val machine = doc.toObject(Machine::class.java)
                    if (machine != null && (machine.nextMaintenanceHour - machine.estimatedHours) <= threshold) {
                        machine.copy(id = doc.id)
                    } else null
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    fun checkAndNotifyUpcomingMaintenances(
        threshold: Int = 48,
        notify: (Machine) -> Unit
    ) {
        db.collection("machines").get()
            .addOnSuccessListener { result ->
                result.documents.forEach { doc ->
                    val machine = doc.toObject(Machine::class.java)
                    if (machine != null && machine.nextMaintenanceHour - machine.estimatedHours <= threshold) {
                        notify(machine.copy(id = doc.id))
                    }
                }
            }
    }

}
