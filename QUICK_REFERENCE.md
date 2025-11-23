# Quick Reference Card

## For Users

### Controls
- **⚙️ Button** - Open/close settings sidebar
- **Resolution** - 640x480 to 4K (3840x2160)
- **FPS** - 15, 24, 30, 60 frames per second
- **Rotation** - 0°, 90°, 180°, 270°
- **Flip H/V** - Horizontal and vertical flip
- **Brightness** - -100 to +100
- **Contrast** - -100 to +100
- **Saturation** - -100 to +100
- **Auto Best** - Apply 720p@30fps optimal settings
- **View Logs** - Open in-app log viewer

### Shortcuts
- Tap anywhere on video to hide/show status bar
- Settings auto-save when changed
- App auto-launches when USB camera connected

### Troubleshooting
| Problem | Solution |
|---------|----------|
| Black screen | Try Auto Best Config or lower resolution |
| No camera detected | Check USB OTG support, try different cable |
| Permission denied | Reconnect camera and grant permission |
| Poor performance | Lower resolution to 720p, set FPS to 30 |
| App crashes | Clear app data, reinstall |

---

## For Developers

### File Structure
```
UsbCameraActivity.kt      - Main activity (50 lines)
UsbCameraFragment         - Camera UI (300 lines)
SettingsManager.kt        - Persistence (80 lines)
AppLogger.kt              - Logging (60 lines)
LogViewerActivity.kt      - Log viewer (100 lines)
```

### Key Classes
```kotlin
// Main fragment
class UsbCameraFragment : CameraFragment()

// Settings
data class CameraConfig(...)
class SettingsManager(context)

// Logging
object AppLogger
data class LogEntry(...)
```

### Common Tasks

#### Add Camera Control
```kotlin
// 1. Add UI
val controlSeek = createSeekBar("Control", -100, 100)

// 2. Handle change
when (control) {
    "Control" -> strategy.setControl(value)
}

// 3. Save (optional)
data class CameraConfig(
    val control: Int = 0
)
```

#### Add Resolution
```kotlin
addSpinnerControl("Resolution", listOf(
    "640x480",
    "1280x720",
    "1920x1080",
    "2560x1440",  // Add here
    "3840x2160"
))
```

#### Debug Logging
```kotlin
AppLogger.d("Tag", "Debug message")
AppLogger.i("Tag", "Info message")
AppLogger.w("Tag", "Warning message")
AppLogger.e("Tag", "Error message", exception)
```

#### Access Camera Strategy
```kotlin
val strategy = getCurrentCameraStrategy() as? CameraUvcStrategy
strategy?.setBrightness(50)
strategy?.setContrast(0)
```

### Build Commands
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Install
./gradlew installDebug

# Clean
./gradlew clean
```

### Debug Commands
```bash
# View logs
adb logcat | grep UsbCamera

# View errors only
adb logcat *:E

# Memory info
adb shell dumpsys meminfo com.example.usbcameraviewer

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### AUSBC Library Methods

#### Camera Operations
```kotlin
updateResolution(width, height)
setRotateType(RotateType.ANGLE_90)
getAllPreviewSizes()
isCameraOpened()
```

#### Camera Controls
```kotlin
strategy.setBrightness(value)    // -100 to 100
strategy.setContrast(value)      // -100 to 100
strategy.setSaturation(value)    // -100 to 100
strategy.setHue(value)           // -100 to 100
strategy.setSharpness(value)     // -100 to 100
strategy.setZoom(value)          // 0 to 100
strategy.setAutoFocus(enabled)   // true/false
strategy.setAutoWhiteBalance(enabled)
```

#### Capture Operations
```kotlin
captureImage(callback, path)
captureVideoStart(callback, path, duration)
captureVideoStop()
```

### Settings Manager
```kotlin
// Save
val config = CameraConfig(width = 1920, height = 1080)
settingsManager.saveConfig(config)

// Load
val config = settingsManager.loadConfig()

// Auto-launch
settingsManager.setAutoLaunch(true)
val enabled = settingsManager.isAutoLaunchEnabled()
```

### Common Patterns

#### Safe Camera Access
```kotlin
val strategy = getCurrentCameraStrategy() as? CameraUvcStrategy
strategy?.let {
    it.setBrightness(value)
} ?: run {
    AppLogger.w("Camera", "Strategy not available")
}
```

#### UI Updates
```kotlin
activity?.runOnUiThread {
    statusText.text = "Updated"
    Toast.makeText(requireContext(), "Message", Toast.LENGTH_SHORT).show()
}
```

#### Error Handling
```kotlin
try {
    // Operation
} catch (e: Exception) {
    AppLogger.e("Tag", "Error", e)
    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
}
```

### Testing Checklist
- [ ] App launches
- [ ] Camera detected
- [ ] Video displays
- [ ] All resolutions work
- [ ] All controls work
- [ ] Settings persist
- [ ] Rotation works
- [ ] Flip works
- [ ] Logs accessible
- [ ] No crashes

### Performance Targets
- **APK Size**: < 10MB
- **Memory**: < 100MB
- **CPU**: < 15%
- **Startup**: < 2 seconds
- **Frame Rate**: 30 fps stable

### Version Info
- **Current**: 1.2
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 33 (Android 13)
- **AUSBC**: 3.2.7

---

## Quick Links

- [Full Documentation](README.md)
- [App Flow](APP_FLOW.md)
- [Developer Guide](DEVELOPER.md)
- [Setup Instructions](SETUP.md)
- [Changelog](CHANGELOG.md)

---

**Print this page for quick reference while developing!**
