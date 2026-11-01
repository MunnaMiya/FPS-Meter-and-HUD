# Android 10 Real Render-Rate (SurfaceFlinger) Pipeline & Settings Toggle

Resolves the Android 10 (API 29) fixed-refresh-rate reporting issue where the HUD reports the display's hardware VSYNC lock (e.g., 60 Hz) instead of the underlying game engine's actual rendered frame rate (e.g., 20 FPS), while preserving the existing Android 11–17 telemetry pipeline without modification and offering an optional manual toggle in Overlay Settings.

## User Review & Critical Decisions

> [!IMPORTANT]
> The default behavior on **Android 11 through Android 17 (API 30+)** remains 100% unchanged unless the user explicitly turns on the new **SurfaceFlinger Real Render FPS** override toggle in Overlay Settings. On **Android 10 (API 29)**, this mode activates automatically when Root is granted.

- **Confirmed Decision 1 (Target Privilege Environment)**: Optimized for **Rooted Android 10** using the existing single persistent interactive `su` shell session (ensuring zero repeated Superuser permission toasts).
- **Confirmed Decision 2 (Activation & Version Isolation)**: Automatically enabled on **Android 10 (API 29)** by default, with an **optional toggle in Overlay Settings** (`SurfaceFlinger Real Render FPS`) so users on any Android version can turn it on or off at will without risking compatibility on Android 11–17.

---

## 1. Overview & Core Concept

- **What It Does**:
  - On Android 10, `Choreographer` callbacks inside an overlay process tick at the display panel's fixed VSYNC rate (60 Hz), even when a heavy 3D game underneath only commits 20 frames per second to the compositor.
  - When **SurfaceFlinger Real Render FPS** is active (automatically on Android 10, or when toggled on in Overlay Settings), the telemetry engine queries the hardware compositor's actual committed frame/page-flip counters (`SurfaceFlinger` transaction code `1013` hardware page-flip counter and focused `SurfaceView`/`BLAST` layer latency timestamps) over the already-open persistent `su` pipe.
  - By measuring the exact delta of committed compositor frames (`(flipsNow - flipsPrev) / elapsedSeconds`), the meter displays the true rendered game FPS (e.g., 20 FPS during a frame drop) rather than the static 60 Hz screen refresh rate.
- **Target Audience / Persona**: Mobile gamers and performance enthusiasts testing frame pacing on rooted Android 10 devices as well as modern Android 11–17 devices.
- **Key Value**: True game-engine render rate accuracy on Android 10 without sacrificing the proven stability of Android 11–17 or triggering repeated root toasts.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Automatic Android 10 Detection**: When launching the app on an Android 10 (`API 29`) device with Root granted, the **SurfaceFlinger Real Render FPS** mode defaults to `ON` and reads live compositor buffer commits.
  2. **Android 11–17 Preservation**: When launching on Android 11 through Android 17 (`API 30+`), the app defaults to the existing VSYNC/Display pipeline so current compatibility is completely untouched.
  3. **Overlay Settings Toggle**: In the **Overlay** settings tab, a new setting row (**SurfaceFlinger Real Render FPS (Android 10 Fix)**) allows the user to enable or disable the hardware compositor reader at any time.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Clean, high-contrast tactical telemetry interface supporting the existing Dark (`#0B0F17` canvas, `#10B981` neon emerald accent) and Light (`#F8FAFC` canvas) themes.
  - *Color Palette & Mood*: Uses semantic Material 3 surface tokens with `AccentGreen` (`#10B981`) active switches and status badges.
  - *Typography & Hierarchy*: Tabular monospace numerals (`FontFamily.Monospace`) for live FPS readouts and clear descriptive subtitles explaining the Android 10 compositor hook.
  - *Component Styling & Layout*: Integrated seamlessly into the existing **Expanded HUD Metrics** / **Overlay Settings** card with generous spacing and 48dp minimum touch targets.
