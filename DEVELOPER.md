# Developer Documentation

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Code Structure](#code-structure)
3. [Key Components](#key-components)
4. [AUSBC Library Integration](#ausbc-library-integration)
5. [Adding Features](#adding-features)
6. [Testing Guide](#testing-guide)
7. [Debugging](#debugging)
8. [Common Issues](#common-issues)
9. [Best Practices](#best-practices)
10. [API Reference](#api-reference)

---

## Architecture Overview

### Design Pattern

The app uses a **Fragment-based architecture** with the AUSBC library handling camera operations:

```
┌──────────────────────────────────────┐
│         UsbCameraActivity            │
│         (Host Container)             │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│       UsbCameraFragment              │
│    (UI + Camera Controller)          │
├──────────────────────────────────────┤
│  - UI Management                     │
│  - User Input Handling               │
│  - Settings Coordination             │
│  - Camera Control                    │
└──────┬───────────────┬───────────────┘
       │               │
       ↓               ↓
┌─────────────┐  ┌──────────────┐
│ Settings    │  │ AppLogger    │
│ Manager     │  │ (Singleton)  │
└─────────────┘  └──────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│      AUSBC Library (External)        │
├──────────────────────────────────────┤
│  - CameraClient                      │
│  - CameraUvcStrategy                 │
│  - USBMonitor                        │
│  - Native libuvc                     │
└──────────────────────────────────────┘
```

### Separation of Concerns

- **Activity**: Lifecycle management only
- **Fragment**: UI and user interaction
- **SettingsManager**: Data persistence
- **AppLogger**: Logging and debugging
- **AUSBC Library**: Camera operations

---

## Code Structure

### File Organization

```
app/src/main/
├── java/com/example/usbcameraviewer/
│   ├── UsbCameraActivity.kt          # 50 lines
│   │   └── UsbCameraFragment         # 300 lines
│   │       ├── UI Creation
│   │       ├── Camera Setup
│   │       ├── User Interactions
│   │       └── Settings Management
│   │
│   ├── SettingsManager.kt            # 80 lines
│   │   ├── CameraConfig (data class)
│   │   └── SharedPreferences wrapper
│   │
│   ├── AppLogger.kt                  # 60 lines
│   │   ├── LogEntry (data class)
│   │   └── Logging methods
│   │
│   └── LogViewerActivity.kt          # 100 lines
│       └── Log display UI
│
├── res/
│   ├── layout/
│   │   └── activity_log_viewer.xml  # Log viewer layout
│   ├── values/
│   │   ├── strings.xml
│   │   ├── colors.xml
│   │   └── themes.xml
│   └── xml/
│       └── device_filter.xml         # USB device filter
│
└── AndroidManifest.xml               # App configuration
```

### Dependency Graph

```
UsbCameraActivity
    └── UsbCameraFragment
            ├── SettingsManager
            ├── AppLogger
            └── CameraClient (AUSBC)
                    └── CameraUvcStrategy (AUSBC)
                            └── USBMonitor (AUSBC)
                                    └── libuvc (Native)
```

---

## Key Components

### 1. UsbCameraActivity

**Purpose**: Host container for the camera fragment

**Responsibilities**:
- Create and manage fragment
- Handle activity lifecycle
- Minimal logic (single responsibility)

**Key Methods**:
```kotlin
onCreate(savedInstanceState: Bundle?)
    - Creates UsbCameraFragment
    - Adds to fragment manager
```

**When to modify**:
- Adding multiple fragments
- Changing navigation
- Adding activity-level features

---

### 2. UsbCameraFragment

**Purpose**: Main UI and camera controller

**Responsibilities**:
- Create and manage UI
- Handle user interactions
- Configure camera client
- Coordinate settings
- Apply camera controls

**Key Methods**:

```kotlin
// Required overrides from CameraFragment
getRootView(): View
    - Creates entire UI hierarchy
    - Returns root layout

getCameraView(): IAspectRatio
    - Creates AspectRatioTextureView
    - Returns camera preview view

getCameraViewContainer(): ViewGroup
    - Returns container for camera view

getCameraClient(): CameraClient?
    - Configures and returns camera client
    - Called once during initialization

getGravity(): Int
    - Returns view gravity (CENTER)

// Initialization
initView()
    - Called after view created
    - Setup initial state

initData()
    - Called after initView
    - Setup spinners and load settings

// UI Creation
createSidebar()
    - Creates all control UI
    - Sets up event listeners

addSpinnerControl(label, items): Spinner
    - Helper to create labeled spinner

createSeekBar(label, min, max): SeekBar
    - Helper to create labeled seekbar

// User Interactions
toggleSidebar()
    - Show/hide sidebar

applyBestConfig()
    - Set optimal settings

applyTransform()
    - Apply rotation and flip

applyCameraControl(control, value)
    - Apply brightness/contrast/saturation

changeResolution(width, height)
    - Change camera resolution

// Settings
setupSpinners()
    - Configure spinner listeners

loadSavedConfig()
    - Restore saved settings
```

**When to modify**:
- Adding new controls
- Changing UI layout
- Adding camera features
- Modifying user interactions

---

### 3. SettingsManager

**Purpose**: Persistent storage of user preferences

**Responsibilities**:
- Save camera configuration
- Load camera configuration
- Manage SharedPreferences
- Provide default values

**Data Model**:
```kotlin
data class CameraConfig(
    val deviceName: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)
```

**Key Methods**:
```kotlin
saveConfig(config: CameraConfig)
    - Saves all settings to SharedPreferences
    - Async operation (apply())

loadConfig(): CameraConfig
    - Loads settings from SharedPreferences
    - Returns defaults if not found

setAutoLaunch(enabled: Boolean)
    - Save auto-launch preference

isAutoLaunchEnabled(): Boolean
    - Check auto-launch setting

clearSettings()
    - Reset all settings
```

**When to modify**:
- Adding new settings
- Changing default values
- Adding validation
- Migrating settings format

---

### 4. AppLogger

**Purpose**: Application-wide logging system

**Responsibilities**:
- Log to Logcat
- Store logs in memory
- Provide logs to viewer
- Manage log buffer size

**Data Model**:
```kotlin
data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)
```

**Key Methods**:
```kotlin
d(tag: String, message: String)
    - Debug log

i(tag: String, message: String)
    - Info log

w(tag: String, message: String)
    - Warning log

e(tag: String, message: String, throwable: Throwable?)
    - Error log

getLogs(): List<LogEntry>
    - Get all stored logs

clearLogs()
    - Clear log buffer
```

**When to modify**:
- Changing log buffer size
- Adding log filtering
- Adding log export
- Changing log format

---

## AUSBC Library Integration

### Understanding AUSBC

**AndroidUSBCamera (AUSBC)** is a third-party library that handles USB camera operations.

**Key Classes**:

1. **CameraFragment** (Base class we extend)
   - Handles camera lifecycle
   - Manages USB device detection
   - Provides camera control methods

2. **CameraClient** (Camera controller)
   - Main interface to camera
   - Handles preview, capture, recording
   - Manages OpenGL rendering

3. **CameraUvcStrategy** (USB camera strategy)
   - Implements UVC protocol
   - Handles USB communication
   - Provides camera controls

4. **USBMonitor** (USB device monitor)
   - Detects USB devices
   - Requests permissions
   - Monitors connect/disconnect

### Integration Points

#### 1. Fragment Extension

```kotlin
class UsbCameraFragment : CameraFragment() {
    // Must override these methods:
    override fun getRootView(): View
    override fun getCameraView(): IAspectRatio
    override fun getCameraViewContainer(): ViewGroup
    override fun getCameraClient(): CameraClient?
    override fun getGravity(): Int
}
```

#### 2. Camera Client Configuration

```kotlin
override fun getCameraClient(): CameraClient? {
    return CameraClient.newBuilder(requireContext())
        .setEnableGLES(true)           // Enable OpenGL
        .setRawImage(false)            // Don't need raw data
        .openDebug(true)               // Enable debug logs
        .setCameraStrategy(            // Use UVC strategy
            CameraUvcStrategy(requireContext())
        )
        .setCameraRequest(             // Set resolution
            CameraRequest.Builder()
                .setPreviewWidth(width)
                .setPreviewHeight(height)
                .create()
        )
        .build()
}
```

#### 3. Camera Controls

```kotlin
// Get the strategy
val strategy = getCurrentCameraStrategy() as? CameraUvcStrategy

// Apply controls
strategy?.setBrightness(value)
strategy?.setContrast(value)
strategy?.setSaturation(value)
strategy?.setZoom(value)
strategy?.setAutoFocus(enabled)
strategy?.setAutoWhiteBalance(enabled)
```

#### 4. Camera Operations

```kotlin
// Change resolution
updateResolution(width, height)

// Rotate view
setRotateType(RotateType.ANGLE_90)

// Get supported resolutions
val sizes = getAllPreviewSizes()

// Check if camera is open
val isOpen = isCameraOpened()
```

### Library Lifecycle

```
App Start
    ↓
Fragment created
    ↓
getCameraClient() called
    ↓
CameraClient built
    ↓
Lifecycle observer added
    ↓
ON_CREATE event
    ↓
CameraUvcStrategy.register()
    ↓
USBMonitor created
    ↓
Waiting for USB device...
    ↓
Device attached
    ↓
Permission requested
    ↓
Permission granted
    ↓
Camera opened
    ↓
Preview started
```

---

## Adding Features

### Adding a New Camera Control

**Example**: Adding Sharpness control

1. **Add UI in `createSidebar()`**:
```kotlin
val sharpnessSeek = createSeekBar("Sharpness", -100, 100)
```

2. **Handle in `applyCameraControl()`**:
```kotlin
when (control) {
    "Brightness" -> strategy.setBrightness(value)
    "Contrast" -> strategy.setContrast(value)
    "Saturation" -> strategy.setSaturation(value)
    "Sharpness" -> strategy.setSharpness(value)  // Add this
}
```

3. **Add to `CameraConfig` (optional)**:
```kotlin
data class CameraConfig(
    // ... existing fields
    val sharpness: Int = 0
)
```

4. **Save/Load in `SettingsManager`**:
```kotlin
// Save
putInt(KEY_SHARPNESS, config.sharpness)

// Load
sharpness = prefs.getInt(KEY_SHARPNESS, 0)
```

### Adding a New Resolution

1. **Update resolution list in `addSpinnerControl()`**:
```kotlin
addSpinnerControl("Resolution", listOf(
    "640x480",
    "1280x720",
    "1920x1080",
    "2560x1440",
    "3840x2160",
    "4096x2160"  // Add 4K DCI
))
```

2. **Test with your camera** to ensure it supports the resolution

### Adding a New Setting

**Example**: Adding zoom level

1. **Add to `CameraConfig`**:
```kotlin
data class CameraConfig(
    // ... existing fields
    val zoom: Int = 0
)
```

2. **Add UI control**:
```kotlin
val zoomSeek = createSeekBar("Zoom", 0, 100)
```

3. **Apply to camera**:
```kotlin
strategy?.setZoom(value)
```

4. **Save/Load**:
```kotlin
// SettingsManager
private const val KEY_ZOOM = "zoom"

fun saveConfig(config: CameraConfig) {
    putInt(KEY_ZOOM, config.zoom)
}

fun loadConfig(): CameraConfig {
    zoom = prefs.getInt(KEY_ZOOM, 0)
}
```

### Adding Photo Capture

```kotlin
// Add button
Button(requireContext()).apply {
    text = "📷 Capture"
    setOnClickListener { capturePhoto() }
}

// Implement capture
private fun capturePhoto() {
    val dir = File(requireContext().getExternalFilesDir(null), "photos")
    dir.mkdirs()
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    
    captureImage(object : ICaptureCallBack {
        override fun onBegin() {
            statusText.text = "Capturing..."
        }
        
        override fun onError(error: String?) {
            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
        }
        
        override fun onComplete(path: String?) {
            Toast.makeText(requireContext(), "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }, file.absolutePath)
}
```

---

## Testing Guide

### Manual Testing Checklist

#### Basic Functionality
- [ ] App launches successfully
- [ ] USB camera detected
- [ ] Permission dialog appears
- [ ] Video preview displays
- [ ] Sidebar opens/closes
- [ ] Settings persist after restart

#### Resolution Testing
- [ ] 640x480 works
- [ ] 1280x720 works
- [ ] 1920x1080 works
- [ ] Higher resolutions (if supported)
- [ ] Resolution changes apply correctly

#### Control Testing
- [ ] Brightness adjustment works
- [ ] Contrast adjustment works
- [ ] Saturation adjustment works
- [ ] Rotation works (0°, 90°, 180°, 270°)
- [ ] Horizontal flip works
- [ ] Vertical flip works
- [ ] FPS changes apply

#### Edge Cases
- [ ] Disconnect camera during preview
- [ ] Reconnect camera
- [ ] Deny USB permission
- [ ] Grant permission after denial
- [ ] Rotate device
- [ ] Background/foreground app
- [ ] Low memory conditions

### Automated Testing

**Unit Tests** (Future):
```kotlin
@Test
fun testSettingsManager_saveAndLoad() {
    val config = CameraConfig(width = 1920, height = 1080)
    settingsManager.saveConfig(config)
    val loaded = settingsManager.loadConfig()
    assertEquals(1920, loaded.width)
    assertEquals(1080, loaded.height)
}
```

**UI Tests** (Future):
```kotlin
@Test
fun testSidebarToggle() {
    onView(withId(R.id.toggleButton)).perform(click())
    onView(withId(R.id.sidebar)).check(matches(isDisplayed()))
}
```

### Test Devices

**Recommended test matrix**:
- Android 5.0 (API 21) - Minimum
- Android 8.0 (API 26) - Common
- Android 10 (API 29) - Common
- Android 13 (API 33) - Target
- Various manufacturers (Samsung, Xiaomi, Google, etc.)

### Test Cameras

- Standard webcam (Logitech, etc.)
- USB endoscope
- Action camera with USB output
- Different resolutions (480p, 720p, 1080p, 4K)
- Different frame rates

---

## Debugging

### Enable Debug Logs

Already enabled in `getCameraClient()`:
```kotlin
.openDebug(true)
```

### View Logs

1. **In-app**: Tap ⚙️ → View Logs
2. **Logcat**: `adb logcat | grep -E "UsbCamera|AUSBC"`
3. **Filtered**: `adb logcat *:E` (errors only)

### Common Log Tags

- `UsbCameraFragment` - Fragment operations
- `SettingsManager` - Settings operations
- `AppLogger` - Logging system
- `CameraClient` - AUSBC camera client
- `CameraUvc` - AUSBC UVC strategy
- `USBMonitor` - USB device monitoring

### Debug Breakpoints

**Key locations**:
1. `getCameraClient()` - Camera initialization
2. `changeResolution()` - Resolution changes
3. `applyCameraControl()` - Control adjustments
4. `applyTransform()` - Rotation/flip
5. `toggleSidebar()` - UI interactions

### Memory Profiling

```bash
# Check memory usage
adb shell dumpsys meminfo com.example.usbcameraviewer

# Monitor in real-time
adb shell top | grep usbcamera
```

### Performance Profiling

```bash
# CPU usage
adb shell top -m 10

# Frame rate
adb shell dumpsys gfxinfo com.example.usbcameraviewer
```

---

## Common Issues

### Issue: Black Screen

**Causes**:
1. Camera not supported
2. Wrong resolution
3. USB permission denied
4. OpenGL not initialized

**Solutions**:
1. Check logs for errors
2. Try lower resolution (640x480)
3. Reconnect camera and grant permission
4. Restart app

**Debug**:
```kotlin
// Add in getCameraClient()
AppLogger.i("Debug", "OpenGL enabled: ${isEnableGLES}")
AppLogger.i("Debug", "Resolution: ${width}x${height}")
```

### Issue: Camera Not Detected

**Causes**:
1. USB OTG not supported
2. Camera not UVC-compliant
3. Bad USB cable
4. USB filter mismatch

**Solutions**:
1. Check device specs for OTG
2. Test camera on PC
3. Try different cable
4. Check `device_filter.xml`

**Debug**:
```kotlin
// Add in initData()
val usbManager = requireContext().getSystemService(UsbManager::class.java)
val devices = usbManager.deviceList
AppLogger.i("Debug", "USB devices: ${devices.size}")
devices.values.forEach { device ->
    AppLogger.i("Debug", "Device: ${device.productName}, Class: ${device.deviceClass}")
}
```

### Issue: Settings Not Persisting

**Causes**:
1. SharedPreferences not saving
2. App data cleared
3. Storage permission issue

**Solutions**:
1. Check `apply()` is called
2. Don't clear app data
3. Check storage permissions

**Debug**:
```kotlin
// Add in saveConfig()
AppLogger.i("Settings", "Saving: $config")

// Add in loadConfig()
val loaded = loadConfig()
AppLogger.i("Settings", "Loaded: $loaded")
```

### Issue: Poor Performance

**Causes**:
1. Resolution too high
2. FPS too high
3. Device too old
4. Other apps running

**Solutions**:
1. Lower resolution to 720p
2. Set FPS to 30
3. Close other apps
4. Restart device

**Debug**:
```kotlin
// Monitor frame rate
var frameCount = 0
var lastTime = System.currentTimeMillis()

// In preview callback
frameCount++
val now = System.currentTimeMillis()
if (now - lastTime >= 1000) {
    AppLogger.i("FPS", "Current FPS: $frameCount")
    frameCount = 0
    lastTime = now
}
```

---

## Best Practices

### Code Style

1. **Kotlin conventions**:
   - Use `val` over `var` when possible
   - Use data classes for models
   - Use extension functions
   - Use scope functions (apply, let, also)

2. **Naming**:
   - Classes: PascalCase
   - Functions: camelCase
   - Constants: UPPER_SNAKE_CASE
   - Private fields: camelCase with descriptive names

3. **Documentation**:
   - Document public APIs
   - Explain complex logic
   - Add TODO comments for future work

### Error Handling

```kotlin
// Always handle errors
try {
    // Risky operation
} catch (e: Exception) {
    AppLogger.e("Tag", "Error message", e)
    Toast.makeText(context, "User-friendly message", Toast.LENGTH_SHORT).show()
}
```

### Resource Management

```kotlin
// Clean up resources
override fun onDestroyView() {
    super.onDestroyView()
    cameraView = null
    settingsManager = null
}
```

### Threading

```kotlin
// UI updates on main thread
activity?.runOnUiThread {
    statusText.text = "Updated"
}

// Heavy work on background thread
lifecycleScope.launch(Dispatchers.IO) {
    // Heavy operation
    withContext(Dispatchers.Main) {
        // Update UI
    }
}
```

### Memory Leaks

**Avoid**:
- Storing activity/context in static fields
- Not canceling coroutines
- Not unregistering listeners
- Holding references to views

**Good practice**:
```kotlin
// Use lifecycle-aware components
lifecycleScope.launch {
    // Automatically canceled when lifecycle ends
}

// Weak references for callbacks
private var callback: WeakReference<Callback>? = null
```

---

## API Reference

### CameraFragment Methods

```kotlin
// Camera operations
fun updateResolution(width: Int, height: Int)
fun setRotateType(type: RotateType)
fun getAllPreviewSizes(aspectRatio: Double?): List<PreviewSize>
fun isCameraOpened(): Boolean

// Capture operations
fun captureImage(callback: ICaptureCallBack, path: String)
fun captureVideoStart(callback: ICaptureCallBack, path: String, durationInSec: Long)
fun captureVideoStop()

// Camera strategy
fun getCurrentCameraStrategy(): ICameraStrategy?
fun getCurrentPreviewSize(): PreviewSize?
```

### CameraUvcStrategy Methods

```kotlin
// Camera controls
fun setBrightness(value: Int)
fun setContrast(value: Int)
fun setSaturation(value: Int)
fun setHue(value: Int)
fun setSharpness(value: Int)
fun setGain(value: Int)
fun setGamma(value: Int)
fun setZoom(value: Int)
fun setAutoFocus(enabled: Boolean)
fun setAutoWhiteBalance(enabled: Boolean)

// Get current values
fun getBrightness(): Int?
fun getContrast(): Int?
// ... etc
```

### SettingsManager Methods

```kotlin
fun saveConfig(config: CameraConfig)
fun loadConfig(): CameraConfig
fun setAutoLaunch(enabled: Boolean)
fun isAutoLaunchEnabled(): Boolean
fun clearSettings()
```

### AppLogger Methods

```kotlin
fun d(tag: String, message: String)
fun i(tag: String, message: String)
fun w(tag: String, message: String)
fun e(tag: String, message: String, throwable: Throwable? = null)
fun getLogs(): List<LogEntry>
fun clearLogs()
```

---

## Contributing

### Pull Request Process

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Make changes
4. Test thoroughly
5. Commit (`git commit -m 'Add amazing feature'`)
6. Push (`git push origin feature/amazing-feature`)
7. Open Pull Request

### Code Review Checklist

- [ ] Code follows style guide
- [ ] All tests pass
- [ ] No new warnings
- [ ] Documentation updated
- [ ] Changelog updated
- [ ] Tested on real device

---

## Resources

### Documentation
- [AUSBC Library](https://github.com/jiangdongguo/AndroidUSBCamera)
- [Android USB Host](https://developer.android.com/guide/topics/connectivity/usb/host)
- [UVC Specification](https://www.usb.org/document-library/video-class-v15-document-set)

### Tools
- [Android Studio](https://developer.android.com/studio)
- [ADB](https://developer.android.com/studio/command-line/adb)
- [Logcat](https://developer.android.com/studio/debug/am-logcat)

### Community
- GitHub Issues
- Stack Overflow
- Android Developers Community

---

**Last Updated**: November 2024  
**Version**: 1.2  
**Maintainer**: Development Team
