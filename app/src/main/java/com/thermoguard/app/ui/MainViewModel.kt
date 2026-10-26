package com.thermoguard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thermoguard.app.data.ReadingStore
import com.thermoguard.app.data.Settings
import com.thermoguard.app.data.TempReading
import com.thermoguard.app.data.TemperatureReader
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val reader = TemperatureReader(app)
    private val store = ReadingStore.get(app)
    private val settings = Settings(app)

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
        .onEach { r -> _session.update { (it + r).takeLast(SESSION_POINTS) } }
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

    private var historyHours = 24

    fun setFahrenheit(v: Boolean) { settings.useFahrenheit = v; _fahrenheit.value = v }
    fun setThreshold(c: Float) { settings.alertThresholdC = c; _threshold.value = c }
    fun setAlerts(v: Boolean) { settings.alertsEnabled = v; _alerts.value = v }

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

    fun historyCsv(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("timestamp,battery_c,cpu_c,ambient_c,thermal_status")
            history.value.forEach { r ->
                appendLine(
                    listOf(
                        fmt.format(Date(r.timestamp)),
                        r.batteryC?.toString() ?: "",
                        r.cpuC?.toString() ?: "",
                        r.ambientC?.toString() ?: "",
                        r.thermalStatus?.toString() ?: "",
                    ).joinToString(",")
                )
            }
        }
    }

    companion object {
        private const val LIVE_INTERVAL_MS = 2_000L
        private const val SESSION_POINTS = 150
    }
}
