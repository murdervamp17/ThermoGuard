package com.thermoguard.app.data

import java.util.Locale

/** One point-in-time snapshot of every temperature source the device exposes. */
data class TempReading(
    val timestamp: Long,
    val batteryC: Float?,
    val cpuC: Float?,
    val ambientC: Float?,
    val thermalStatus: Int?,
    val headroom: Float? = null,
    val charging: Boolean = false,
    val batterySource: SensorAvailability = availabilityFor(batteryC),
    val cpuSource: SensorAvailability = availabilityFor(cpuC),
    val ambientSource: SensorAvailability = availabilityFor(ambientC),
)

enum class SensorAvailability {
    AVAILABLE,
    UNAVAILABLE,
    READ_FAILED;

    companion object {
        fun fromStorage(value: String?): SensorAvailability = when (value) {
            AVAILABLE.name -> AVAILABLE
            UNAVAILABLE.name -> UNAVAILABLE
            READ_FAILED.name -> READ_FAILED
            else -> READ_FAILED
        }
    }
}

private fun availabilityFor(value: Float?): SensorAvailability =
    if (value == null) SensorAvailability.UNAVAILABLE else SensorAvailability.AVAILABLE

data class DeviceMemoryInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val appPssBytes: Long,
)

fun Float.toUnit(fahrenheit: Boolean): Float = if (fahrenheit) this * 9f / 5f + 32f else this

fun formatTempDelta(celsius: Float, fahrenheit: Boolean): String {
    val value = if (fahrenheit) celsius * 9f / 5f else celsius
    val unit = if (fahrenheit) "F" else "C"
    return String.format(Locale.US, "%.1f°%s", value, unit)
}

fun formatMemory(bytes: Long): String {
    val gibibytes = bytes / (1024f * 1024f * 1024f)
    return if (gibibytes >= 1f) String.format(Locale.US, "%.1f GB", gibibytes)
    else String.format(Locale.US, "%.0f MB", bytes / (1024f * 1024f))
}

fun formatTemp(celsius: Float?, fahrenheit: Boolean, decimals: Int = 1): String {
    if (celsius == null) return "—"
    val unit = if (fahrenheit) "F" else "C"
    return String.format(Locale.US, "%.${decimals}f°%s", celsius.toUnit(fahrenheit), unit)
}

/** Maps PowerManager.THERMAL_STATUS_* to a friendly label. */
fun thermalLabel(status: Int?): String = when (status) {
    null -> "Unavailable"
    0 -> "Normal"
    1 -> "Light"
    2 -> "Moderate"
    3 -> "Severe"
    4 -> "Critical"
    5 -> "Emergency"
    6 -> "Shutdown"
    else -> "Unknown"
}
