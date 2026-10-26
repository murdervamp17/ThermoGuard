package com.thermoguard.app.data

import android.content.Context

class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("thermoguard_settings", Context.MODE_PRIVATE)

    var useFahrenheit: Boolean
        get() = prefs.getBoolean("fahrenheit", false)
        set(v) = prefs.edit().putBoolean("fahrenheit", v).apply()

    var alertsEnabled: Boolean
        get() = prefs.getBoolean("alerts", true)
        set(v) = prefs.edit().putBoolean("alerts", v).apply()

    /** Battery temperature (°C) that triggers an overheat alert. */
    var alertThresholdC: Float
        get() = prefs.getFloat("threshold", DEFAULT_THRESHOLD_C)
        set(v) = prefs.edit().putFloat("threshold", v).apply()

    var lastAlertAt: Long
        get() = prefs.getLong("last_alert", 0L)
        set(v) = prefs.edit().putLong("last_alert", v).apply()

    companion object {
        const val DEFAULT_THRESHOLD_C = 42f
        const val ALERT_COOLDOWN_MS = 30 * 60 * 1000L
        const val HISTORY_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
