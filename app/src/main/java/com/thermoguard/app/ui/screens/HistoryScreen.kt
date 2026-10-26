package com.thermoguard.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thermoguard.app.data.formatTemp
import com.thermoguard.app.data.SensorAvailability
import com.thermoguard.app.ui.MainViewModel
import com.thermoguard.app.ui.components.LineChart
import com.thermoguard.app.ui.components.MetricCard
import com.thermoguard.app.ui.components.PanelCard
import com.thermoguard.app.ui.theme.Frost
import com.thermoguard.app.ui.theme.Hot
import com.thermoguard.app.ui.theme.Ink
import com.thermoguard.app.ui.theme.TextBright
import com.thermoguard.app.ui.theme.TextDim
import com.thermoguard.app.ui.theme.tempColor
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val history by vm.history.collectAsStateWithLifecycle()
    val fahrenheit by vm.fahrenheit.collectAsStateWithLifecycle()
    val threshold by vm.threshold.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var hours by rememberSaveable { mutableIntStateOf(24) }

    LaunchedEffect(hours) { vm.loadHistory(hours) }

    val battery = history.mapNotNull { r -> r.batteryC?.let { r.timestamp to it } }
    val cpu = history.mapNotNull { r -> r.cpuC?.let { r.timestamp to it } }
    val values = battery.map { it.second }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("History", color = TextBright, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1 to "1H", 6 to "6H", 24 to "24H", 72 to "3D", 168 to "7D").forEach { (h, label) ->
                if (h == hours) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Hot, contentColor = Ink),
                    ) { Text(label) }
                } else {
                    OutlinedButton(onClick = { hours = h }) { Text(label, color = TextDim) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val min = values.minOrNull()
            val avg = if (values.isEmpty()) null else values.average().toFloat()
            val max = values.maxOrNull()
            MetricCard("Min", formatTemp(min, fahrenheit), tempColor(min), Modifier.weight(1f))
            MetricCard("Avg", formatTemp(avg, fahrenheit), tempColor(avg), Modifier.weight(1f))
            MetricCard("Peak", formatTemp(max, fahrenheit), tempColor(max), Modifier.weight(1f))
        }

        PanelCard {
            Text("Battery", color = TextBright, fontWeight = FontWeight.SemiBold)
            Text("Dashed line = your alert threshold", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LineChart(battery, Hot, fahrenheit, Modifier.fillMaxWidth().height(200.dp), thresholdC = threshold)
        }

        PanelCard {
            Text("CPU", color = TextBright, fontWeight = FontWeight.SemiBold)
            if (cpu.size >= 2) {
                Spacer(Modifier.height(8.dp))
                LineChart(cpu, Frost, fahrenheit, Modifier.fillMaxWidth().height(160.dp))
            } else {
                val source = history.lastOrNull()?.cpuSource
                val explanation = when (source) {
                    SensorAvailability.UNAVAILABLE ->
                        "CPU temperature is not exposed by Android/device."
                    SensorAvailability.READ_FAILED ->
                        "CPU temperature reads failed; no trend is available yet."
                    else -> "Not enough CPU readings in this range to draw a trend."
                }
                Text(explanation, color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Text(
            "${history.size} readings in range · logged automatically every 15 minutes, kept for 7 days.",
            color = TextDim, fontSize = 12.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.logNow() },
                colors = ButtonDefaults.buttonColors(containerColor = Hot, contentColor = Ink),
                modifier = Modifier.weight(1f),
            ) { Text("Log now") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "ThermoGuard troubleshooting report")
                            putExtra(Intent.EXTRA_TEXT, vm.troubleshootingReport())
                        }
                        context.startActivity(Intent.createChooser(send, "Share troubleshooting report"))
                    }
                },
                enabled = history.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text("Share report", color = TextBright) }
        }
    }
}