- **Interactive Feedback & Motion**: Instant state persistence via `SettingsRepository` with immediate live telemetry switching in both the in-app dashboard and the floating HUD overlay.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Two-Stage SurfaceFlinger Frame Counter via Persistent Root Pipe**
  - *Chosen Approach*: Query `service call SurfaceFlinger 1013` (which returns the raw hexadecimal page-flip count from the Android SurfaceFlinger hardware composer) paired with focused game layer timestamp delta parsing (`dumpsys SurfaceFlinger --latency <layer>`) inside the existing persistent `su` session.
  - *Why*: `service call SurfaceFlinger 1013` executes in under 2 milliseconds over an already-open `su` pipe, works across all Android 10 vendor ROMs (AOSP, MIUI, OneUI, OxygenOS, RealmeUI), and counts actual compositor buffer swaps rather than display VSYNC ticks. Because it reuses the single open `su` stream, it produces **zero** Superuser toast notifications.
  - *Alternatives Considered*: Reading `dumpsys SurfaceFlinger --latency` without a layer name was previously returning only the static VSYNC refresh period on line 0 (16.67ms = 60 FPS), which caused the 60 FPS lock on Android 10.
- **Decision 2: Strict Version Gate with Manual Override**
  - *Chosen Approach*: Initialize the setting `useSurfaceFlingerFps` to `Build.VERSION.SDK_INT == Build.VERSION_CODES.Q` by default, while exposing a toggle in **Overlay Settings** so the user has full control.
  - *Why*: Guarantees zero regression on Android 11–17 out of the box while fulfilling both Android 10 accuracy and optional manual control on any OS version.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌─────────────────────────────────────────────────────────────────────────┐
│                         OverlaySettingsTab UI                           │
│  [Toggle: SurfaceFlinger Real Render FPS (Auto-Enabled on Android 10)]  │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ updates state
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       SettingsRepository (Prefs)                        │
│     useSurfaceFlingerFps: Boolean (Default = true on API 29, else false)│
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ observed by
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        HardwareMonitor Engine                           │
│  ┌────────────────────────────────────┐  ┌───────────────────────────┐  │
│  │ Standard Pipeline (API 30+ Default)│  │ SurfaceFlinger Render Path│  │
│  │ • Choreographer VSYNC Frame Delta  │  │ (API 29 Default / Toggled)│  │
│  │ • DisplayManager Refresh Rate      │  │ • Page-Flip Delta (1013)  │  │
│  └────────────────────────────────────┘  │ • Layer Buffer Timestamps │  │
│                                          └─────────────┬─────────────┘  │
└────────────────────────────────────────────────────────┼────────────────┘
                                                         │ stdin / stdout
                                                         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│               PrivilegeManager (Singleton Persistent su)                │
│         Single open ProcessBuilder("su") pipe — Zero Repeat Toasts      │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - Extend `OverlaySettings` with `useSurfaceFlingerFps: Boolean = (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)`.
  - Persist `useSurfaceFlingerFps` in `SettingsRepository` (`SharedPreferences`), defaulting to `true` when `Build.VERSION.SDK_INT == Build.VERSION_CODES.Q` (Android 10) and `false` on Android 11–17 (`API 30+`).
- **Interactive Component & State Mapping**:
  - Toggling **SurfaceFlinger Real Render FPS** in `OverlaySettingsTab` immediately updates `SettingsRepository.settings`.
  - `HardwareMonitor` reads `settingsRepository.settings.value.useSurfaceFlingerFps`.
  - When `useSurfaceFlingerFps` is `true` and Root is active:
    1. Executes `service call SurfaceFlinger 1013` over the persistent root shell and parses the 32-bit hex page-flip counter (`Parcel(00000000 0000xxxx ...)`).
    2. Computes `(currentFlips - previousFlips) * 1000f / elapsedMs`, clamping out idle screen states and smoothly reporting the real rendered FPS (e.g., 20 FPS when a game lags).
    3. If the vendor ROM restricts `1013`, automatically falls back to querying the active top game layer (`dumpsys SurfaceFlinger --list` + `dumpsys SurfaceFlinger --latency <topLayer>`) to count actual presented frame timestamps within the last sampling window.
  - When `useSurfaceFlingerFps` is `false` (default on Android 11–17), `HardwareMonitor` executes the exact existing `calculatedFps` pipeline without any change.
