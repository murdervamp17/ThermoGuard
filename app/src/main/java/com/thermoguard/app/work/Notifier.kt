package com.thermoguard.app.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thermoguard.app.MainActivity
import com.thermoguard.app.R
import com.thermoguard.app.data.formatTemp

object Notifier {
    private const val CHANNEL_ID = "overheat_alerts"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Overheat alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Warns you when your device runs hotter than your threshold" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun overheat(context: Context, celsius: Float, fahrenheit: Boolean) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_thermo)
            .setContentTitle("Your phone is running hot")
            .setContentText("Battery at ${formatTemp(celsius, fahrenheit)} — above your alert threshold.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Battery at ${formatTemp(celsius, fahrenheit)}. Close heavy apps, unplug the charger, " +
                        "remove the case and move it out of direct sun."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
