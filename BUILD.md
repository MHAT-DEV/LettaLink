# Build & Compilation Instructions

## Prerequisites
- Android Studio Ladybug / Meerkat or Gradle 8.x+
- JDK 11 or higher
- Android SDK 36 (compileSdk 36, minSdk 24)

## Compilation Steps
1. Sync project Gradle dependencies:
   ```bash
   gradle compileDebugSources
   ```
2. Build debug APK:
   ```bash
   gradle assembleDebug
   ```
3. Run unit tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

## Server Setup for Testing
To connect with a local Letta instance:
1. Start Letta on your host machine:
   ```bash
   letta run
   ```
   (Server will run on `http://localhost:8283`)
2. In the Android Emulator, use `http://10.0.2.2:8283` as the Server Base URL.
3. On a physical Android device on the same Wi-Fi, use your machine's LAN IP (e.g. `http://192.168.1.50:8283`).
