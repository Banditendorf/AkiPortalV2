package com.example.akiportal.viewmodel

import androidx.lifecycle.ViewModel
import com.example.akiportal.model.Company
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompanyViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies

    fun loadCompanies() {
        db.collection("companies").get().addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Company::class.java)
            }
            _companies.value = list
        }
    }
    fun updateCompany(company: Company) {
        db.collection("companies").document(company.id).set(company)
            .addOnSuccessListener { loadCompanies() }
    }
}
