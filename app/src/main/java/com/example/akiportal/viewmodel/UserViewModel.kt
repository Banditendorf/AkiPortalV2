package com.example.akiportal.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.akiportal.model.User
import com.example.akiportal.permission.PermissionManager
import com.example.akiportal.util.LogHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val pm = PermissionManager.getInstance(application.applicationContext)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers

    init {
        fetchCurrentUser()
        listenAllUsers()
    }

    /**
     * Giriş yapan kullanıcının bilgilerini dinler ve günceller
     */
    fun fetchCurrentUser() {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .addSnapshotListener { doc, error ->
                    if (error == null && doc != null && doc.exists()) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            _currentUser.value = user
                            pm.saveUser(user)
                            Log.d("UserViewModel", "Kullanıcı yüklendi: ${user.email}")
                        } else {
                            Log.w("UserViewModel", "Firebase'den gelen kullanıcı verisi null.")
                        }
                    } else {
                        Log.e("UserViewModel", "Kullanıcı verisi alınamadı: ${error?.message}")
                    }
                }
        } ?: Log.w("UserViewModel", "Giriş yapan kullanıcı UID alınamadı.")
    }

    /**
     * Tüm kullanıcıları dinler
     */
    private fun listenAllUsers() {
        db.collection("users")
            .addSnapshotListener { snaps, error ->
                if (error == null && snaps != null) {
                    _allUsers.value = snaps.documents.mapNotNull { it.toObject(User::class.java) }
                } else {
                    Log.e("UserViewModel", "Tüm kullanıcılar alınamadı: ${error?.message}")
                }
            }
    }

    /**
     * Yeni kullanıcı oluşturur veya mevcut kullanıcıyı günceller
     */
    fun saveUser(
        user: User,
        password: String? = null,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        if (!pm.canManageUsers()) {
            Toast.makeText(getApplication(), "Yetkiniz yok", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != null && user.uid.isEmpty()) {
            auth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener { authRes ->
                    val newUid = authRes.user?.uid ?: return@addOnSuccessListener
                    val newUser = user.copy(uid = newUid)
                    db.collection("users").document(newUid)
                        .set(newUser)
                        .addOnSuccessListener {
                            logUserAction("Kullanıcı Eklendi", newUid)
                            onSuccess()
                            fetchCurrentUser() // kendi kaydını güncelle
                        }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Veritabanı hatası") }
                }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Authentication hatası") }
        } else {
            viewModelScope.launch {
                db.collection("users").document(user.uid)
                    .set(user, SetOptions.merge())
                    .addOnSuccessListener {
                        logUserAction("Kullanıcı Güncellendi", user.uid)
                        onSuccess()
                        fetchCurrentUser()
                    }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Güncelleme hatası") }
            }
        }
    }

    fun deleteUser(uid: String, onComplete: () -> Unit = {}) {
        if (!pm.canManageUsers()) {
            Toast.makeText(getApplication(), "Yetkiniz yok", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(uid)
            .delete()
            .addOnSuccessListener {
                logUserAction("Kullanıcı Silindi", uid)
                onComplete()
            }
    }

    fun toggleUserActive(uid: String, newState: Boolean) {
        if (!pm.canManageUsers()) {
            Toast.makeText(getApplication(), "Yetkiniz yok", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("users").document(uid)
            .update("isActive", newState)
            .addOnSuccessListener {
                logUserAction("Aktiflik Durumu Güncellendi", uid, "newState" to newState)
            }
    }

    /**
     * Kendi e-posta adresini döner
     */
    private fun currentUserEmail(): String = _currentUser.value?.email.orEmpty()

    /**
     * Firebase loglama işlemi
     */
    private fun logUserAction(action: String, uid: String, vararg details: Pair<String, Any?>) {
        val logMap = buildMap<String, Any> {
            put("uid", uid)
            details.forEach { (k, v) ->
                if (v != null) put(k, v) // null değerleri dahil etme
            }
        }

        LogHelper.logFirebaseAction(
            userEmail = currentUserEmail(),
            action = action,
            details = logMap
        )
    }


    /**
     * UI katmanına direkt yetki verebilecek yardımcı fonksiyonlar
     */
    fun canManageUsers(): Boolean = _currentUser.value?.permissions?.manageUser ?: false
    fun canViewCompanies(): Boolean = _currentUser.value?.permissions?.viewCompanies ?: false
    // Diğer izinler de buraya eklenebilir...
}
