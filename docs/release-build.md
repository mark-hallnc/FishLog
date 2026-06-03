# Release Build Instructions

This document describes how to generate the artifacts required for a Google Play Console release.

## Prerequisites

- Android Studio or Gradle.
- A valid signing keystore (not tracked in Git).
- `key.properties` configured with keystore paths and passwords (not tracked in Git).

## Generating the App Bundle (AAB)

Run the following command:

```bash
./gradlew clean bundleRelease
```

## Release Artifacts

After a successful build, the following artifacts are generated:

### 1. App Bundle (AAB)
**Path:** `app/build/outputs/bundle/release/app-release.aab`
**Action:** Upload this to the Google Play Console release.

### 2. Deobfuscation Mapping File
**Path:** `app/build/outputs/mapping/release/mapping.txt`
**Note:** This file is only generated if `isMinifyEnabled = true` in `build.gradle.kts`.
**Action:** If generated, upload this to the Play Console to symbolicate stack traces. If minification is disabled, this warning can be ignored.

### 3. Native Debug Symbols
**Path:** `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip`
**Note:** This is generated because `ndk.debugSymbolLevel = "SYMBOL_TABLE"` is configured.
**Action:** Upload this to the Play Console to symbolicate native crash reports.

## Security Warning

**NEVER** commit your `.jks` keystore files, `key.properties`, or passwords to the Git repository. These are excluded by `.gitignore`.
