package com.thermoguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.os.Build
import com.thermoguard.app.data.ReadingStore
import com.thermoguard.app.data.Settings
import com.thermoguard.app.data.TempReading
import com.thermoguard.app.data.TemperatureReader
import com.thermoguard.app.data.DeviceMemoryInfo
import com.thermoguard.app.data.SensorAvailability
import com.thermoguard.app.data.thermalLabel
import com.thermoguard.app.widget.ThermoWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val reader = TemperatureReader(app)
    private val store = ReadingStore.get(app)
    private val settings = Settings(app)
    private val _memory = MutableStateFlow<DeviceMemoryInfo?>(null)
    val memory: StateFlow<DeviceMemoryInfo?> = _memory.asStateFlow()

    private val _session = MutableStateFlow<List<TempReading>>(emptyList())
    /** Readings taken while the app is open (last ~5 minutes) for the live trend. */
    val session: StateFlow<List<TempReading>> = _session.asStateFlow()

    /** Live reading every 2 s — only polls while the UI is visible. */
    val live: StateFlow<TempReading?> = flow {
        while (true) {
            emit(reader.read())
            delay(LIVE_INTERVAL_MS)
        }
    }
        .onEach { r ->
            _session.update { (it + r).takeLast(SESSION_POINTS) }
            ThermoWidgetProvider.publish(getApplication(), r)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _history = MutableStateFlow<List<TempReading>>(emptyList())
    val history: StateFlow<List<TempReading>> = _history.asStateFlow()

    private val _fahrenheit = MutableStateFlow(settings.useFahrenheit)
    val fahrenheit: StateFlow<Boolean> = _fahrenheit.asStateFlow()

    private val _threshold = MutableStateFlow(settings.alertThresholdC)
    val threshold: StateFlow<Float> = _threshold.asStateFlow()

    private val _alerts = MutableStateFlow(settings.alertsEnabled)
    val alerts: StateFlow<Boolean> = _alerts.asStateFlow()
    private val _quietHoursEnabled = MutableStateFlow(settings.quietHoursEnabled)
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()
    private val _quietStartMinute = MutableStateFlow(settings.quietStartMinute)
    val quietStartMinute: StateFlow<Int> = _quietStartMinute.asStateFlow()
    private val _quietEndMinute = MutableStateFlow(settings.quietEndMinute)
    val quietEndMinute: StateFlow<Int> = _quietEndMinute.asStateFlow()

    private var historyHours = 24

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _memory.value = reader.memoryInfo()
                delay(MEMORY_INTERVAL_MS)
            }
        }
    }

    fun setFahrenheit(v: Boolean) {
        settings.useFahrenheit = v
        _fahrenheit.value = v
        viewModelScope.launch(Dispatchers.IO) {
            ThermoWidgetProvider.publish(getApplication(), store.latest(), force = true)
        }
    }
    fun setThreshold(c: Float) { settings.alertThresholdC = c; _threshold.value = c }
    fun setAlerts(v: Boolean) { settings.alertsEnabled = v; _alerts.value = v }
    fun setQuietHoursEnabled(v: Boolean) { settings.quietHoursEnabled = v; _quietHoursEnabled.value = v }
    fun setQuietStartMinute(v: Int) { settings.quietStartMinute = v; _quietStartMinute.value = settings.quietStartMinute }
    fun setQuietEndMinute(v: Int) { settings.quietEndMinute = v; _quietEndMinute.value = settings.quietEndMinute }

    fun loadHistory(hours: Int) {
        historyHours = hours
        viewModelScope.launch(Dispatchers.IO) {
            _history.value = store.since(System.currentTimeMillis() - hours * 3_600_000L)
        }
    }

    fun logNow() {
        viewModelScope.launch(Dispatchers.IO) {
            store.insert(reader.read())
            _history.value = store.since(System.currentTimeMillis() - historyHours * 3_600_000L)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            store.clear()
            _history.value = emptyList()
        }
    }

    suspend fun troubleshootingReport(): String = withContext(Dispatchers.IO) {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val device = "${Build.MANUFACTURER} ${Build.MODEL}".replace(",", " ").replace("\n", " ")
        val memoryInfo = reader.memoryInfo()
        buildString {
            appendLine("ThermoGuard troubleshooting report")
            appendLine("Generated,${fmt.format(Date())}")
            appendLine("Device,$device")
            appendLine("Android,${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Temperature units,${if (settings.useFahrenheit) "Fahrenheit" else "Celsius"}")
            appendLine("Alert threshold (C),${settings.alertThresholdC}")
            appendLine("Alerts enabled,${settings.alertsEnabled}")
            appendLine("Quiet hours enabled,${settings.quietHoursEnabled}")
            appendLine(
                "Device memory total/available (bytes),${memoryInfo?.totalBytes ?: ""}/${memoryInfo?.availableBytes ?: ""}"
            )
            appendLine("ThermoGuard process PSS (bytes),${memoryInfo?.appPssBytes ?: ""}")
            appendLine("History range (hours),$historyHours")
            appendLine("Reading count,${history.value.size}")
            appendLine()
            appendLine("record_type,timestamp,battery_c,cpu_c,ambient_c,thermal_status,battery_source,cpu_source,ambient_source")
            history.value.forEach { r ->
                appendLine(
                    listOf(
                        "reading",
                        fmt.format(Date(r.timestamp)),
                        r.batteryC?.toString() ?: "",
                        r.cpuC?.toString() ?: "",
                        r.ambientC?.toString() ?: "",
                        r.thermalStatus?.let(::thermalLabel) ?: "",
                        r.batterySource.name,
                        r.cpuSource.name,
                        r.ambientSource.name,
                    ).joinToString(",")
                )
            }
        }
    }

    fun sensorDescription(availability: SensorAvailability): String = when (availability) {
        SensorAvailability.AVAILABLE -> "Available"
        SensorAvailability.UNAVAILABLE -> "Not exposed by Android/device"
        SensorAvailability.READ_FAILED -> "Present, but the latest read failed"
    }

    companion object {
        private const val LIVE_INTERVAL_MS = 2_000L
        private const val SESSION_POINTS = 150
        private const val MEMORY_INTERVAL_MS = 15_000L
    }
}
