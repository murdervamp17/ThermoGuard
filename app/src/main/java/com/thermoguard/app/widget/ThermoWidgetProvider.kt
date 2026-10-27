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
import com.thermoguard.app.data.SensorAvailability
import com.thermoguard.app.data.Settings
import com.thermoguard.app.data.TempReading
import com.thermoguard.app.data.formatTemp
import com.thermoguard.app.data.thermalLabel
import com.thermoguard.app.data.temperatureColorArgb
import com.thermoguard.app.data.thermalColorArgb
import com.thermoguard.app.work.WidgetRefreshWorker

class ThermoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val latest = readCachedReading(context) ?: ReadingStore.get(context).latest()
        publish(context, latest, force = true)
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CYCLE_METRIC) {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SHOW_CPU, !prefs.getBoolean(KEY_SHOW_CPU, false)).apply()
            publish(context, readCachedReading(context) ?: ReadingStore.get(context).latest(), force = true)
        }
    }

    companion object {
        private const val PREFERENCES = "thermoguard_widget"
        private const val KEY_SHOW_CPU = "show_cpu"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_TIMESTAMP = "reading_timestamp"
        private const val KEY_BATTERY_C = "reading_battery_c"
        private const val KEY_CPU_C = "reading_cpu_c"
        private const val KEY_AMBIENT_C = "reading_ambient_c"
        private const val KEY_STATUS = "reading_status"
        private const val KEY_BATTERY_SOURCE = "reading_battery_source"
        private const val KEY_CPU_SOURCE = "reading_cpu_source"
        private const val KEY_AMBIENT_SOURCE = "reading_ambient_source"
        private const val ACTION_CYCLE_METRIC = "com.thermoguard.app.widget.CYCLE_METRIC"
        private const val REFRESH_WORK_NAME = "thermoguard-widget-refresh"

        @Synchronized
        fun publish(context: Context, reading: TempReading?, force: Boolean = false) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ThermoWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (!force && now - prefs.getLong(KEY_LAST_UPDATE, 0L) < MIN_WIDGET_UPDATE_INTERVAL_MS) return

            val settings = Settings(context)
            val showCpu = prefs.getBoolean(KEY_SHOW_CPU, false)
            val temperature = if (showCpu) reading?.cpuC else reading?.batteryC
            val source = if (showCpu) reading?.cpuSource else reading?.batterySource
            val sourceLabel = if (showCpu) "CPU" else "BATTERY"
            val rootPending = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val cycleIntent = Intent(context, ThermoWidgetProvider::class.java).setAction(ACTION_CYCLE_METRIC)
            val cyclePending = PendingIntent.getBroadcast(
                context,
                1,
                cycleIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_thermo).apply {
                setTextViewText(R.id.widget_temperature, formatTemp(temperature, settings.useFahrenheit))
                setTextColor(R.id.widget_temperature, temperatureColorArgb(temperature))
                setTextViewText(R.id.widget_source_switch, "$sourceLabel  ·  TAP TO SWITCH")
                setOnClickPendingIntent(R.id.widget_source_switch, cyclePending)
                setTextViewText(
                    R.id.widget_thermal_status,
                    reading?.let { thermalLabel(it.thermalStatus) } ?: "Waiting for reading",
                )
                setTextColor(R.id.widget_thermal_status, thermalColorArgb(reading?.thermalStatus))
                setTextViewText(R.id.widget_sensor_status, sourceDescription(showCpu, source))
                setTextViewText(
                    R.id.widget_updated,
                    reading?.let { "Updated ${android.text.format.DateFormat.format("h:mm a", it.timestamp)}" }
                        ?: "Open ThermoGuard to take a reading",
                )
                setOnClickPendingIntent(R.id.widget_root, rootPending)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
            prefs.edit().apply {
                putLong(KEY_LAST_UPDATE, now)
                if (reading != null) {
                    putLong(KEY_TIMESTAMP, reading.timestamp)
                    putFloat(KEY_BATTERY_C, reading.batteryC ?: Float.NaN)
                    putFloat(KEY_CPU_C, reading.cpuC ?: Float.NaN)
                    putFloat(KEY_AMBIENT_C, reading.ambientC ?: Float.NaN)
                    putInt(KEY_STATUS, reading.thermalStatus ?: -1)
                    putString(KEY_BATTERY_SOURCE, reading.batterySource.name)
                    putString(KEY_CPU_SOURCE, reading.cpuSource.name)
                    putString(KEY_AMBIENT_SOURCE, reading.ambientSource.name)
                }
            }.apply()
        }

        private fun readCachedReading(context: Context): TempReading? {
            val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
            if (timestamp <= 0L) return null
            fun temperature(key: String): Float? = prefs.getFloat(key, Float.NaN).takeIf(Float::isFinite)
            return TempReading(
                timestamp = timestamp,
                batteryC = temperature(KEY_BATTERY_C),
                cpuC = temperature(KEY_CPU_C),
                ambientC = temperature(KEY_AMBIENT_C),
                thermalStatus = prefs.getInt(KEY_STATUS, -1).takeIf { it >= 0 },
                batterySource = SensorAvailability.fromStorage(prefs.getString(KEY_BATTERY_SOURCE, null)),
                cpuSource = SensorAvailability.fromStorage(prefs.getString(KEY_CPU_SOURCE, null)),
                ambientSource = SensorAvailability.fromStorage(prefs.getString(KEY_AMBIENT_SOURCE, null)),
            )
        }

        private fun sourceDescription(showCpu: Boolean, source: SensorAvailability?): String = when {
            source == null -> "Add a temperature reading"
            source == SensorAvailability.UNAVAILABLE && showCpu -> "CPU/SoC sensor not exposed"
            source == SensorAvailability.UNAVAILABLE -> "Battery sensor not exposed"
            source == SensorAvailability.READ_FAILED -> "Sensor read failed · will retry"
            showCpu -> "CPU/SoC reading · device dependent"
            else -> "Battery sensor reading · not surface temperature"
        }

        private const val MIN_WIDGET_UPDATE_INTERVAL_MS = 60 * 1000L
    }
}
