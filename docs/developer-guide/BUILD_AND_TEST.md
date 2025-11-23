# Build and Test Guide

## Quick Build

### Using Android Studio (Recommended)

1. **Open Project**
   ```
   File → Open → Select project folder
   ```

2. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```
   This will download UVCCamera library from JitPack

3. **Connect Device**
   - Enable USB debugging on Android device
   - Connect via USB cable
   - Accept debugging authorization

4. **Build and Run**
   ```
   Run → Run 'app'
   ```
   Or click the green play button

### Using Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build and install in one command
./gradlew installDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

### Output Location

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

## Testing Procedure

### Pre-Test Checklist

- [ ] Android device with USB Host support
- [ ] Android 7.0 (API 24) or higher
- [ ] USB OTG adapter (if needed)
- [ ] UVC-compliant USB camera
- [ ] USB debugging enabled
- [ ] Adequate power supply for camera

### Test 1: App Installation

```bash
# Install app
adb install app/build/outputs/apk/debug/app-debug.apk

# Or use Gradle
./gradlew installDebug

# Verify installation
adb shell pm list packages | grep webviewcamberviewr
```

**Expected**: App installs without errors

### Test 2: App Launch

```bash
# Launch app
adb shell am start -n com.example.webviewcamberviewr/.MainActivity

# Monitor logs
adb logcat | grep -E "UsbCamera|MainActivity"
```

**Expected**: 
- App launches successfully
- Camera permission dialog appears
- No crashes in logcat

### Test 3: Permission Grant

1. Grant camera permission when prompted
2. Check logcat for permission status

```bash
adb logcat | grep "permission"
```

**Expected**: Permission granted, no errors

### Test 4: USB Camera Connection

1. Connect USB camera to device
2. Grant USB permission when prompted
3. Observe logcat

```bash
adb logcat | grep -E "USB|UVC"
```

**Expected**:
```
D/UsbCamera: USB device attached: /dev/bus/usb/001/002
D/UsbCamera: USB device connected: /dev/bus/usb/001/002
D/UsbCamera: Started streaming 1920x1080
```

### Test 5: Video Display

**Visual Check**:
- [ ] Video displays full screen
- [ ] Video is smooth (no stuttering)
- [ ] Video quality is good
- [ ] No black screen
- [ ] No frozen frames

**Performance Check**:
```bash
# Monitor CPU usage
adb shell top | grep webviewcamberviewr

# Monitor memory
adb shell dumpsys meminfo com.example.webviewcamberviewr
```

### Test 6: Configuration Panel

1. Tap gear icon (⚙) in top-right
2. Verify sidebar opens
3. Check all controls present:
   - [ ] Device spinner
   - [ ] Resolution spinner
   - [ ] FPS spinner
   - [ ] Rotation spinner
   - [ ] Flip horizontal checkbox
   - [ ] Flip vertical checkbox
   - [ ] Apply button
   - [ ] Status text

### Test 7: Resolution Changes

Test each resolution:
- [ ] 4K (3840x2160) - if supported
- [ ] 1080p (1920x1080)
- [ ] 720p (1280x720)
- [ ] VGA (640x480)

**Expected**: Video restarts with new resolution

### Test 8: FPS Changes

Test each frame rate:
- [ ] 60 FPS - if supported
- [ ] 30 FPS
- [ ] 24 FPS
- [ ] 15 FPS

**Expected**: Frame rate changes (may not be visually obvious)

### Test 9: Rotation

Test each rotation:
- [ ] 0° - Normal
- [ ] 90° - Rotated right
- [ ] 180° - Upside down
- [ ] 270° - Rotated left

**Expected**: Video rotates immediately

### Test 10: Flip

Test flip options:
- [ ] Flip horizontal - Mirror left-right
- [ ] Flip vertical - Mirror top-bottom
- [ ] Both flips - Mirror both ways
- [ ] No flip - Normal

**Expected**: Video flips immediately

### Test 11: USB Hotplug

1. Disconnect USB camera
2. Wait for error message
3. Reconnect camera
4. Wait 15 seconds max

**Expected**:
- Disconnect detected
- Error message shown
- Auto-retry starts
- Camera reconnects automatically

### Test 12: Multiple Cameras

If you have multiple cameras:
1. Connect first camera
2. Connect second camera
3. Open configuration panel
4. Check device list
5. Switch between cameras

**Expected**: Both cameras listed, can switch

### Test 13: App Lifecycle

Test app behavior:
- [ ] Minimize app → Resume
- [ ] Lock screen → Unlock
- [ ] Switch to another app → Return
- [ ] Rotate device (if not locked)

**Expected**: Camera continues working, no crashes

### Test 14: Long-Running Stability

Run app for extended period:
- [ ] 5 minutes
- [ ] 30 minutes
- [ ] 1 hour

Monitor:
```bash
# Watch for crashes
adb logcat | grep -E "FATAL|AndroidRuntime"

