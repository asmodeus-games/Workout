# CyberWorkout

A modern, native Android application for high-intensity interval/calisthenics tracking. Built with Kotlin, Jetpack Compose, and Clean Architecture.

Features a "Cyber-Dark" aesthetic, a smart state machine, screen wake-locks, and haptic feedback.

## Features

- **Smart State Machine:** Idle (Daily routine), Active (Workout & reps), Rest (Countdown).
- **Session Persistence:** Resumes exactly where you left off if closed or crashed.
- **Haptic Engine:** Vibrates on set completion and rest timer end.
- **Gesture Control:** Swipe left to skip a set.

## Pre-built APK

You can find the pre-built APK file to install on your phone here:
[app/build/outputs/apk/debug/app-debug.apk](app/build/outputs/apk/debug/app-debug.apk)

### How to Install the APK
1. Download `app-debug.apk` to your Android device.
2. Open the downloaded file using a file manager on your phone.
3. If prompted, allow "Install from unknown sources."
4. Click "Install" and enjoy the app!

## How to Build the Project Yourself

To build the APK from source using the command line:

1. Ensure you have Java 17+ installed.
2. Open a terminal in the root directory of this repository.
3. Run the following command:

   ```bash
   ./gradlew assembleDebug
   ```

4. The built APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`.
