# Quick Start Guide

Get started with USB Camera Viewer in 5 minutes!

## Installation

### Option 1: ADB Install (Recommended)
```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

### Option 2: Manual Install
1. Copy APK to your Android device
2. Open the file
3. Allow installation from unknown sources
4. Install

## First Use

### 1. Launch App
Open "USB Camera Viewer" from your app drawer

### 2. Grant Permissions
- **Camera permission** - Tap "Allow" when prompted
- **USB permission** - Tap "OK" when camera is connected

### 3. Connect Camera
- Connect USB camera via OTG adapter
- Camera will be detected automatically
- Live view starts immediately

## Basic Usage

### Open Settings
Tap the **⚙** (gear) icon in the top-right corner

### Change Resolution
1. Open settings sidebar
2. Tap "Resolution" dropdown
3. Select desired resolution (4K, 1080p, 720p, VGA)
4. Changes apply automatically

### Rotate Image
1. Open settings sidebar
2. Tap "Rotation" dropdown
3. Select angle (0°, 90°, 180°, 270°)
4. Image rotates immediately

### Flip Image
1. Open settings sidebar
2. Check "Flip Horizontal" or "Flip Vertical"
3. Image flips immediately

### Camera Controls
1. Open settings sidebar
2. Tap "Camera Controls" (red button)
3. Adjust brightness, contrast, saturation
4. Toggle auto-focus and white balance
5. Changes apply in real-time

### Enable Auto-Launch
1. Open settings sidebar
2. Check "Launch app when camera connected"
3. App will open automatically when camera is plugged in

## Tips

- **Settings are saved** - Your configuration persists across restarts
- **Auto-retry** - App reconnects automatically if camera disconnects
- **Hotplug support** - Connect/disconnect camera anytime
- **Multiple cameras** - Switch between cameras in settings

## Troubleshooting

### Camera Not Detected
- Check USB connection
- Try different USB port
- Verify camera is UVC-compliant
- Grant USB permission

### Black Screen
- Wait 15 seconds for auto-retry
- Try different resolution
- Check camera power supply

### App Crashes
- Check Android version (7.0+ required)
- Grant all permissions
- Clear app data and retry

## Next Steps

- Read the [User Manual](USER_MANUAL.md) for detailed features
- Check [Troubleshooting](TROUBLESHOOTING.md) for common issues
- Explore camera controls for best image quality

---

**Need help?** Check the [FAQ](../reference/FAQ.md) or [Troubleshooting Guide](TROUBLESHOOTING.md)
