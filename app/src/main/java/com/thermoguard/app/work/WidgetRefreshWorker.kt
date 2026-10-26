package com.thermoguard.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thermoguard.app.data.TemperatureReader
import com.thermoguard.app.widget.ThermoWidgetProvider

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reading = TemperatureReader(applicationContext).read()
        ThermoWidgetProvider.publish(applicationContext, reading, force = true)
        return Result.success()
    }
}
