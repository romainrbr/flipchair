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
