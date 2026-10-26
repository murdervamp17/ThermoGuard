package com.thermoguard.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/**
 * Reads temperatures from every source available without root:
 *  - Battery temperature (all devices)
 *  - CPU/SoC thermal zones (many devices; restricted on some newer ROMs)
 *  - Ambient temperature sensor (rare, but used if present)
 *  - System thermal status / headroom (Android 10+/11+)
 */
class TemperatureReader(context: Context) {
    private val ctx = context.applicationContext

    suspend fun read(): TempReading {
        val battery = batteryInfo()
        return TempReading(
            timestamp = System.currentTimeMillis(),
            batteryC = battery.first,
            cpuC = cpuTemp(),
            ambientC = ambientTemp(),
            thermalStatus = thermalStatus(),
            headroom = headroom(),
            charging = battery.second,
        )
    }

    private fun batteryInfo(): Pair<Float?, Boolean> {
        val intent: Intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null to false
        val raw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (raw == Int.MIN_VALUE) null else raw / 10f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return temp to charging
    }

    private fun cpuTemp(): Float? {
        val zones = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
            ?: return null
        var hottest: Float? = null
        for (zone in zones) {
            val type = runCatching { File(zone, "type").readText().trim().lowercase() }.getOrNull() ?: continue
            val isCpu = listOf("cpu", "soc", "tsens", "mtktscpu", "exynos").any { type.contains(it) }
            if (!isCpu) continue
            val raw = runCatching { File(zone, "temp").readText().trim().toFloat() }.getOrNull() ?: continue
            val c = if (raw > 1000f) raw / 1000f else raw
            if (c in 1f..150f) hottest = maxOf(hottest ?: c, c)
        }
        return hottest
    }

    private suspend fun ambientTemp(timeoutMs: Long = 1_500): Float? {
        val sm = ctx.getSystemService(SensorManager::class.java) ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0])
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        }
    }

    private fun thermalStatus(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return ctx.getSystemService(PowerManager::class.java)?.currentThermalStatus
    }

    private fun headroom(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val h = ctx.getSystemService(PowerManager::class.java)?.getThermalHeadroom(0) ?: return null
        return if (h.isNaN()) null else h
    }
}
