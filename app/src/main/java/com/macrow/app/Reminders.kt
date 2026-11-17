package com.macrow.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.macrow.app.data.MacrowRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val WORK = "meal-reminder"
private const val CHANNEL = "reminders"

object Reminders {
    /** Schedules a daily reminder near [minutes] after midnight. Android may move it a little to save battery. */
    fun schedule(context: Context, minutes: Int) {
        val now = LocalDateTime.now()
        var at = now.toLocalDate().atStartOfDay().plusMinutes(minutes.toLong())
        if (!at.isAfter(now)) at = at.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, at))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
}

class ReminderWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = context.applicationContext as MacrowApp
        // Stay quiet if you already logged dinner today.
        if (app.repo.dinnerLogged(MacrowRepository.today())) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Meal reminder", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Log your meals")
            .setContentText("Take a minute to add what you ate today.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(1, n)
        return Result.success()
    }
}
