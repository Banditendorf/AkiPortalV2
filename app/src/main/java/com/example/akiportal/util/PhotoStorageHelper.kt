package com.example.akiportal.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PhotoStorageHelper {

    // Mevcut isimler korunacak, yeni fonksiyon isimleri İngilizce

    fun getMaintenancePhotoDir(
        context: Context,
        machineId: String,
        maintenanceId: String
    ): File {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File(context.getExternalFilesDir(null), "akiportal/$machineId/$maintenanceId")
        } else {
            File(Environment.getExternalStorageDirectory(), "akiportal/$machineId/$maintenanceId")
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun getPhotoDirFromRelative(context: Context, relativePath: String): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File(context.getExternalFilesDir(null), relativePath)
        } else {
            File(Environment.getExternalStorageDirectory(), relativePath)
        }
    }

    fun saveUrisToMaintenanceDir(
        context: Context,
        uris: List<Uri>,
        machineId: String,
        maintenanceId: String
    ): List<File> {
        val savedFiles = mutableListOf<File>()
        uris.forEachIndexed { index, uri ->
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}_$index.jpg"
            val targetDir = getMaintenancePhotoDir(context, machineId, maintenanceId)
            val destFile = File(targetDir, fileName)

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                savedFiles.add(destFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return savedFiles
    }

    fun movePhotoFromUri(
        context: Context,
        uri: Uri,
        machineId: String,
        maintenanceId: String
    ): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val targetDir = getMaintenancePhotoDir(context, machineId, maintenanceId)
        val destFile = File(targetDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listPhotosByFolderName(photoFolderName: String): List<File> {
        val dir = File(photoFolderName)
        return dir.listFiles()?.sortedBy { it.name } ?: emptyList()
    }

    fun findMissingPhotos(
        context: Context,
        photoFolderRelative: String,
        expectedNames: List<String>
    ): List<String> {
        val dir = getPhotoDirFromRelative(context, photoFolderRelative)
        val localNames = dir.listFiles()?.map { it.name } ?: emptyList()
        return expectedNames.filter { it !in localNames }
    }

    suspend fun downloadPhotoFromPeer(
        peerIp: String,
        port: Int,
        fileName: String,
        targetDir: File
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "http://$peerIp:$port/photo/$fileName"
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    val destFile = File(targetDir, fileName)
                    destFile.writeBytes(bytes)
                    true
                } else false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun completeMissingPhotosFromPeers(
        context: Context,
        photoFolderRelative: String,
        expectedNames: List<String>,
        peers: List<Pair<String, Int>>
    ) {
        val dir = getPhotoDirFromRelative(context, photoFolderRelative)
        val missing = findMissingPhotos(context, photoFolderRelative, expectedNames)
        for (fileName in missing) {
            var downloaded = false
            for ((peerIp, port) in peers) {
                downloaded = downloadPhotoFromPeer(peerIp, port, fileName, dir)
                if (downloaded) break
            }
        }
    }

    class PeerPhotoHttpServer(val basePhotoFolder: File) : NanoHTTPD(8080) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            if (uri.startsWith("/photo/")) {
                val fileName = uri.removePrefix("/photo/")
                val file = File(basePhotoFolder, fileName)
                if (file.exists()) {
                    return newChunkedResponse(Response.Status.OK, "image/jpeg", file.inputStream())
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }


    /**
     * Eksik fotoğrafları Firebase Storage'a upload eden fonksiyon.
     */
    suspend fun uploadMissingPhotosToFirebase(
        context: Context,
        machineId: String,
        maintenanceId: String,
        missingPhotoNames: List<String>
    ) = withContext(Dispatchers.IO) {
        val photoDir = getMaintenancePhotoDir(context, machineId, maintenanceId)
        for (fileName in missingPhotoNames) {
            val file = File(photoDir, fileName)
            if (file.exists()) {
                try {
                    val storageRef = Firebase.storage.reference
                    val fileRef = storageRef.child("photos/$machineId/$maintenanceId/$fileName")
                    fileRef.putFile(Uri.fromFile(file)).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Firebase Storage'dan belirli fotoğrafı indirir.
     */
    suspend fun downloadPhotoFromFirebase(
        context: Context,
        machineId: String,
        maintenanceId: String,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val storageRef = Firebase.storage.reference
            val fileRef = storageRef.child("photos/$machineId/$maintenanceId/$fileName")
            val localFile = File(getMaintenancePhotoDir(context, machineId, maintenanceId), fileName)
            fileRef.getFile(localFile).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fotoğrafı Firebase Storage'dan siler.
     */
    suspend fun deletePhotoFromFirebase(
        machineId: String,
        maintenanceId: String,
        fileName: String
    ) = withContext(Dispatchers.IO) {
        try {
            val storageRef = Firebase.storage.reference
            val fileRef = storageRef.child("photos/$machineId/$maintenanceId/$fileName")
            fileRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Eksik fotoğrafları bulur, önce P2P, sonra Firebase'den indirir.
     * Eksik olanları geri Firebase'e upload eder. Sonra tekrar eksikleri indirir, en son storage'dan siler.
     */
    suspend fun hybridPhotoSync(
        context: Context,
        machineId: String,
        maintenanceId: String,
        expectedNames: List<String>,
        peers: List<Pair<String, Int>>
    ) {
        val folderRelative = "akiportal/$machineId/$maintenanceId"
        // 1. Eksikleri bul
        var missingPhotos = findMissingPhotos(context, folderRelative, expectedNames)
        // 2. P2P ile eksikleri indir
        completeMissingPhotosFromPeers(context, folderRelative, expectedNames, peers)
        // 3. Kalan eksikleri Firebase'den indir
        missingPhotos = findMissingPhotos(context, folderRelative, expectedNames)
        for (fileName in missingPhotos) {
            downloadPhotoFromFirebase(context, machineId, maintenanceId, fileName)
        }
        // 4. Hala eksik varsa, diğer cihazlar ellerindekini tekrar Firebase'e yükler
        // (Burada tüm cihazlar ellerindeki fazla dosyaları uploadMissingPhotosToFirebase ile yükleyebilir)
        // 5. 2. turda tekrar indir
        missingPhotos = findMissingPhotos(context, folderRelative, expectedNames)
        for (fileName in missingPhotos) {
            downloadPhotoFromFirebase(context, machineId, maintenanceId, fileName)
        }
        // 6. Tüm dosyalar tamamsa, Firebase'den sil
        missingPhotos = findMissingPhotos(context, folderRelative, expectedNames)
        if (missingPhotos.isEmpty()) {
            for (fileName in expectedNames) {
                deletePhotoFromFirebase(machineId, maintenanceId, fileName)
            }
        }
    }
}
