package com.example.akiportal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.akiportal.model.UserPermissions
import com.example.akiportal.util.EncryptedSharedPrefHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.auth.FirebaseUser
import com.example.akiportal.model.User
import com.google.gson.Gson

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                user?.let { firebaseUser ->
                    val uid = firebaseUser.uid
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { snapshot ->
                            val fullName = snapshot.getString("fullName") ?: ""
                            val workPhone = snapshot.getString("workPhone") ?: ""
                            val personalPhone = snapshot.getString("personalPhone") ?: ""
                            val isActive = snapshot.getBoolean("isActive") ?: true

                            val permissionsMap = snapshot.get("permissions") as? Map<String, Boolean>
                            val permissions = permissionsMap?.let {
                                Gson().fromJson(
                                    Gson().toJson(it),
                                    UserPermissions::class.java
                                )
                            } ?: UserPermissions()

                            val userModel = User(
                                uid = uid,
                                email = firebaseUser.email ?: "",
                                fullName = fullName,
                                isActive = isActive,
                                workPhone = workPhone,
                                personalPhone = personalPhone,
                                permissions = permissions
                            )

                            _currentUser.value = userModel
                            EncryptedSharedPrefHelper.saveUserPermissions(context, userModel)
                        }
                }
                _authError.value = null
            }
            .addOnFailureListener {
                _authError.value = it.message
            }
    }

    fun updateFullName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("fullName", newName)
            .addOnSuccessListener {
                val oldUser = _currentUser.value
                if (oldUser != null) {
                    _currentUser.value = oldUser.copy(fullName = newName)
                }
            }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        EncryptedSharedPrefHelper.clearRememberMe(context)
    }
}
