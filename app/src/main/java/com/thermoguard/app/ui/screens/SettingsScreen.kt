package com.thermoguard.app.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thermoguard.app.data.formatTemp
import com.thermoguard.app.ui.MainViewModel
import com.thermoguard.app.ui.components.PanelCard
import com.thermoguard.app.ui.theme.Critical
import com.thermoguard.app.ui.theme.Hot
import com.thermoguard.app.ui.theme.Ink
import com.thermoguard.app.ui.theme.TextBright
import com.thermoguard.app.ui.theme.TextDim
import com.thermoguard.app.ui.theme.Track
import com.thermoguard.app.ui.theme.tempColor
import com.thermoguard.app.work.Notifier
import kotlin.math.roundToInt
import java.time.LocalTime

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val fahrenheit by vm.fahrenheit.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val threshold by vm.threshold.collectAsStateWithLifecycle()
    val quietHoursEnabled by vm.quietHoursEnabled.collectAsStateWithLifecycle()
    val quietStartMinute by vm.quietStartMinute.collectAsStateWithLifecycle()
    val quietEndMinute by vm.quietEndMinute.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Ink, checkedTrackColor = Hot,
        uncheckedThumbColor = TextDim, uncheckedTrackColor = Track,
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", color = TextBright, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        PanelCard {
            SettingRow("Use Fahrenheit", "Show temperatures in °F") {
                Switch(checked = fahrenheit, onCheckedChange = vm::setFahrenheit, colors = switchColors)
            }
        }

        PanelCard {
            SettingRow("Overheat alerts", "Notify me when the battery passes my limit") {
                Switch(
                    checked = alerts,
                    onCheckedChange = { on ->
                        vm.setAlerts(on)
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.canNotify(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = switchColors,
                )
            }
            Text(
                "Alert threshold: ${formatTemp(threshold, fahrenheit, 0)}",
                color = tempColor(threshold), fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Slider(
                value = threshold,
                onValueChange = { vm.setThreshold(it.roundToInt().toFloat()) },
                valueRange = 35f..55f,
                steps = 19,
                enabled = alerts,
                colors = SliderDefaults.colors(thumbColor = Hot, activeTrackColor = Hot, inactiveTrackColor = Track),
            )
            Text(
                "Most phones are happiest below 35 °C. Above 45 °C batteries age faster, and many phones " +
                    "shut down or throttle hard around 50 °C. Alerts are rate-limited to one every 30 minutes.",
                color = TextDim, fontSize = 12.sp,
            )
        }

        PanelCard {
            SettingRow("Quiet hours", "Pause heat notifications on a daily schedule") {
                Switch(
                    checked = quietHoursEnabled,
                    onCheckedChange = vm::setQuietHoursEnabled,
                    colors = switchColors,
                )
            }
            if (quietHoursEnabled) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> vm.setQuietStartMinute(hour * 60 + minute) },
                                quietStartMinute / 60,
                                quietStartMinute % 60,
                                false,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("From ${formatClock(quietStartMinute)}", color = TextBright) }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> vm.setQuietEndMinute(hour * 60 + minute) },
                                quietEndMinute / 60,
                                quietEndMinute % 60,
                                false,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Until ${formatClock(quietEndMinute)}", color = TextBright) }
                }
                Text(
                    "Quiet hours use your device’s local time. A sustained high reading can alert after quiet hours end.",
                    color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Text(
                    "Default schedule when enabled: 10:00 PM–7:00 AM.",
                    color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        PanelCard {
            Text("Data", color = TextBright, fontWeight = FontWeight.SemiBold)
            Text(
                "Readings are stored only on this device for 7 days.",
                color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedButton(onClick = { confirmClear = true }) { Text("Clear history", color = Critical) }
        }

        PanelCard {
            Text("About", color = TextBright, fontWeight = FontWeight.SemiBold)
            Text(
                "ThermoGuard 1.0.0 · Live readings every 2 s while open, background readings every 15 min. " +
                    "Temperature sources depend on what Android exposes. The home-screen widget follows the " +
                    "same background update schedule.",
                color = TextDim, fontSize = 12.sp,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("This permanently deletes every saved reading.") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear", color = Critical) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

private fun formatClock(minutes: Int): String {
    val time = LocalTime.of(minutes / 60, minutes % 60)
    val hour = when (val h = time.hour % 12) { 0 -> 12; else -> h }
    return "$hour:${time.minute.toString().padStart(2, '0')} ${if (time.hour < 12) "AM" else "PM"}"
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextBright, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextDim, fontSize = 12.sp)
        }
        trailing()
    }
}
