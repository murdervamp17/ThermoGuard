package com.thermoguard.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.ActivityManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
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
        val cpu = cpuTemp()
        val ambient = ambientTemp()
        return TempReading(
            timestamp = System.currentTimeMillis(),
            batteryC = battery.value,
            cpuC = cpu.value,
            ambientC = ambient.value,
            thermalStatus = thermalStatus(),
            headroom = headroom(),
            charging = battery.charging,
            batterySource = battery.availability,
            cpuSource = cpu.availability,
            ambientSource = ambient.availability,
        )
    }

    fun memoryInfo(): DeviceMemoryInfo? {
        val manager = ctx.getSystemService(ActivityManager::class.java) ?: return null
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return DeviceMemoryInfo(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            appPssBytes = Debug.getPss() * 1024L,
        )
    }

    private data class SourceValue<T>(val value: T?, val availability: SensorAvailability)
    private data class BatteryValue(
        val value: Float?,
        val charging: Boolean,
        val availability: SensorAvailability,
    )

    private fun batteryInfo(): BatteryValue {
        val intent: Intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryValue(null, false, SensorAvailability.UNAVAILABLE)
        val raw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = parseBatteryTemperature(raw)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryValue(
            temp,
            charging,
            if (temp == null) SensorAvailability.READ_FAILED else SensorAvailability.AVAILABLE,
        )
    }

    private fun cpuTemp(): SourceValue<Float> {
        val thermalRoot = File("/sys/class/thermal")
        if (!thermalRoot.exists()) return SourceValue(null, SensorAvailability.UNAVAILABLE)
        val zones = thermalRoot.listFiles { f -> f.name.startsWith("thermal_zone") }
            ?: return SourceValue(null, SensorAvailability.READ_FAILED)
        var hottest: Float? = null
        var cpuZoneFound = false
        var typeReadFailed = false
        for (zone in zones) {
            val type = runCatching { File(zone, "type").readText().trim().lowercase(Locale.ROOT) }.getOrNull()
            if (type == null) {
                typeReadFailed = true
                continue
            }
            if (!isCpuThermalZone(type)) continue
            cpuZoneFound = true
            val c = runCatching { parseThermalZoneTemperature(File(zone, "temp").readText()) }.getOrNull()
                ?: continue
            hottest = maxOf(hottest ?: c, c)
        }
        return SourceValue(
            hottest,
            when {
                hottest != null -> SensorAvailability.AVAILABLE
                cpuZoneFound -> SensorAvailability.READ_FAILED
                typeReadFailed -> SensorAvailability.READ_FAILED
                else -> SensorAvailability.UNAVAILABLE
            },
        )
    }

    private suspend fun ambientTemp(timeoutMs: Long = 1_500): SourceValue<Float> {
        val sm = ctx.getSystemService(SensorManager::class.java)
            ?: return SourceValue(null, SensorAvailability.UNAVAILABLE)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            ?: return SourceValue(null, SensorAvailability.UNAVAILABLE)
        val temperature = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sm.unregisterListener(this)
                        val value = event.values.firstOrNull()
                        if (cont.isActive) cont.resume(value?.takeIf { it.isFinite() } ?: Float.NaN)
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                if (!sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL) && cont.isActive) {
                    cont.resume(Float.NaN)
                }
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        }
        return if (temperature != null && isPlausibleAmbientTemperature(temperature)) {
            SourceValue(temperature, SensorAvailability.AVAILABLE)
        } else {
            SourceValue(null, SensorAvailability.READ_FAILED)
        }
    }

    private fun thermalStatus(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return ctx.getSystemService(PowerManager::class.java)?.currentThermalStatus
    }

    private fun headroom(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val h = ctx.getSystemService(PowerManager::class.java)?.getThermalHeadroom(0) ?: return null
        return h.takeIf { it.isFinite() && it >= 0f }
    }
}
