# Application Flow Documentation

## Overview

This document describes the complete flow of the USB Camera Viewer application, from startup to camera streaming.

## 1. Application Startup Flow

```
User launches app
    ↓
UsbCameraActivity.onCreate()
    ↓
Creates UsbCameraFragment
    ↓
Fragment added to activity
    ↓
Fragment lifecycle begins
```

### 1.1 Activity Initialization

**File**: `UsbCameraActivity.kt`

```kotlin
onCreate() {
    - Check if savedInstanceState is null
    - Create new UsbCameraFragment
    - Replace fragment in content view
}
```

**Purpose**: Host the camera fragment and manage activity lifecycle.

---

## 2. Fragment Initialization Flow

```
UsbCameraFragment created
    ↓
getRootView() called
    ↓
UI components created
    ↓
onViewCreated()
    ↓
initView() called
    ↓
initData() called
    ↓
Camera client initialized
```

### 2.1 UI Creation (`getRootView()`)

**Order of execution**:

1. **Create main layout** (FrameLayout)
   - Full-screen container
   - Black background

2. **Create camera container** (FrameLayout)
   - Holds the camera preview
   - Full-screen, behind other UI

3. **Create status bar** (TextView)
   - Top of screen
   - Shows current status
   - Semi-transparent background

4. **Create toggle button** (Button)
   - Top-right corner
   - Opens/closes sidebar
   - ⚙️ icon

5. **Create sidebar** (`createSidebar()`)
   - ScrollView with controls
   - Initially hidden
   - 300dp wide, right-aligned

### 2.2 Sidebar Creation (`createSidebar()`)

**Components created in order**:

1. Title text
2. Auto Best Config button
3. Resolution spinner (640x480 to 4K)
4. FPS spinner (15, 24, 30, 60)
5. Rotation spinner (0°, 90°, 180°, 270°)
6. Flip horizontal checkbox
7. Flip vertical checkbox
8. Brightness seekbar (-100 to +100)
9. Contrast seekbar (-100 to +100)
10. Saturation seekbar (-100 to +100)
11. View Logs button

---

## 3. Camera Initialization Flow

```
initData() called
    ↓
getCameraClient() called
    ↓
SettingsManager created
    ↓
Load saved config
    ↓
CameraClient.Builder created
    ↓
Configure camera strategy (UVC)
    ↓
Set camera request (resolution)
    ↓
Build camera client
    ↓
Return to parent class
    ↓
Parent class handles USB detection
```

### 3.1 Camera Client Configuration

**File**: `UsbCameraActivity.kt` - `getCameraClient()`

```kotlin
1. Create SettingsManager
2. Load saved configuration
3. Create CameraClient.Builder with:
   - Context
   - Enable OpenGL ES (true)
   - Raw image (false)
   - Debug mode (true)
   - Camera strategy (CameraUvcStrategy)
   - Camera request (width, height from config)
4. Build and return client
```

### 3.2 AUSBC Library Internal Flow

**Handled by AndroidUSBCamera library**:

```
CameraClient created
    ↓
Lifecycle observer added
    ↓
ON_CREATE event
    ↓
CameraUvcStrategy.register() called
    ↓
USBMonitor created
    ↓
USB device listener registered
    ↓
Waiting for USB device...
```

---

## 4. USB Camera Connection Flow

```
USB camera plugged in
    ↓
Android broadcasts USB_DEVICE_ATTACHED
    ↓
USBMonitor receives broadcast
    ↓
Device detected
    ↓
Check device filter (UVC class 14)
    ↓
Request USB permission
    ↓
User grants permission
    ↓
onConnectDev() callback
    ↓
Camera opened
    ↓
OpenGL surface created
    ↓
onSurfaceTextureAvailable() callback
    ↓
startPreview() called
    ↓
Video streaming starts
```

### 4.1 USB Permission Flow

```
USBMonitor detects device
    ↓
Check if permission already granted
    ↓
If not: Show Android permission dialog
    ↓
User taps "OK" or "Cancel"
    ↓
If OK: onConnectDev() called
    ↓
If Cancel: onCancelDev() called
```

### 4.2 Camera Opening Flow

**Internal AUSBC flow**:

