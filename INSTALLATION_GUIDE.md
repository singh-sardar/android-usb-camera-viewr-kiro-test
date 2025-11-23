# Installation Guide - v1.2

## Current APK Files

**Debug APK** (Recommended for testing):
- **Location**: `app/build/outputs/apk/debug/app-debug.apk`
- **Size**: ~13 MB
- **Package**: com.example.usbcameraviewer.debug
- **Better error messages for debugging**

**Release APK**:
- **Location**: `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Size**: ~9.7 MB
- **Package**: com.example.usbcameraviewer

## Installation Methods

### Method 1: ADB (Most Reliable)

```bash
# 1. Uninstall ALL previous versions
adb uninstall com.example.webviewcamberviewr
adb uninstall com.example.webviewcamberviewr.debug
adb uninstall com.example.usbcameraviewer
adb uninstall com.example.usbcameraviewer.debug

# 2. Install debug version (recommended for testing)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Launch the app
adb shell am start -n com.example.usbcameraviewer.debug/.MainActivity

# 4. Watch for errors
adb logcat | grep -E "AndroidRuntime|UsbCamera|MainActivity"
```

### Method 2: Manual Installation

1. **Copy APK to phone**
   - Use USB cable
   - Or use cloud storage (Google Drive, Dropbox)
   - Or use file sharing app

2. **Enable Unknown Sources**
   - Settings → Security → Unknown sources → Enable
   - Or Settings → Additional Settings → Privacy → Install via USB

3. **Install APK**
   - Open File Manager
   - Navigate to APK file
   - Tap to install
   - Grant permissions

### Method 3: Android Studio

1. Open project in Android Studio
2. Connect device
3. Click Run (green play button)
4. Select your device
5. Wait for installation

## Troubleshooting Installation

### "App not installed" Error

**Cause 1: Old version conflict**
```bash
# Solution: Uninstall all old versions
adb shell pm list packages | grep camera
adb uninstall <package_name_from_above>
```

**Cause 2: Corrupted APK**
```bash
# Solution: Rebuild
./gradlew clean
./gradlew assembleDebug
```

**Cause 3: Insufficient storage**
- Free up at least 50MB space
- Check: Settings → Storage

**Cause 4: MIUI Security (Xiaomi)**
- Settings → Additional Settings → Privacy
- Enable "Install via USB"
- Disable "MIUI optimization" (Developer options)

### App Crashes on Launch

**Check crash logs:**
```bash
adb logcat | grep -E "AndroidRuntime|FATAL"
```

**Common causes:**

1. **Missing resources**
   - Rebuild: `./gradlew clean assembleDebug`

2. **Permission issues**
   - Grant camera permission manually
   - Settings → Apps → USB Camera Viewer → Permissions

3. **Incompatible Android version**
   - Check: Settings → About phone → Android version
   - Must be 5.0 or higher

4. **Corrupted installation**
   - Uninstall completely
   - Reboot device
   - Reinstall

### Emulator-Specific Issues

**Emulator crashes:**
1. Use x86_64 emulator (not ARM)
2. Enable hardware acceleration
3. Allocate more RAM (2GB+)
4. Use API 28 or higher

**Create compatible emulator:**
```
AVD Manager → Create Virtual Device
- Device: Pixel 4
- System Image: API 30 (Android 11) x86_64
- RAM: 2048 MB
- Graphics: Hardware
```

## Verification

### Check if installed:
```bash
adb shell pm list packages | grep usbcamera
```

Should show:
```
package:com.example.usbcameraviewer.debug
```

### Check app info:
```bash
adb shell dumpsys package com.example.usbcameraviewer.debug | grep -E "versionName|versionCode"
```

Should show:
```
versionCode=3
versionName=1.2-debug
```

### Launch and check logs:
```bash
# Launch
adb shell am start -n com.example.usbcameraviewer.debug/.MainActivity

# Watch logs
adb logcat -c  # Clear logs
adb logcat | grep -E "UsbCamera|MainActivity|AndroidRuntime"
```

## Testing Checklist

After installation:

- [ ] App launches without crashing
- [ ] Main screen appears
- [ ] Can open settings sidebar (⚙ icon)
- [ ] Can see all configuration options
- [ ] No error toasts appear
- [ ] Status shows "Camera disconnected" or similar

## Device-Specific Instructions

### Xiaomi (MIUI)
1. Settings → Additional Settings → Privacy
2. Enable "Install via USB"
3. Settings → Additional Settings → Developer options
4. Disable "MIUI optimization"
5. Reboot
6. Install APK

### Samsung (One UI)
1. Settings → Biometrics and security
2. Enable "Install unknown apps"
3. Select your file manager
4. Enable installation
5. Install APK

### Huawei (EMUI)
1. Settings → Security
2. Enable "Install apps from external sources"
3. Install APK

### Stock Android
1. Settings → Security
2. Enable "Unknown sources"
3. Install APK

## Getting Help

### Collect Debug Information

```bash
# 1. Device info
adb shell getprop ro.build.version.release  # Android version
adb shell getprop ro.product.model  # Device model

# 2. Installation attempt
adb install -r app/build/outputs/apk/debug/app-debug.apk 2>&1 | tee install.log

# 3. Crash logs (if crashes)
adb logcat -d > crash.log

# 4. Package info
adb shell pm list packages | grep camera
```

### Report Issue

Include:
1. Device model and Android version
2. Installation method used
3. Exact error message
4. install.log (if available)
5. crash.log (if app crashes)
6. Screenshots of error

## Quick Commands Reference

```bash
# Uninstall all versions
adb uninstall com.example.webviewcamberviewr
adb uninstall com.example.usbcameraviewer
adb uninstall com.example.usbcameraviewer.debug

# Install debug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.example.usbcameraviewer.debug/.MainActivity

# Check logs
adb logcat | grep -E "UsbCamera|AndroidRuntime"

# Force stop
adb shell am force-stop com.example.usbcameraviewer.debug

# Clear data
adb shell pm clear com.example.usbcameraviewer.debug
```

## Success Indicators

✅ **Installation successful if:**
- No error during `adb install`
- App appears in app drawer
- App launches without crashing
- Main screen with camera view appears

✅ **App working if:**
- Settings sidebar opens
- All controls visible
- No crash toasts
- Can change settings
- Status indicator shows

---

**Need more help?** Check crash logs and report the issue with device details.

**Last Updated**: November 23, 2025
