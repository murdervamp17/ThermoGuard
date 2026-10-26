# ThermoGuard — Roadmap & Project Management

## Status: v1.0.0 (MVP) — code complete, awaiting first build and device test

### v1.0 MVP (delivered)
- [x] Live battery / CPU / ambient readings + Android thermal status & headroom
- [x] Animated arc gauge, insight card, live trend chart
- [x] Background logging every 15 min (WorkManager), 7-day retention (SQLite)
- [x] Overheat notifications with threshold + 30-min cooldown
- [x] History 6H/24H/7D with Min/Avg/Peak, CSV share
- [x] °C/°F, adaptive + themed launcher icon, dark "Ember & Frost" theme
- [x] GitHub Actions CI that builds the APK

### v1.1 Polish (next)
- [ ] Home-screen widget (Glance) showing current temp + colour
- [ ] Optional persistent "monitor mode" (foreground service, 1-min sampling) for gaming/charging
- [ ] Charging-aware alerts (separate threshold while charging)
- [ ] Light theme + Material You dynamic colour option
- [ ] Onboarding (3 screens: what we measure, alerts permission, privacy promise)

### v1.2 Insights
- [ ] Daily heat summary ("Hottest at 14:20 while charging")
- [ ] Per-app heat correlation (UsageStats, opt-in)
- [ ] Battery-health tips based on time spent above 40 °C

### v2.0 External sensors
- [ ] Bluetooth LE sensors (e.g. Xiaomi LYWSD03MMC, SwitchBot, Govee) for room/fridge/server-room monitoring
- [ ] Multiple named "spaces" with their own thresholds
- [ ] Optional cloud sync / multi-device dashboard

## Release checklist
1. Build debug APK (CI) → install on 2–3 devices (Samsung, Pixel, budget MediaTek)
2. Verify: battery reading, CPU availability, notification permission flow, background logging after 1 h
3. Create release keystore → signed release build
4. Play Store: privacy policy (no data collected), screenshots, feature graphic in Ember & Frost

## Known risks
| Risk | Mitigation |
|---|---|
| CPU thermal zones blocked on many Android 10+ devices | Shown as "Locked by OS"; battery is primary |
| OEM battery savers kill background work (Xiaomi, Huawei, Samsung) | v1.1 onboarding hint to disable optimisation |
| WorkManager minimum interval is 15 min | v1.1 foreground "monitor mode" |
| Thermal headroom can return NaN when polled too often | Handled → shows "—" |
