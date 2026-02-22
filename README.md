# Lawnchair — Galaxy Z Flip Cover Screen Fork

This is a fully vibe-coded fork of [Lawnchair](https://github.com/LawnchairLauncher/lawnchair), aimed at making Lawnchair usable as a full launcher on the **Samsung Galaxy Z Flip** cover screen.

Only tested on the **Galaxy Z Flip 7** (Android 16). Your mileage may vary on other Flip models.


---

## Demo


https://github.com/user-attachments/assets/22f4053b-9b73-446b-96e6-fd82c87a84ce



---

## Prerequisites

### 1. Grant accessibility permission
The cover screen integration relies on an `AccessibilityService`. After installing, go to:

**Settings → Accessibility → Installed apps → Lawnchair → Use Lawnchair**

Enable the toggle. Without this, auto-launch and home button interception will not work.

### 2. Add apps to a Multistar Launcher
Samsung restricts which apps can run on the cover screen. Any app you want to use within Lawnchair on the cover screen must first be added to a **Multistar** launcher entry:

**Settings → Advanced features → Multi Star → Cover screen apps**

Add every app you want accessible from Lawnchair on the cover screen. Apps not added here will not open (or will open on the main screen instead).

---

## Features

### Auto-launch on cover screen
When the cover screen is unlocked, Lawnchair is automatically brought to the foreground instead of Samsung's default cover screen launcher. Unlock → straight into your launcher.

### Home button returns to Samsung menu
Pressing home from the Lawnchair home screen switches to Samsung's native cover screen menu. This lets you toggle between the two launchers: home from Lawnchair goes to Samsung's menu, and any subsequent navigation back to an app context will bring Lawnchair back up.

---

## Settings

Both features are toggleable under **Settings → General**, visible only on Samsung devices:

- **Auto-launch on cover screen** — enabled/disabled independently
- **Home button returns to Samsung menu** — enabled/disabled independently

---

## How it works

A background `AccessibilityService` (`LawnchairAccessibilityService`) monitors window state changes on the cover screen display. When the cover screen is unlocked (`ACTION_USER_PRESENT`), or when Samsung's cover home (`SubHomeActivity`) appears after leaving a non-Lawnchair context, the service launches Lawnchair. If the user intentionally pressed home from Lawnchair, the service steps aside and lets Samsung's home screen stay.

---

## Building

```bash
GRADLE_OPTS="-Xmx16g" ./gradlew assembleLawnWithQuickstepGithub \
  -Pkotlin.daemon.jvmargs="-Xmx16g" \
  -Dorg.gradle.workers.max=12
```

Output APKs land in `build/outputs/apk/lawnWithQuickstepGithub/`.

## Installing

```bash
adb install -r "$(find build/outputs/apk/lawnWithQuickstepGithub/release -name '*.apk' | head -1)"
```

---

## Disclaimer

This fork is totally experimental. It makes no attempt to be upstreamed or broadly maintained. The cover screen integration is built on top of accessibility service hacks and Samsung-specific internals (`com.android.systemui.subscreen.SubHomeActivity`) that may break on future One UI updates.
