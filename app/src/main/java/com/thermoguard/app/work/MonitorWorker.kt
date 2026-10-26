package com.thermoguard.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.thermoguard.app.data.ReadingStore
import com.thermoguard.app.data.Settings
import com.thermoguard.app.data.TemperatureReader
import com.thermoguard.app.data.isWithinQuietHours
import com.thermoguard.app.widget.ThermoWidgetProvider
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Runs every 15 minutes (Android's minimum) to log a reading and raise alerts. */
class MonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val reading = TemperatureReader(ctx).read()
        val store = ReadingStore.get(ctx)
        val previous = store.latest()
        store.insert(reading)
        store.pruneOlderThan(System.currentTimeMillis() - Settings.HISTORY_RETENTION_MS)
        ThermoWidgetProvider.publish(ctx, reading, force = true)

        val settings = Settings(ctx)
        val battery = reading.batteryC
        val nowTime = LocalTime.now()
        val quiet = settings.quietHoursEnabled && isWithinQuietHours(
            nowTime.hour * 60 + nowTime.minute,
            settings.quietStartMinute,
            settings.quietEndMinute,
        )
        if (settings.alertsEnabled && !quiet && battery != null && battery >= settings.alertThresholdC) {
            val now = System.currentTimeMillis()
            if (now - settings.lastAlertAt > Settings.ALERT_COOLDOWN_MS) {
                if (Notifier.overheat(ctx, battery, settings.useFahrenheit, previous?.batteryC)) {
                    settings.lastAlertAt = now
                }
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "thermoguard-monitor"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonitorWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
