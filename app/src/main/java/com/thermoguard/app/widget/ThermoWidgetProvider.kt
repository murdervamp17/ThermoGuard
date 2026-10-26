package com.thermoguard.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.thermoguard.app.MainActivity
import com.thermoguard.app.R
import com.thermoguard.app.data.ReadingStore
import com.thermoguard.app.data.Settings
import com.thermoguard.app.data.TempReading
import com.thermoguard.app.data.formatTemp
import com.thermoguard.app.data.thermalLabel
import com.thermoguard.app.work.WidgetRefreshWorker

class ThermoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val latest = ReadingStore.get(context).latest()
        publish(context, latest, force = true)
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
        )
    }

    companion object {
        private const val REFRESH_WORK_NAME = "thermoguard-widget-refresh"

        @Synchronized
        fun publish(context: Context, reading: TempReading?, force: Boolean = false) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ThermoWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val prefs = context.getSharedPreferences("thermoguard_widget", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (!force && now - prefs.getLong("last_update", 0L) < MIN_WIDGET_UPDATE_INTERVAL_MS) return

            val settings = Settings(context)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_thermo).apply {
                setTextViewText(R.id.widget_temperature, formatTemp(reading?.batteryC, settings.useFahrenheit))
                setTextViewText(
                    R.id.widget_status,
                    reading?.let {
                        val sensor = when (it.batterySource) {
                            com.thermoguard.app.data.SensorAvailability.AVAILABLE -> "Battery temperature"
                            com.thermoguard.app.data.SensorAvailability.UNAVAILABLE -> "Battery sensor not exposed"
                            com.thermoguard.app.data.SensorAvailability.READ_FAILED -> "Battery sensor read failed"
                        }
                        "${thermalLabel(it.thermalStatus)} · $sensor"
                    }
                        ?: "Waiting for a temperature reading",
                )
                setTextViewText(
                    R.id.widget_updated,
                    reading?.let { "Updated ${android.text.format.DateFormat.format("h:mm a", it.timestamp)}" }
                        ?: "Open ThermoGuard to take a reading",
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
            prefs.edit().putLong("last_update", now).apply()
        }

        private const val MIN_WIDGET_UPDATE_INTERVAL_MS = 15 * 60 * 1000L
    }
}
