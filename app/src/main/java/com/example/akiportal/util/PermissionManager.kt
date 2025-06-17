package com.example.akiportal.permission

import android.content.Context
import android.util.Log
import com.example.akiportal.model.User
import com.example.akiportal.model.UserPermissions
import com.google.gson.Gson

/**
 * PermissionManager: Kullanıcı izin bilgisini SharedPreferences ile önbelleğe alır,
 * uygulama boyunca tekil örnek (singleton) olarak izni sorgulama imkânı sunar.
 */
class PermissionManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "akiportal_prefs"
        private const val KEY_USER = "key_current_user"
        private const val TAG = "PermissionManager"

        @Volatile
        private var instance: PermissionManager? = null

        /**
         * PermissionManager örneğini al
         */
        fun getInstance(context: Context): PermissionManager =
            instance ?: synchronized(this) {
                instance ?: PermissionManager(context.applicationContext).also { instance = it }
            }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Sunucudan dönen User nesnesini JSON'a çevirip kaydeder (içinde permissions da var)
     */
    fun saveUser(user: User) {
        val json = gson.toJson(user)
        prefs.edit()
            .putString(KEY_USER, json)
            .apply()
        Log.d(TAG, "Kullanıcı kaydedildi: $json")
    }

    /**
     * Önbelleğe alınmış User nesnesini döner; yoksa default User()
     */
    fun loadUser(): User {
        val json = prefs.getString(KEY_USER, null)
        return if (json.isNullOrEmpty()) {
            Log.w(TAG, "Kayıtlı kullanıcı bulunamadı. Varsayılan kullanıcı döndürüldü.")
            User()
        } else {
            try {
                val user = gson.fromJson(json, User::class.java)
                Log.d(TAG, "Kullanıcı yüklendi: $json")
                user
            } catch (e: Exception) {
                Log.e(TAG, "Kullanıcı verisi parse edilemedi. Hata: ${e.message}")
                User()
            }
        }
    }

    /**
     * Kaydedilmiş izinleri döner; null durumlarda güvenli şekilde default boş izin döner
     */
    private fun loadPermissions(): UserPermissions {
        val permissions = loadUser().permissions
        return permissions ?: UserPermissions().also {
            Log.w(TAG, "Permissions null geldi, boş UserPermissions oluşturuldu.")
        }
    }

    /**
     * Yetki kontrol metotları
     */
    fun canManageUsers(): Boolean = loadPermissions().manageUser
    fun canManageMachines(): Boolean = loadPermissions().manageMachine
    fun canManageCompanies(): Boolean = loadPermissions().manageCompany
    fun canManageMaintenance(): Boolean = loadPermissions().manageMaintenance
    fun canManageCategories(): Boolean = loadPermissions().manageCategory
    fun canManageMaterials(): Boolean = loadPermissions().manageMaterial
    fun canCallCustomer(): Boolean = loadPermissions().callCustomer
    fun canViewCompanies(): Boolean = loadPermissions().viewCompanies
    fun canViewMaintenancePlans(): Boolean = loadPermissions().viewMaintenancePlans
    fun canViewPreparationLists(): Boolean = loadPermissions().viewPreparationLists
    fun canViewMaterialsList(): Boolean = loadPermissions().viewMaterialsList
    fun canViewUsers(): Boolean = loadPermissions().viewUsers
    fun canViewNotifications(): Boolean = loadPermissions().viewNotifications

    /**
     * Kullanıcı çıkışı (logout) için önbelleği temizle
     */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER)
            .apply()
        Log.i(TAG, "Oturum temizlendi, kullanıcı çıkışı yapıldı.")
    }
}
