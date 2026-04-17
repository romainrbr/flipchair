# Lawnchair — Claude Code Notes

## Build

```bash
GRADLE_OPTS="-Xmx16g" ./gradlew assembleLawnWithQuickstepGithub -Pkotlin.daemon.jvmargs="-Xmx16g" -Dorg.gradle.workers.max=12
```

Output APKs land in `build/outputs/apk/lawnWithQuickstepGithub/`.

## Install

```bash
adb install -r "./build/outputs/apk/lawnWithQuickstepGithub/release/Lawnchair.16.Dev.(...).github.release.apk"
```

The APK filename includes the short git hash, so glob or `find` if the exact name is unknown:

```bash
adb install -r "$(find build/outputs/apk/lawnWithQuickstepGithub/release -name '*.apk' | head -1)"
```

## Signing

Release builds are signed with a local keystore. Both `keystore.properties` and `lawnchair-release.jks` are gitignored and must exist locally.

To (re)generate the keystore from scratch:

```bash
keytool -genkeypair \
  -alias lawnchair \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore lawnchair-release.jks \
  -storepass lawnchair_store \
  -keypass lawnchair_store \
  -dname "CN=Lawnchair, O=Personal, C=FR" \
  -noprompt
```

`keystore.properties` (already created, gitignored):

```properties
storeFile=lawnchair-release.jks
storePassword=lawnchair_store
keyAlias=lawnchair
keyPassword=lawnchair_store
```

If `keystore.properties` is missing, Gradle falls back to the debug keystore automatically.

## Branch

Active feature branch: `lawnchair-accessibility` (base: `16-dev`)

## Cover Screen Architecture (Samsung Z Flip)

### Key Files

| File | Role |
|---|---|
| `lawnchair/src/app/lawnchair/LawnchairAccessibilityService.kt` | Core service — detects cover screen state, auto-launches/restores apps, intercepts Home key, tracks recent packages |
| `lawnchair/src/app/lawnchair/gestures/handlers/RecentsGestureHandler.kt` | On cover screen: shows `ComposeBottomSheet` with recent apps; on main screen: `GLOBAL_ACTION_RECENTS` |
| `lawnchair/src/app/lawnchair/util/CoverScreenCompat.kt` | Manages Samsung MultiStar cover screen app allowlist (`Settings.Secure` key) |
| `lawnchair/src/app/lawnchair/util/Compatibility.kt` | `isSamsung` checks `ro.build.version.oneui` system property |
| `lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt` | Two Samsung-only prefs: `coverScreenAutoLaunch`, `coverScreenSamsungHomeToggle` |
| `lawnchair/src/app/lawnchair/ui/preferences/destinations/GeneralPreferences.kt` | Settings UI for the two prefs (Samsung-only section) |

### Cover Screen Detection

- `isCoverScreen()` = `Display.DEFAULT_DISPLAY.state == STATE_OFF` (main screen off = phone closed, cover display active)
- `DisplayManager.DisplayListener` tracks cover display (ID 1) state for screen-off/on events
- Cover display ID = `1` (constant `COVER_DISPLAY_ID`)

### Auto-Launch & App Restoration Flow

When `coverScreenAutoLaunch` is enabled:

1. **Cover display turns OFF** → `pendingRestorePackage` is set to the last foreground non-Lawnchair app; cooldown reset
2. **Screen turns back on** — two entry points both check `pendingRestorePackage` first:
   - `ACTION_USER_PRESENT` → `launchIfCoverScreen()`: restores previous app if set, else launches Lawnchair
   - Samsung's `SubHomeActivity` appears → `isCoverHome` branch in `onAccessibilityEvent()`: restores previous app if set, else launches Lawnchair
3. **Shared cooldown** (`lastLaunchTime`, 1s) prevents both `restoreApp()` and `launchLawnchair()` from firing in the same cycle

### Home Key Interception

- On cover screen, Home key is intercepted when `coverScreenAutoLaunch` is on AND `lastForegroundPackage` is not Lawnchair
- Interception launches Lawnchair directly, clears `pendingRestorePackage`
- If `lastForegroundPackage == packageName` (already on Lawnchair) AND `coverScreenSamsungHomeToggle` is on: Home goes to Samsung's native cover home instead

### Package Tracking

- `lastForegroundPackage`: updated on every non-home, non-SystemUI `TYPE_WINDOW_STATE_CHANGED` event
- `recentPackages`: most-recent-first `ArrayDeque` (max 10), used by `RecentsGestureHandler`'s cover screen sheet
- `isCoverHome` classification: Samsung launcher, `SubHomeActivity`, AOD service — but NOT `RecentsActivity`

### Launch Method

- `launchLawnchair()`: targets `LawnchairLauncher` with `FLAG_ACTIVITY_NEW_TASK | REORDER_TO_FRONT | NO_ANIMATION` + `ActivityOptions.launchDisplayId = 1`
- `restoreApp(pkg)`: uses `PackageManager.getLaunchIntentForPackage()` with same flags + display targeting; falls back to `launchLawnchair()` if no launch intent

### Known Constraints

- Samsung blocks `RecentsActivity` launch from `app.lawnchair` package on display 1 — don't attempt it
- `CoverScreenCompat.ensureAllowlisted()` requires `WRITE_SECURE_SETTINGS` (via ADB/Shizuku)
- On non-Z-Flip devices, display 1 doesn't exist — all cover screen logic is inert