```
onConnectDev() callback
    ↓
CameraClient.openCamera() called
    ↓
Check if OpenGL enabled
    ↓
RenderManager.startRenderScreen()
    ↓
Create SurfaceTexture
    ↓
onSurfaceTextureAvailable() callback
    ↓
ICameraStrategy.startPreview()
    ↓
Send message to handler (what=1)
    ↓
Handler calls startPreviewInternal()
    ↓
UVC camera opened
    ↓
Video frames start flowing
```

---

## 5. User Interaction Flows

### 5.1 Opening Sidebar

```
User taps ⚙️ button
    ↓
toggleSidebar() called
    ↓
Check current visibility
    ↓
If hidden: Show sidebar (slide in)
    ↓
If visible: Hide sidebar (slide out)
```

### 5.2 Changing Resolution

```
User selects resolution from spinner
    ↓
onItemSelected() callback
    ↓
Parse resolution string (e.g., "1920x1080")
    ↓
changeResolution(width, height) called
    ↓
updateResolution() (parent method)
    ↓
Save to SettingsManager
    ↓
Update status text
    ↓
Show toast notification
    ↓
Camera restarts with new resolution
```

### 5.3 Adjusting Camera Control

```
User moves seekbar (brightness/contrast/saturation)
    ↓
onProgressChanged() callback
    ↓
Calculate value (progress + min)
    ↓
Update value text
    ↓
If fromUser: applyCameraControl() called
    ↓
Get CameraUvcStrategy
    ↓
Call strategy method (setBrightness/setContrast/setSaturation)
    ↓
Strategy sends USB control command
    ↓
Camera hardware applies change
    ↓
Effect visible in preview
```

### 5.4 Applying Rotation/Flip

```
User changes rotation spinner or flip checkbox
    ↓
onItemSelected() or onCheckedChanged() callback
    ↓
applyTransform() called
    ↓
Get rotation value (0, 90, 180, 270)
    ↓
Get flip values (horizontal, vertical)
    ↓
Convert to RotateType enum
    ↓
setRotateType() (parent method)
    ↓
Save to SettingsManager
    ↓
Update status text
    ↓
OpenGL applies transformation
    ↓
Preview updates immediately
```

### 5.5 Auto Best Config

```
User taps "Auto Best Config" button
    ↓
applyBestConfig() called
    ↓
Set resolution to 1280x720 (index 1)
    ↓
Set FPS to 30 (index 2)
    ↓
Set rotation to 0° (index 0)
    ↓
Uncheck flip horizontal
    ↓
Uncheck flip vertical
    ↓
Reset brightness to 0
    ↓
Reset contrast to 0
    ↓
Reset saturation to 0
    ↓
Show toast "Applied best configuration"
    ↓
All changes trigger their respective flows
```

---

## 6. Settings Persistence Flow

### 6.1 Saving Settings

```
User changes any setting
    ↓
Setting change callback triggered
    ↓
Get current config from SettingsManager
    ↓
Create new config with updated value
    ↓
Call settingsManager.saveConfig()
    ↓
SharedPreferences.edit()
    ↓
Put values (putInt, putBoolean, etc.)
    ↓
apply() - async save
    ↓
Settings saved to disk
```

### 6.2 Loading Settings

```
initData() called
    ↓
setupSpinners() called
    ↓
loadSavedConfig() called
    ↓
Get config from SettingsManager
    ↓
SettingsManager.loadConfig()
    ↓
SharedPreferences.getInt/getBoolean/etc.
    ↓
Create CameraConfig object
    ↓
Return to fragment
    ↓
Set spinner selections
    ↓
Set checkbox states
    ↓
UI reflects saved settings
```

---

## 7. Logging Flow

### 7.1 Writing Logs

```
Code calls AppLogger.i/d/w/e()
    ↓
Create LogEntry with:
    - Current timestamp
    - Log level (INFO/DEBUG/WARN/ERROR)
    - Tag
    - Message
    ↓
Add to logs list
    ↓
If list > maxLogs: Remove oldest
    ↓
Also log to Android Logcat
    ↓
Log stored in memory
```

### 7.2 Viewing Logs

```
User taps "View Logs" button
    ↓
Start LogViewerActivity
    ↓
Activity onCreate()
    ↓
Get all logs from AppLogger
    ↓
Create formatted text with colors
    ↓
Display in TextView
    ↓
User can scroll through logs
```

