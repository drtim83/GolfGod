# ⛳ GolfGod

> **Intelligent Golf Swing Analysis, Ball Flight Launch Monitor & AI Biomechanics Coach**  
> Designed for **Android (Samsung Galaxy S24 Ultra & modern Android devices)** and **Wear OS (Samsung Galaxy Watch 4/5/6/7)**.

---

## 📖 Overview

**GolfGod** transforms your smartwatch and smartphone into a professional-grade golf swing studio and launch monitor:

- **Wear OS Smartwatch (`app-wear`)**: Real-time inertial measurement unit (IMU) swing detection utilizing high-frequency linear accelerometer and gyroscope sensors. Captures backswing/downswing timing, calculates 3.0:1 tempo ratios, estimates clubhead velocity, and delivers instant haptic feedback to your wrist.
- **Android Smartphone (`app-phone`)**: High-performance Jetpack Compose mobile command center with 240 FPS down-the-line vision coaching, aerodynamic ball flight trajectory radar simulation, 14-club bag distance gapping, and left/right-handedness calibrations.
- **Core Architecture (`core-model`, `core-swing`, `core-physics`)**: Decoupled, modular Kotlin multi-project architecture with full physics and biomechanical modeling.

---

## 📱 Pre-Built Release APK Locations

The release APKs are compiled and ready for sideloading:

| Target Device | APK File Path | Size |
| :--- | :--- | :--- |
| **Android Phone** | [`app-phone/build/outputs/apk/release/app-phone-release.apk`](file:///Users/drtimothytok/VibeCoding/GolfGod/app-phone/build/outputs/apk/release/app-phone-release.apk) | ~10.2 MB |
| **Wear OS Watch** | [`app-wear/build/outputs/apk/release/app-wear-release.apk`](file:///Users/drtimothytok/VibeCoding/GolfGod/app-wear/build/outputs/apk/release/app-wear-release.apk) | ~16.7 MB |

---

## 🚀 How to Install the APKs

### Part 1: Installing the Phone App (`app-phone-release.apk`)

#### Method A: Via USB / ADB (Recommended for Mac / PC)
1. Connect your Android phone to your computer with a USB cable.
2. Ensure **USB Debugging** is enabled on your phone (*Settings > Developer options > USB debugging*).
3. Open Terminal in the project root and run:
   ```bash
   adb install -r app-phone/build/outputs/apk/release/app-phone-release.apk
   ```

#### Method B: Direct File Transfer (No PC Required)
1. Transfer `app-phone-release.apk` to your phone via **Quick Share**, Google Drive, USB drive, or email.
2. Open your phone's file manager (e.g., **My Files** or **Files by Google**).
3. Tap on `app-phone-release.apk`.
4. If prompted with *"Install unknown apps"*, tap **Settings** and toggle **Allow from this source**.
5. Tap **Install** and launch **GolfGod**.

---

### Part 2: Installing the Wear OS App (`app-wear-release.apk`)

To install the app on a **Samsung Galaxy Watch (Galaxy Watch 4, 5, 6, 7, Ultra)** or any Wear OS smartwatch:

#### Step 1: Enable Developer Options & Wireless Debugging on Watch
1. On your Galaxy Watch, go to **Settings** ⚙️.
2. Scroll down and tap **About watch** > **Software info**.
3. Tap **Software version** repeatedly (7 times) until a message displays: *"Developer mode turned on"*.
4. Swipe back to the main **Settings** menu and select the newly unlocked **Developer options**.
5. Turn ON **ADB debugging**.
6. Turn ON **Wireless debugging**.
   > ⚠️ **Important:** Ensure both your computer (or phone) and the watch are connected to the **exact same Wi-Fi network**.

---

#### Step 2: Sideloading via Mac / PC (Wireless ADB)

1. On your watch under **Wireless debugging**, tap **Pair new device**.
   - Note the **Wi-Fi IP address**, **Port**, and **6-digit Wi-Fi pairing code** displayed on your watch screen.
2. On your Mac/PC terminal, pair with the watch:
   ```bash
   adb pair <WATCH_IP>:<PAIRING_PORT>
   # Example: adb pair 192.168.1.55:42135
   ```
   When prompted, enter the 6-digit pairing code shown on your watch.
3. Now look back at the **Wireless debugging** main screen on your watch to find the primary IP address and connection port. Connect:
   ```bash
   adb connect <WATCH_IP>:<CONNECTION_PORT>
   # Example: adb connect 192.168.1.55:38921
   ```
4. Verify the watch is connected:
   ```bash
   adb devices
   ```
   *(You should see your watch IP listed as `device`)*
5. Sideload the Wear OS APK:
   ```bash
   adb install -r app-wear/build/outputs/apk/release/app-wear-release.apk
   ```
6. Once installation completes, open the app drawer on your watch and launch **GolfGod**!

---

#### Step 3 (Alternative): Sideload Directly from Phone (No PC Needed)

You can also sideload the watch app directly from your Android phone:
1. Download **GeminiMan Wear OS Manager** or **Bugjaeger Mobile ADB** from Google Play Store on your phone.
2. Copy `app-wear-release.apk` to your phone's storage.
3. Connect the phone app to your watch via **Wireless Debugging** (entering the watch IP and port).
4. Tap **Install APK / Sideload**, select `app-wear-release.apk`, and tap **Install**.

---

## ⚙️ Initial App Permissions Setup

When opening **GolfGod** on your watch for the first time:
- **Body Sensors / Motion Sensors:** Tap **Allow** so the app can access high-frequency accelerometer and gyroscope data.
- **Keep Screen On:** The app will keep the screen active during practice sessions for instant swing tempo readouts.

---

## 🛠️ Project Structure & Architecture

```text
GolfGod/
├── app-phone/          # Android Phone UI (Jetpack Compose, CameraX 240fps AI Coach, Launch Radar)
├── app-wear/           # Wear OS App (Wear Compose, SensorEventListener, Real-time IMU FSM)
├── core-model/         # Domain Models (SwingRecord, Club, GolferProfile, BallFlightData, CoachFeedback)
├── core-swing/         # Biomechanical algorithms (SwingDetectionStateMachine, TempoEngine, SpeedEstimator)
├── core-physics/       # Aerodynamics engine (BallFlightSimulator, ImpactDynamics, Trajectory equations)
├── gradle/             # Gradle wrapper
├── build.gradle        # Root build configuration
├── settings.gradle     # Multi-module project definitions
└── gradle.properties   # JVM and Android build configurations
```

---

## 🏗️ Building from Source

To build debug or release APKs from source:

```bash
# Clone the repository
git clone https://github.com/drtim83/GolfGod.git
cd GolfGod

# Verify build with a dry run
./gradlew assembleRelease --dry-run

# Build both Phone and Wear OS Release APKs
./gradlew assembleRelease

# Or build debug APKs
./gradlew assembleDebug
```

> **JDK Note:** This project is preconfigured to automatically utilize Android Studio's bundled OpenJDK runtime on macOS if `JAVA_HOME` is not set.

---

## 📜 License

Created for high-performance golf swing analysis and training. All rights reserved.
