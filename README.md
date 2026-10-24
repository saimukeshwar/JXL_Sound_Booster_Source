# JXL Sound Booster (prototype)

Ad-free Android app prototype intended for a JXL 10-inch Android head unit running Android 10.

## What it does
- Tries to attach Android audio effects to audio session 0 (global output mix).
- Provides a master gain control, bass control and equalizer controls.
- Reports when the firmware rejects global effects rather than claiming that system-wide processing works.

## Important compatibility limitation
Android does **not** guarantee that third-party apps can apply effects to every audio source. Some Android 10 OEM builds permit global output-mix effects; many block them or ignore them. FM radio may be implemented by a vendor app or separate audio path and may bypass standard Android effects. Bluetooth and other apps can also behave differently.

This prototype is a best-effort compatibility test, not a guarantee of system-wide enhancement. It does not root the stereo, modify firmware, or bypass vendor security restrictions. If global effects are blocked, a system/privileged app signed by the head-unit vendor or a firmware-supported audio-effects interface may be required. Do not root or flash firmware just to use this app.

## Build APK
1. Install Android Studio on a Windows PC.
2. Install Android SDK Platform 35 and a compatible JDK (Android Studio's bundled JDK is fine).
3. Open this folder in Android Studio.
4. Allow Gradle sync to finish.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.
7. Copy it to a USB drive or use ADB to install it on the JXL stereo. Enable installation from unknown sources only if you trust the APK you built.

For command line, with Android SDK and Gradle configured:
`gradle assembleDebug`

## Test on the JXL unit
1. Install and open the app.
2. Read the status message. If it says global effects are blocked, the app cannot provide universal system-wide enhancement on that firmware.
3. Test low-volume music from a safe level and separately test FM, Bluetooth and another app.
4. If one source is unaffected, that source likely bypasses the Android global effects path.

## Safety
- Keep volume moderate.
- If you hear distortion, reduce gain immediately.
- Software cannot create more physical amplifier power. Persistent low output from installation day may indicate wiring, speaker compatibility, or a low-output head unit.
- Do not change hidden factory settings or connect speaker wires to the vehicle chassis.