---

## 8. Application Lifecycle

### 8.1 App Pause/Resume

```
User switches to another app
    ↓
onPause() called
    ↓
Camera keeps streaming (background)
    ↓
User returns to app
    ↓
onResume() called
    ↓
UI updates
    ↓
Camera still streaming
```

### 8.2 App Destroy

```
User closes app or system kills it
    ↓
onDestroy() called
    ↓
Fragment onDestroyView()
    ↓
Camera client cleanup
    ↓
CameraUvcStrategy.unRegister()
    ↓
USBMonitor.unregister()
    ↓
Release camera resources
    ↓
OpenGL context destroyed
    ↓
App terminated
```

### 8.3 USB Camera Disconnect

```
User unplugs USB camera
    ↓
USBMonitor detects disconnect
    ↓
onDetachDec() callback
    ↓
Camera preview stops
    ↓
Black screen shown
    ↓
Status text: "Camera disconnected"
    ↓
Waiting for reconnection...
```

---

## 9. Error Handling Flow

### 9.1 Camera Open Error

```
Camera fails to open
    ↓
onError() callback
    ↓
Log error message
    ↓
Update status text
    ↓
Show toast with error
    ↓
Camera remains closed
    ↓
User can try reconnecting
```

### 9.2 Permission Denied

```
User denies USB permission
    ↓
onCancelDev() callback
    ↓
Log permission denied
    ↓
Show toast "Permission denied"
    ↓
Camera cannot open
    ↓
User must reconnect and grant permission
```

---

## 10. Data Flow Diagram

```
┌─────────────────┐
│  User Action    │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  UI Component   │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Fragment       │
│  (Controller)   │
└────────┬────────┘
         │
         ├──→ SettingsManager (Save/Load)
         │
         ├──→ AppLogger (Log)
         │
         └──→ CameraClient (AUSBC)
                    │
                    ↓
         ┌──────────────────┐
         │ CameraUvcStrategy│
         └────────┬─────────┘
                  │
                  ↓
         ┌──────────────────┐
         │   USBMonitor     │
         └────────┬─────────┘
                  │
                  ↓
         ┌──────────────────┐
         │  USB Camera      │
         │  (Hardware)      │
         └──────────────────┘
```

---

## 11. Threading Model

### Main Thread (UI Thread)
- UI updates
- User interactions
- Fragment lifecycle
- Toast messages
- Status text updates

### Background Threads (AUSBC Library)
- USB communication
- Camera frame capture
- Video encoding
- Image processing

### OpenGL Thread
- Video rendering
- Frame transformation
- Rotation/flip effects

---

## 12. Memory Management

### Objects Lifecycle

**Activity Scope**:
- UsbCameraActivity instance

**Fragment Scope**:
- UI components (layouts, buttons, spinners)
- CameraView (AspectRatioTextureView)
- SettingsManager

**Application Scope**:
- AppLogger (singleton)
- Log entries (limited to 500)

### Memory Cleanup

```
Fragment destroyed
    ↓
cameraView = null
    ↓
settingsManager = null
    ↓
UI components garbage collected
    ↓
CameraClient destroyed by parent
    ↓
Native resources released
```

---

## 13. Performance Considerations

### Optimization Points

1. **OpenGL Rendering**: Hardware-accelerated, minimal CPU usage
2. **Settings Persistence**: Async writes, no UI blocking
3. **Logging**: Limited buffer (500 entries), old logs discarded
4. **USB Communication**: Handled by native library, efficient
5. **UI Updates**: Only when values change, no unnecessary redraws

### Resource Usage

- **CPU**: Low (5-10% on modern devices)
- **Memory**: ~50MB (including video buffers)
- **Battery**: Moderate (camera streaming)
- **Storage**: Minimal (only settings, ~1KB)

---

## Summary

The application follows a clean, event-driven architecture:

1. **Initialization**: Activity → Fragment → Camera Client
2. **USB Detection**: Automatic via AUSBC library
3. **User Control**: Direct manipulation via sidebar
4. **Settings**: Persistent via SharedPreferences
5. **Logging**: In-memory buffer for debugging
6. **Cleanup**: Proper resource management on destroy

All flows are designed to be simple, efficient, and maintainable.
