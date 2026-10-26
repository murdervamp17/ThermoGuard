package com.thermoguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thermoguard.app.data.TempReading
import com.thermoguard.app.data.SensorAvailability
import com.thermoguard.app.data.formatMemory
import com.thermoguard.app.data.formatTempDelta
import com.thermoguard.app.data.formatTemp
import com.thermoguard.app.data.thermalLabel
import com.thermoguard.app.ui.MainViewModel
import com.thermoguard.app.ui.components.LineChart
import com.thermoguard.app.ui.components.MetricCard
import com.thermoguard.app.ui.components.PanelCard
import com.thermoguard.app.ui.components.TempGauge
import com.thermoguard.app.ui.theme.TextBright
import com.thermoguard.app.ui.theme.TextDim
import com.thermoguard.app.ui.theme.statusColor
import com.thermoguard.app.ui.theme.tempColor
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val live by vm.live.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val fahrenheit by vm.fahrenheit.collectAsStateWithLifecycle()
    val threshold by vm.threshold.collectAsStateWithLifecycle()
    val memory by vm.memory.collectAsStateWithLifecycle()

    val battery = live?.batteryC
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(live?.thermalStatus)

        TempGauge(
            celsius = battery,
            display = formatTemp(battery, fahrenheit),
            label = "Battery",
            valueColor = tempColor(battery),
            caption = when {
                live == null -> "Reading sensors…"
                live?.batterySource == SensorAvailability.READ_FAILED -> "Battery sensor read failed"
                live?.batterySource == SensorAvailability.UNAVAILABLE -> "Battery sensor unavailable"
                live?.charging == true -> "⚡ Charging"
                else -> "On battery"
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                "CPU", formatTemp(live?.cpuC, fahrenheit), tempColor(live?.cpuC), Modifier.weight(1f),
                hint = live?.let { vm.sensorDescription(it.cpuSource) },
            )
            MetricCard(
                "Ambient", formatTemp(live?.ambientC, fahrenheit), tempColor(live?.ambientC), Modifier.weight(1f),
                hint = live?.let { vm.sensorDescription(it.ambientSource) },
            )
            val headroom = live?.headroom
            MetricCard(
                "Load",
                headroom?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                statusColor(live?.thermalStatus),
                Modifier.weight(1f),
                hint = "Throttle risk",
            )
        }

        InsightCard(live, threshold, fahrenheit, session.takeLast(4))

        memory?.let { info ->
            PanelCard {
                Text("Memory", color = TextBright, fontWeight = FontWeight.SemiBold)
                Text("Device RAM and this app’s process memory", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Total", formatMemory(info.totalBytes), TextBright, Modifier.weight(1f))
                    MetricCard("Available", formatMemory(info.availableBytes), TextBright, Modifier.weight(1f))
                    MetricCard("ThermoGuard", formatMemory(info.appPssBytes), TextBright, Modifier.weight(1f))
                }
                Text(
                    "App memory is this process's PSS estimate—not memory use by other apps.",
                    color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        PanelCard {
            Text("Live trend", color = TextBright, fontWeight = FontWeight.SemiBold)
            Text("Battery temperature since you opened the app", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LineChart(
                points = session.mapNotNull { r -> r.batteryC?.let { r.timestamp to it } },
                lineColor = tempColor(battery),
                fahrenheit = fahrenheit,
                thresholdC = threshold,
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
        }
    }
}

@Composable
private fun Header(status: Int?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("ThermoGuard", color = TextBright, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Keep your cool.", color = TextDim, fontSize = 13.sp)
        }
        val color = statusColor(status)
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(thermalLabel(status), color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InsightCard(
    r: TempReading?,
    thresholdC: Float,
    fahrenheit: Boolean,
    recentReadings: List<TempReading>,
) {
    val b = r?.batteryC
    val (title, body) = when {
        b == null && r?.batterySource == SensorAvailability.UNAVAILABLE ->
            "Battery temperature unavailable" to "Android does not expose a battery temperature reading on this device."
        b == null && r?.batterySource == SensorAvailability.READ_FAILED ->
            "Battery sensor read failed" to "Android did not return a battery temperature. ThermoGuard will try again."
        b == null -> "Warming up" to "Taking the first readings from your device's sensors."
        b >= thresholdC -> "Too hot" to
            "Above your ${formatTemp(thresholdC, fahrenheit, 0)} limit. Close heavy apps, unplug the charger, " +
            "take off the case and move out of direct sun."
        b >= thresholdC - 5f -> "Getting warm" to
            (if (r?.charging == true) "Charging while gaming or navigating heats things up fast. Give it a break."
            else "Heavy apps or sunlight are pushing the temperature up. Keep an eye on it.")
        b < 25f -> "Running cool" to "Everything's comfortable. Your battery loves this."
        else -> "All good" to "Normal operating temperature. Nothing to worry about."
    }
    PanelCard {
        Text(title, color = tempColor(b), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, color = TextDim, fontSize = 14.sp)
        val recentTemperatures = recentReadings.mapNotNull { it.batteryC }
        if (recentTemperatures.size >= 2) {
            val change = recentTemperatures.last() - recentTemperatures.first()
            if (kotlin.math.abs(change) >= 0.3f) {
                Text(
                    "Temperature is ${if (change > 0f) "rising" else "cooling"} " +
                        "${formatTempDelta(kotlin.math.abs(change), fahrenheit)} over recent readings.",
                    color = TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
