# ThermoGuard 🌡️ — Android temperature monitor

Kotlin + Jetpack Compose (Material 3). minSdk 26 (Android 8.0), targetSdk 35.

## Build the APK

### Option A — GitHub (no install needed)
1. Create a new GitHub repo and push this folder to it.
2. GitHub Actions runs `.github/workflows/build-apk.yml` automatically.
3. Open **Actions → latest run → Artifacts → ThermoGuard-debug-apk**, download it and install it on your phone.
   You need to allow "Install unknown apps".

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
│   ├── TemperatureReader.kt # Battery, CPU thermal zones, ambient sensor, thermal status/headroom
│   ├── ReadingStore.kt      # SQLite history (7-day retention)
│   ├── Settings.kt          # Preferences (unit, threshold, alerts)
│   └── TempReading.kt       # Model + formatting helpers
├── work/
│   ├── MonitorWorker.kt     # 15-min background logging + alert logic
│   └── Notifier.kt          # Overheat notifications
└── ui/
    ├── MainViewModel.kt
    ├── theme/Theme.kt       # "Ember & Frost" palette
    ├── components/          # TempGauge, LineChart, cards
    └── screens/             # Dashboard, History, Settings
```

See `docs/CREATIVE_DIRECTION.md` and `docs/ROADMAP.md`.
