# Screen Time Tracker — Android Studio Build Instructions

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 (bundled with Android Studio) |
| Android SDK | API 23 (min) — API 34 (compile/target) |
| Gradle | 8.x (via wrapper, auto-downloaded) |

---

## Step 1 — Open the Project

1. Launch **Android Studio**
2. Click **File → Open**
3. Navigate to and select the `ScreenTimeTracker/` folder (the one containing `settings.gradle`)
4. Click **OK** — Android Studio will sync the Gradle project automatically
5. Wait for the "Gradle sync finished" message in the status bar

---

## Step 2 — Verify SDK is Installed

1. Go to **Tools → SDK Manager**
2. Under **SDK Platforms**, ensure **Android 14 (API 34)** is installed
3. Under **SDK Tools**, ensure:
   - **Android SDK Build-Tools 34**
   - **Android SDK Platform-Tools**
   are installed
4. Click **Apply** if any items need installing

---

## Step 3 — Build the APK

### Option A — Debug APK (recommended for testing)

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Android Studio will show a notification when done:
> "APK(s) generated successfully"  
> Click **locate** to open the output folder

The APK is at:
```
ScreenTimeTracker/app/build/outputs/apk/debug/app-debug.apk
```

### Option B — Release APK (for distribution)

1. **Build → Generate Signed Bundle / APK**
2. Choose **APK** → **Next**
3. Create or select a **keystore file**
4. Fill in key alias, passwords → **Next**
5. Select **release** build variant → **Finish**

Output location:
```
ScreenTimeTracker/app/build/outputs/apk/release/app-release.apk
```

---

## Step 4 — Install on Device / Emulator

### Physical Android device
1. Enable **Developer Options** → **USB Debugging** on the phone
2. Connect via USB
3. Click the **Run ▶** button in Android Studio
4. Select your device from the list

### Using ADB from terminal
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 5 — Grant Usage Access Permission

This is required for the app to read screen time data:

1. Open the app on the device
2. Tap **"Grant Usage Access"** on the Dashboard screen
3. On the system Settings screen that opens, find **"Screen Time Tracker"**
4. Enable the toggle → Press the back button
5. The app will now show real usage data

> **Why is this needed?** Android's `UsageStatsManager` API is protected by the
> `PACKAGE_USAGE_STATS` permission, which cannot be granted automatically — the user
> must enable it manually in Settings. This is intentional Android security design.

---

## Troubleshooting

### Gradle sync fails — "Could not resolve com.github.PhilJay:MPAndroidChart"
Make sure you have internet access. JitPack requires an internet connection on first build.

### "SDK location not found"
Run **Tools → SDK Manager** and confirm Android SDK is installed. Android Studio will set the `local.properties` file automatically.

### App shows "0m" usage data after permission granted
The `UsageStatsManager` on some devices only starts recording after the first reboot post-permission grant. Use the device normally for a few minutes — the periodic WorkManager sync (every 15 minutes) will populate data.

### Build error: `ksp` not found
Ensure you are using Android Studio **Hedgehog (2023.1.1)** or newer. Older versions may not support KSP 1.9.x.

---

## Project Architecture

```
app/
├── data/
│   ├── db/          AppDatabase.kt, AppUsageDao.kt   (Room)
│   ├── model/       AppUsageRecord, AppSummary, DailyTotal
│   └── repository/  UsageRepository.kt
├── ui/
│   ├── dashboard/   DashboardFragment.kt
│   ├── apps/        InstalledAppsFragment.kt, AppUsageAdapter.kt
│   ├── uninstalled/ UninstalledAppsFragment.kt
│   └── settings/    SettingsFragment.kt
├── viewmodel/       DashboardViewModel, AppsViewModel, SettingsViewModel
├── worker/          UsageSyncWorker.kt, BootReceiver.kt
└── util/            DateUtils, PermissionUtils, ExportUtils
```

### Key design decisions

| Decision | Rationale |
|----------|-----------|
| **Room database** | All usage data is persisted locally; works offline; preserves uninstalled app history |
| **UsageStatsManager** | Android's official API for per-app usage; requires `PACKAGE_USAGE_STATS` special permission |
| **WorkManager** | Syncs usage data every 15 minutes in the background; survives device reboots |
| **MVVM** | ViewModels + LiveData decouple data from UI; fragments observe changes reactively |
| **ListAdapter + DiffUtil** | Efficient RecyclerView updates without full re-renders |
| **MPAndroidChart** | Proven charting library; bar chart for daily/weekly/monthly breakdown |

---

## Known Limitations

1. **UsageStatsManager accuracy**: The API reports foreground time in daily buckets. Precision to the exact second is not guaranteed by Android.
2. **Background sync timing**: WorkManager's 15-minute period is approximate. Android may delay it when battery saver is on.
3. **Uninstall detection**: Uninstalls are detected the next time a sync runs. Immediate detection would require a PACKAGE_REMOVED broadcast (which requires a foreground service or a running app).
4. **Per-app icons**: Loading app icons is expensive; this version shows text-only rows for speed. You can extend `AppUsageAdapter` to load icons via `PackageManager.getApplicationIcon()` with Glide or Coil.

---

## Features Summary

- Real-time tracking via Android `UsageStatsManager` API
- Room database persistence (all data stays offline, on-device)
- Daily / Weekly / Monthly usage views with bar chart
- Top apps ranked by usage time
- Uninstalled apps history (data preserved forever)
- Dark mode support (system + manual toggle)
- Export all data as JSON or CSV (shared via Android share sheet)
- Optional daily screen time limit warning
- Background sync every 15 minutes via WorkManager
- Survives device reboots (BootReceiver re-schedules sync)
