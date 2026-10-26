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
)

fun Float.toUnit(fahrenheit: Boolean): Float = if (fahrenheit) this * 9f / 5f + 32f else this

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
