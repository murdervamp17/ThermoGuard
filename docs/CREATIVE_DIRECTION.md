# ThermoGuard — Creative Direction

## 1. Concept
**"Keep your cool."** ThermoGuard is a calm, glanceable guardian for your phone's temperature.
It should feel like a premium instrument panel, not a utility. You open it, understand the state in
under a second, and close it reassured. If something is wrong, it tells you plainly and suggests a fix.

## 2. Product pillars
| Pillar | What it means in practice |
|---|---|
| **Glanceable** | One hero gauge. Colour alone tells the story before you read a number. |
| **Honest** | Shows "—", "Locked by OS" or "No sensor" instead of fake data. |
| **Helpful** | Every state has a plain-language insight with a concrete action. |
| **Private** | No internet permission. Data stays on the device for 7 days. |
| **Light** | Polls only while visible (every 2 s); background checks every 15 min via WorkManager. |

## 3. Visual identity — "Ember & Frost"
Dark-first UI. Heat is shown as a continuous journey from frost to ember.

| Token | Hex | Use |
|---|---|---|
| Frost | `#4FC3F7` | < 30 °C, secondary accents, CPU chart |
| Cool | `#26A69A` | 30–37 °C, "Normal" status |
| Warm | `#FFB74D` | 37–42 °C |
| Hot (brand) | `#FF7043` | 42–47 °C, primary buttons, selected nav |
| Critical | `#E53935` | ≥ 47 °C, threshold line, destructive actions |
| Ink | `#0E1116` | App background |
| Panel | `#161B22` | Cards |
| Track | `#242B35` | Gauge track, gridlines |
| Text bright / dim | `#E6EDF3` / `#8B949E` | Primary / secondary text |

**Shape:** 18–20 dp rounded cards and pill-shaped status chips.
**Type:** System Roboto with a strong hierarchy: 52 sp bold hero value, 26 sp screen titles,
uppercase labels with letter-spacing for an instrument feel.
**Signature element:** a 270° arc gauge with a sweep gradient and a glowing marker that eases (900 ms) to each new value.
**Icon:** A white thermometer with ember mercury and frost tick marks on Panel, plus a monochrome themed-icon variant.

## 4. Voice and microcopy
Friendly, brief, never alarmist. Say the state first, then the action.
- Cool: "Running cool. Everything's comfortable. Your battery loves this."
- Normal: "All good. Normal operating temperature."
- Warm: "Getting warm. Charging while gaming or navigating heats things up fast."
- Hot: "Too hot. Close heavy apps, unplug the charger, take off the case and move out of direct sun."
- Notification: "Your phone is running hot. Battery at 44.0°C, above your alert threshold."

## 5. Information architecture
1. **Live:** status chip, hero battery gauge, CPU / Ambient / Load tiles, insight card and live trend chart.
2. **History:** 6H / 24H / 7D ranges, Min / Avg / Peak, battery chart with threshold line, CPU chart, Log now, Share CSV.
3. **Settings:** °C/°F, alerts toggle, threshold slider (35–55 °C, default 42 °C), clear history, about.

## 6. Design decisions I made
- **Battery temperature is the hero.** It is the only sensor guaranteed on every Android phone.
  CPU zones are often locked on Android 10+, and ambient sensors are rare.
- **Alerts use battery temperature only.** CPU spikes are normal and would cause alert fatigue.
- **Alert cooldown of 30 min** to respect the user.
- **No internet permission.** This is a trust signal for a monitoring app.