# Monitor memory leaks
adb shell dumpsys meminfo com.example.webviewcamberviewr
```

**Expected**: No crashes, stable memory usage

### Test 15: Error Recovery

Test error scenarios:
- [ ] Start app without camera
- [ ] Disconnect camera while streaming
- [ ] Deny USB permission
- [ ] Revoke camera permission
- [ ] Low power (camera shuts off)

**Expected**: Graceful error handling, auto-retry works

## Performance Benchmarks

### Target Performance

- **CPU Usage**: < 30% on mid-range device
- **Memory Usage**: < 200MB
- **Frame Rate**: Smooth 30fps minimum
- **Latency**: < 100ms from camera to display
- **Battery**: < 10% per hour

### Measuring Performance

```bash
# CPU usage
adb shell top -n 1 | grep webviewcamberviewr

# Memory usage
adb shell dumpsys meminfo com.example.webviewcamberviewr | grep TOTAL

# Frame rate (requires Android 10+)
adb shell dumpsys gfxinfo com.example.webviewcamberviewr

# Battery usage
adb shell dumpsys batterystats | grep webviewcamberviewr
```

## Debugging

### Enable Verbose Logging

```bash
# All logs
adb logcat

# Filter for app
adb logcat | grep webviewcamberviewr

# Filter for USB/Camera
adb logcat | grep -E "UsbCamera|UVCCamera|USBMonitor"

# Save to file
adb logcat > logcat.txt
```

### Common Issues and Solutions

#### Issue: Camera Not Detected

**Debug**:
```bash
# Check USB devices
adb shell ls -l /dev/bus/usb/

# Check USB permissions
adb shell dumpsys usb
```

**Solutions**:
- Verify USB Host support
- Check USB cable/adapter
- Try different USB port
- Verify camera is UVC-compliant

#### Issue: Black Screen

**Debug**:
```bash
# Check for errors
adb logcat | grep -E "ERROR|Exception"

# Check camera state
adb logcat | grep "CameraState"
```

**Solutions**:
- Wait 15 seconds for retry
- Try different resolution
- Check camera power supply
- Verify MJPEG support

#### Issue: App Crashes

**Debug**:
```bash
# Get crash log
adb logcat | grep -E "FATAL|AndroidRuntime"

# Get stack trace
adb logcat *:E
```

**Solutions**:
- Check Android version
- Verify all permissions
- Clear app data
- Reinstall app

#### Issue: Poor Performance

**Debug**:
```bash
# Profile CPU
adb shell simpleperf record -p $(adb shell pidof com.example.webviewcamberviewr)

# Check frame drops
adb shell dumpsys gfxinfo com.example.webviewcamberviewr
```

**Solutions**:
- Lower resolution
- Lower frame rate
- Close other apps
- Check device specs

## Automated Testing

### Unit Tests

```bash
# Run unit tests
./gradlew test

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

### Instrumented Tests

```bash
# Run on connected device
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.webviewcamberviewr.MainActivityTest
```

## Release Build

### Signing Configuration

Create `keystore.properties`:
```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=your_key_alias
storeFile=path/to/keystore.jks
```

### Build Release APK

```bash
# Build signed release
./gradlew assembleRelease

# Output location
ls -lh app/build/outputs/apk/release/
```

### Verify APK

```bash
# Check APK info
aapt dump badging app/build/outputs/apk/release/app-release.apk

# Verify signature
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

## Test Report Template

```
Test Date: ___________
Tester: ___________
Device: ___________
Android Version: ___________
Camera Model: ___________

[ ] App Installation
[ ] App Launch
[ ] Permission Grant
[ ] USB Camera Connection
[ ] Video Display
[ ] Configuration Panel
[ ] Resolution Changes
[ ] FPS Changes
[ ] Rotation
[ ] Flip
[ ] USB Hotplug
[ ] Multiple Cameras
[ ] App Lifecycle
[ ] Long-Running Stability
[ ] Error Recovery

Performance:
- CPU Usage: ____%
- Memory Usage: ____MB
- Frame Rate: ____fps
- Issues Found: ___________

Notes:
_________________________________
_________________________________
_________________________________
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    - name: Run tests
      run: ./gradlew test
    - name: Upload APK
      uses: actions/upload-artifact@v2
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## Success Criteria

### Minimum Requirements
- ✅ App builds without errors
- ✅ App installs on device
- ✅ Camera detected and connects
- ✅ Video displays smoothly
- ✅ All configuration options work
- ✅ No crashes during normal use

### Optimal Performance
- ✅ < 30% CPU usage
- ✅ < 200MB memory usage
- ✅ Smooth 30fps video
- ✅ < 100ms latency
- ✅ Stable for 1+ hour
- ✅ Works with multiple cameras

## Next Steps After Testing

1. **Document Issues** - Create issue list with details
2. **Fix Critical Bugs** - Address crashes and major issues
3. **Optimize Performance** - Improve CPU/memory usage
4. **Add Features** - Implement recording, zoom, etc.
5. **User Testing** - Get feedback from real users
6. **Release** - Publish to Play Store or distribute APK

---

**Ready to test?** Start with Test 1 and work through the checklist!
