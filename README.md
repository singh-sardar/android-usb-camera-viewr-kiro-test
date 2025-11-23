# USB Camera Viewer for Android

A simple, efficient Android app for viewing USB cameras with real-time controls.

## Features

- ✅ **Live USB Camera Preview** - Real-time video streaming from UVC cameras
- ✅ **Multiple Resolutions** - Support from 640x480 to 4K (3840x2160)
- ✅ **FPS Control** - 15, 24, 30, 60 fps options
- ✅ **Image Rotation** - 0°, 90°, 180°, 270° rotation
- ✅ **Flip Controls** - Horizontal and vertical flip
- ✅ **Camera Adjustments** - Brightness, contrast, saturation
- ✅ **Auto Configuration** - One-tap best settings (720p@30fps)
- ✅ **Settings Persistence** - Remembers your preferences
- ✅ **Auto-Launch** - Opens automatically when USB camera is connected
- ✅ **In-App Logs** - Built-in log viewer for debugging

## Requirements

- Android 5.0 (API 21) or higher
- USB OTG support
- UVC-compatible USB camera

## Installation

1. Download the APK from `app/build/outputs/apk/debug/app-debug.apk`
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK
4. Connect your USB camera
5. Grant USB permission when prompted

## Usage

### Basic Operation

1. **Launch the app** - Opens automatically when USB camera is connected
2. **View live feed** - Camera preview appears full-screen
3. **Access settings** - Tap the ⚙️ button in the top-right corner

### Sidebar Controls

- **Resolution** - Select from 640x480 to 4K
- **FPS** - Choose frame rate (15-60 fps)
- **Rotation** - Rotate image 0°, 90°, 180°, or 270°
- **Flip** - Toggle horizontal/vertical flip
- **Brightness** - Adjust camera brightness (-100 to +100)
- **Contrast** - Adjust camera contrast (-100 to +100)
- **Saturation** - Adjust camera saturation (-100 to +100)
- **Auto Best Config** - Apply optimal settings (720p@30fps)
- **View Logs** - Open in-app log viewer

## Technical Details

### Architecture

- **Library**: AndroidUSBCamera (AUSBC) v3.2.7
- **Native Support**: libuvc with isochronous transfer
- **Rendering**: OpenGL ES 2.0 hardware acceleration
- **Language**: Kotlin
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 33 (Android 13)

### Key Components

- `UsbCameraActivity` - Main activity with camera fragment
- `UsbCameraFragment` - Camera preview and controls
- `SettingsManager` - Persistent configuration storage
- `AppLogger` - Logging system
- `LogViewerActivity` - In-app log viewer

### Supported Cameras

Any UVC (USB Video Class) compliant camera:
- Webcams
- USB endoscopes
- USB microscopes
- Action cameras with USB output
- DSLR cameras with USB streaming

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd WebViewCamberViewr

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

## Project Structure

```
app/src/main/
├── java/com/example/usbcameraviewer/
│   ├── UsbCameraActivity.kt      # Main activity
│   ├── SettingsManager.kt        # Settings persistence
│   ├── AppLogger.kt              # Logging system
│   └── LogViewerActivity.kt      # Log viewer
├── res/
│   ├── layout/
│   │   └── activity_log_viewer.xml
│   └── xml/
│       └── device_filter.xml     # USB device filter
└── AndroidManifest.xml
```

## Troubleshooting

### Camera not detected
- Ensure USB OTG is supported on your device
- Check if camera is UVC-compliant
- Try a different USB cable
- Check USB permissions in Android settings

### Black screen
- Try different resolution (use Auto Best Config)
- Check camera controls (reset brightness/contrast)
- View logs for error messages
- Restart the app

### Poor performance
- Lower resolution (try 720p or 480p)
- Reduce FPS to 30 or 24
- Close other apps
- Check device temperature

## Permissions

- `CAMERA` - Required for camera access (Android 9+)
- `USB_PERMISSION` - Automatically requested for USB devices

## License

Apache License 2.0

## Credits

- **AndroidUSBCamera (AUSBC)** by jiangdongguo - USB camera library
- **libuvc** - Native UVC camera support
- **libusb** - USB communication

## Documentation

📚 **[Complete Documentation Index](DOCUMENTATION_INDEX.md)** - Start here for all documentation

**Quick Links**:
- **[README.md](README.md)** - This file (user guide)
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick reference card
- **[DEVELOPER.md](DEVELOPER.md)** - Developer guide and API reference
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture
- **[APP_FLOW.md](APP_FLOW.md)** - Application flow and lifecycle
- **[SETUP.md](SETUP.md)** - Build and setup instructions
- **[CHANGELOG.md](CHANGELOG.md)** - Version history

## Support

For issues, questions, or contributions:
1. Check in-app logs first (⚙️ → View Logs)
2. Read [DEVELOPER.md](DEVELOPER.md) for technical details
3. See [APP_FLOW.md](APP_FLOW.md) for understanding the flow
4. Check [CHANGELOG.md](CHANGELOG.md) for known issues
