# Quick Setup Guide

## For Users

### Installation
1. Download `app-debug.apk` from `app/build/outputs/apk/debug/`
2. Transfer to your Android device
3. Enable "Install from Unknown Sources" in Settings
4. Install the APK
5. Connect USB camera and grant permission

### First Use
1. App opens automatically when USB camera is connected
2. Tap ⚙️ button to access settings
3. Use "Auto Best Config" for optimal settings
4. Adjust as needed

## For Developers

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK 21+ (Android 5.0+)

### Build Instructions

```bash
# Clone repository
git clone <repo-url>
cd WebViewCamberViewr

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to device
./gradlew installDebug

# Run tests
./gradlew test
```

### Project Configuration

**Minimum SDK**: 21 (Android 5.0)  
**Target SDK**: 33 (Android 13)  
**Compile SDK**: 34  

**Key Dependencies**:
- AndroidUSBCamera (AUSBC) 3.2.7
- Kotlin Coroutines 1.7.3
- AndroidX Core KTX 1.12.0

### Architecture

```
UsbCameraActivity (Host)
    └── UsbCameraFragment (Camera + UI)
            ├── CameraClient (AUSBC)
            ├── CameraUvcStrategy (USB)
            └── SettingsManager (Persistence)
```

### Adding Features

1. **New Camera Control**:
   - Add SeekBar in `createSidebar()`
   - Handle in `applyCameraControl()`
   - Use `CameraUvcStrategy` methods

2. **New Setting**:
   - Add field to `CameraConfig`
   - Update `SettingsManager` save/load
   - Add UI control in sidebar

3. **Custom Resolution**:
   - Add to resolution list in `addSpinnerControl()`
   - Ensure camera supports it

### Debugging

- Use in-app log viewer (⚙️ → View Logs)
- Check Logcat: `adb logcat | grep UsbCamera`
- Enable debug mode in `getCameraClient()`: `.openDebug(true)`

### Common Issues

**Build fails**: Clean project (`./gradlew clean`)  
**Camera not working**: Check USB OTG support  
**Black screen**: Try lower resolution  

### Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

### Code Style

- Kotlin official style guide
- Document public APIs
- Use meaningful variable names
- Keep functions small and focused

### Testing

Test with various cameras:
- Different resolutions
- Different frame rates
- Different USB cables
- Different Android versions

### Release Checklist

- [ ] Update version in `build.gradle.kts`
- [ ] Test on multiple devices
- [ ] Update README.md
- [ ] Build release APK
- [ ] Sign APK
- [ ] Test signed APK
- [ ] Create release notes
