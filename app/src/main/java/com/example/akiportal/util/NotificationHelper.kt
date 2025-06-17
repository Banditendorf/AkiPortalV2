package com.example.akiportal.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.akiportal.R
import java.util.concurrent.TimeUnit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


object NotificationHelper {
    private lateinit var appContext: Context
    private const val CHANNEL_ID = "maintenance_channel"

    fun init(context: Context) {
        appContext = context.applicationContext
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${appContext.packageName}/raw/notification_sound")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bakım Hatırlatmaları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kritik bakım hatırlatmaları ve stok uyarıları"
                setSound(soundUri, audioAttributes)
            }

            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(id: Int, title: String, message: String) {
        if (!::appContext.isInitialized) return

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(appContext).notify(id, builder.build())
    }

    fun scheduleDailyNotificationWorker() {
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.DAYS
        ).setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(
                "DailyNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}

class NotificationWorker(
private val context: Context,
workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val now = SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault()).format(Date())

    override suspend fun doWork(): Result {
        return try {
            handleMachineNotifications()
            handleStockNotifications()
            handleMaintenanceNotifications()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun handleMachineNotifications() {
        val machines = db.collection("machines").get().await()
        machines.documents.forEach { doc ->
            val id = doc.id
            val name = doc.getString("name") ?: return@forEach
            val company = doc.getString("companyName") ?: "Firma"
            val estimated = doc.getLong("estimatedHours")?.toInt() ?: return@forEach
            val next = doc.getLong("nextMaintenanceHour")?.toInt() ?: return@forEach

            when {
                estimated > next -> {
                    val notiId = "overdue_$id"
                    val msg = "$company - $name: planlı bakım saati AŞILDI! ($estimated > $next)"
                    NotificationHelper.showNotification(notiId.hashCode(), "⚠️ Bakım Gecikti", msg)
                    addToNotificationHistory(notiId, msg, "WARNING", now)
                }
                next - estimated <= 48 -> {
                    val notiId = "upcoming_$id"
                    val msg = "$company - $name: planlı bakım 48 saat içinde"
                    NotificationHelper.showNotification(notiId.hashCode(), "🔔 Bakım Yaklaşıyor", msg)
                    addToNotificationHistory(notiId, msg, "INFO", now)
                }
            }
        }
    }

    private suspend fun handleStockNotifications() {
        val materials = db.collection("materials").get().await()
        materials.documents.forEach { doc ->
            val content = doc.get("icerik") as? Map<*, *> ?: return@forEach
            content.values.forEach { value ->
                val item = value as? Map<*, *> ?: return@forEach
                val code = item["code"] as? String ?: return@forEach
                val stock = (item["stock"] as? Long)?.toInt() ?: return@forEach
                val critical = (item["kritikStok"] as? Long)?.toInt() ?: return@forEach

                if (stock <= critical) {
                    val notiId = "lowstock_$code"
                    val msg = "$code malzemesi kritik seviyede: $stock adet"
                    NotificationHelper.showNotification(notiId.hashCode(), "📦 Kritik Stok Uyarısı", msg)
                    addToNotificationHistory(notiId, msg, "WARNING", now)
                }
            }
        }
    }

    private suspend fun handleMaintenanceNotifications() {
        val maintenances = db.collection("plannedMaintenances").get().await()
        maintenances.documents.forEach { doc ->
            val status = doc.getString("status")
            if (status == "tamamlandı") {
                val machineName = doc.getString("machineName") ?: "Makine"
                val company = doc.getString("companyName") ?: "Firma"
                val serial = doc.getString("serialNumber") ?: "Seri"
                val desc = doc.getString("description") ?: ""
                val notiId = "done_${doc.id}"
                val msg = "$company şirketin $serial seri numaralı $machineName makinasının \"$desc\" işlemi tamamlandı."

                NotificationHelper.showNotification(notiId.hashCode(), "✅ Bakım Tamamlandı", msg)
                addToNotificationHistory(notiId, msg, "SUCCESS", now)
            }
        }
    }

    private suspend fun addToNotificationHistory(
        id: String,
        message: String,
        type: String,
        timestamp: String
    ) {
        val data = mapOf(
            "id" to id,
            "message" to message,
            "type" to type,
            "timestamp" to timestamp,
            "isPersistent" to false
        )

        db.collection("notifications").document(id).set(data)
    }
}
