# ThermoGuard 🌡️ — Android temperature monitor

Kotlin + Jetpack Compose (Material 3). minSdk 26 (Android 8.0), targetSdk 35.

## Features

- Live battery, CPU/SoC, ambient, and Android thermal-status readings when exposed by the device. Missing sources are shown as unavailable; a source that exists but fails to return a reading is identified separately.
- A memory panel reports total and available device RAM and ThermoGuard's own process PSS. Android does not provide this app with reliable per-app RAM use for other apps, so no other-app values are shown.
- Configurable battery heat alerts (35–55 °C), daily quiet hours, a 30-minute alert cooldown, and rising/cooling context in alert notifications.
- Local temperature history with 1-hour, 6-hour, 24-hour, 3-day, and 7-day ranges, battery and CPU trends, and an optional shareable troubleshooting report.
- A home-screen widget showing the latest battery temperature and Android thermal status. It refreshes with the app's foreground readings and background monitoring; Android schedules the background monitor at its 15-minute minimum interval, which is not an exact alarm.

Add the widget from the Android launcher’s **Widgets** picker. Settings and history stay on the device unless you explicitly share a troubleshooting report.

## Build the APK

### Option A — GitHub (no install needed)
1. Create a new GitHub repo and push this folder to it.
2. GitHub Actions runs `.github/workflows/build-apk.yml` automatically on every push.
3. Open **Actions → latest run → Artifacts** and download either `ThermoGuard-debug-apk` or `ThermoGuard-release-apk`.
   The release-variant APK is unsigned (`app-release-unsigned.apk`) and cannot be installed until it is signed with an Android signing key. The debug APK is signed for testing and can be sideloaded; allow "Install unknown apps" on your phone.

### Option B — Android Studio
1. Install Android Studio (Ladybug or newer).
2. **File → Open** → select this folder and let Gradle sync. It downloads Gradle 8.9 and the SDK 35 automatically.
3. **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.
   The output is at `app/build/outputs/apk/debug/app-debug.apk`.

### Option C — Command line
With JDK 17, Android SDK (`ANDROID_HOME`) and Gradle 8.9 installed:
```
gradle assembleDebug
```
Run `gradle wrapper` once if you want a `./gradlew` script.

## Project layout
```
app/src/main/java/com/thermoguard/app/
├── MainActivity.kt          # Scaffold + bottom navigation
├── ThermoGuardApp.kt        # Notification channel + schedules background worker
├── data/
│   ├── TemperatureReader.kt # Temperatures, source availability, and memory
│   ├── ReadingStore.kt      # SQLite history and sensor-source status (7-day retention)
│   ├── Settings.kt          # Persistent unit, alert threshold, and quiet-hours preferences
│   └── TempReading.kt       # Reading/memory models and formatting helpers
├── work/
│   ├── MonitorWorker.kt     # Periodic background logging, widget refresh, and alert logic
│   ├── WidgetRefreshWorker.kt
│   └── Notifier.kt          # Overheat notifications
├── widget/
│   └── ThermoWidgetProvider.kt
└── ui/
    ├── MainViewModel.kt
    ├── theme/Theme.kt       # "Ember & Frost" palette
    ├── components/          # TempGauge, LineChart, cards
    └── screens/             # Dashboard, History, Settings
```

See `docs/CREATIVE_DIRECTION.md` and `docs/ROADMAP.md`.
