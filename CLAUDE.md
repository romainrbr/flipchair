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

## Branch

Active feature branch: `lawnchair-accessibility` (base: `16-dev`)
