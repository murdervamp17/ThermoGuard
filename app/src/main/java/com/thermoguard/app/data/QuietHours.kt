package com.thermoguard.app.data

fun isWithinQuietHours(currentMinute: Int, startMinute: Int, endMinute: Int): Boolean {
    val current = currentMinute.coerceIn(0, Settings.MINUTES_PER_DAY - 1)
    val start = startMinute.coerceIn(0, Settings.MINUTES_PER_DAY - 1)
    val end = endMinute.coerceIn(0, Settings.MINUTES_PER_DAY - 1)
    if (start == end) return false
    return if (start < end) current in start until end else current >= start || current < end
}
