# Architecture Documentation

## System Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Android System                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Activity   │  │  Fragment    │  │   Services   │     │
│  │   Manager    │  │   Manager    │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   USB Camera Viewer App                     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              UsbCameraActivity                       │  │
│  │              (Lifecycle Host)                        │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
│                       ↓                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           UsbCameraFragment                          │  │
│  │           (UI Controller)                            │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐    │  │
│  │  │  Camera    │  │  Settings  │  │   Logger   │    │  │
│  │  │   View     │  │  Manager   │  │            │    │  │
│  │  └────────────┘  └────────────┘  └────────────┘    │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
└───────────────────────┼─────────────────────────────────────┘
                        │
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              AUSBC Library (Third-Party)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              CameraClient                            │  │
│  │              (Camera Controller)                     │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
│                       ↓                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           CameraUvcStrategy                          │  │
│  │           (USB Camera Handler)                       │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
│                       ↓                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              USBMonitor                              │  │
│  │              (Device Monitor)                        │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
└───────────────────────┼─────────────────────────────────────┘
                        │
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                  Native Layer (JNI)                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                 libuvc                               │  │
│  │                 (UVC Protocol)                       │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                     │
│                       ↓                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                 libusb                               │  │
│  │                 (USB Communication)                  │  │
│  └────────────────────┬─────────────────────────────────┘  │
└───────────────────────┼─────────────────────────────────────┘
                        │
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                  Hardware Layer                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              USB Camera Device                       │  │
│  │              (Physical Hardware)                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Component Architecture

### Application Layer

```
┌─────────────────────────────────────────────────────────┐
│                  UsbCameraActivity                      │
│                                                         │
│  Responsibilities:                                      │
│  • Host fragment container                              │
│  • Manage activity lifecycle                            │
│  • Handle system callbacks                              │
│                                                         │
│  Lifecycle:                                             │
│  onCreate() → onStart() → onResume() → onPause() →     │
│  onStop() → onDestroy()                                 │
└─────────────────────────────────────────────────────────┘
                        │
                        │ contains
                        ↓
┌─────────────────────────────────────────────────────────┐
│               UsbCameraFragment                         │
│                                                         │
│  Responsibilities:                                      │
│  • Create and manage UI                                 │
│  • Handle user interactions                             │
│  • Configure camera client                              │
│  • Apply camera controls                                │
│  • Coordinate settings                                  │
│                                                         │
│  Key Methods:                                           │
│  • getRootView() - Create UI                            │
│  • getCameraView() - Provide camera view                │
│  • getCameraClient() - Configure camera                 │
│  • initData() - Initialize data                         │
│                                                         │
│  UI Components:                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Camera    │  │   Status    │  │   Toggle    │    │
│  │  Container  │  │    Bar      │  │   Button    │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│  ┌──────────────────────────────────────────────┐      │
│  │              Sidebar (Collapsible)           │      │
│  │  • Resolution Spinner                        │      │
│  │  • FPS Spinner                               │      │
│  │  • Rotation Spinner                          │      │
│  │  • Flip Checkboxes                           │      │
│  │  • Brightness SeekBar                        │      │
│  │  • Contrast SeekBar                          │      │
│  │  • Saturation SeekBar                        │      │
│  │  • Auto Best Config Button                   │      │
│  │  • View Logs Button                          │      │
│  └──────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Data Layer

```
┌─────────────────────────────────────────────────────────┐
│                  SettingsManager                        │
│                                                         │
│  Purpose: Persistent storage of user preferences        │
│                                                         │
│  Storage: SharedPreferences                             │
│  Location: /data/data/com.example.usbcameraviewer/     │
│           shared_prefs/camera_settings.xml              │
│                                                         │
│  Data Model:                                            │
│  ┌─────────────────────────────────────────────┐       │
│  │         CameraConfig                        │       │
│  │  • deviceName: String                       │       │
│  │  • width: Int (1920)                        │       │
│  │  • height: Int (1080)                       │       │
│  │  • fps: Int (30)                            │       │
│  │  • rotation: Int (0)                        │       │
│  │  • flipHorizontal: Boolean (false)          │       │
│  │  • flipVertical: Boolean (false)            │       │
│  └─────────────────────────────────────────────┘       │
│                                                         │
│  Operations:                                            │
│  • saveConfig(config) - Save settings                   │
│  • loadConfig() - Load settings                         │
│  • setAutoLaunch(enabled) - Auto-launch setting         │
│  • isAutoLaunchEnabled() - Check auto-launch            │
│  • clearSettings() - Reset all                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    AppLogger                            │
│                                                         │
│  Purpose: Application-wide logging system               │
│                                                         │
│  Type: Singleton (object)                               │
│  Storage: In-memory buffer (500 entries)                │
│                                                         │
│  Data Model:                                            │
│  ┌─────────────────────────────────────────────┐       │
│  │           LogEntry                          │       │
│  │  • timestamp: String (HH:mm:ss.SSS)         │       │
│  │  • level: String (DEBUG/INFO/WARN/ERROR)    │       │
│  │  • tag: String                              │       │
│  │  • message: String                          │       │
│  └─────────────────────────────────────────────┘       │
│                                                         │
│  Operations:                                            │
│  • d(tag, message) - Debug log                          │
│  • i(tag, message) - Info log                           │
│  • w(tag, message) - Warning log                        │
│  • e(tag, message, throwable) - Error log               │
│  • getLogs() - Retrieve all logs                        │
│  • clearLogs() - Clear buffer                           │
│                                                         │
│  Output:                                                │
│  • Android Logcat                                       │
│  • In-memory buffer (for log viewer)                    │
└─────────────────────────────────────────────────────────┘
```

---

## AUSBC Library Architecture

### Library Components

```
┌─────────────────────────────────────────────────────────┐
│                   CameraClient                          │
│                                                         │
│  Purpose: Main camera controller interface              │
│                                                         │
│  Responsibilities:                                      │
│  • Manage camera lifecycle                              │
│  • Handle preview rendering                             │
│  • Coordinate camera operations                         │
│  • Manage OpenGL context                                │
│                                                         │
│  Configuration:                                         │
│  • enableGLES: Boolean - Use OpenGL rendering           │
│  • rawImage: Boolean - Capture raw frames               │
│  • debug: Boolean - Enable debug logs                   │
│  • cameraStrategy: ICameraStrategy - Camera type        │
│  • cameraRequest: CameraRequest - Resolution/FPS        │
│                                                         │
│  Key Methods:                                           │
│  • openCamera(view) - Start camera                      │
│  • closeCamera() - Stop camera                          │
│  • captureImage() - Take photo                          │
│  • captureVideoStart() - Start recording                │
│  • captureVideoStop() - Stop recording                  │
└─────────────────────────────────────────────────────────┘
                        │
                        │ uses
                        ↓
┌─────────────────────────────────────────────────────────┐
│                CameraUvcStrategy                        │
│                                                         │
│  Purpose: USB Video Class camera implementation         │
│                                                         │
│  Responsibilities:                                      │
│  • Implement UVC protocol                               │
│  • Handle USB communication                             │
│  • Provide camera controls                              │
│  • Manage device connection                             │
│                                                         │
│  Camera Controls:                                       │
│  • Brightness (-100 to 100)                             │
│  • Contrast (-100 to 100)                               │
│  • Saturation (-100 to 100)                             │
│  • Hue (-100 to 100)                                    │
│  • Sharpness (-100 to 100)                              │
│  • Zoom (0 to 100)                                      │
│  • Auto Focus (on/off)                                  │
│  • Auto White Balance (on/off)                          │
│                                                         │
│  Device Management:                                     │
│  • register() - Start monitoring                        │
│  • unRegister() - Stop monitoring                       │
│  • getUsbDeviceList() - List devices                    │
│  • getCurrentDevice() - Get active device               │
└─────────────────────────────────────────────────────────┘
                        │
                        │ uses
                        ↓
┌─────────────────────────────────────────────────────────┐
│                   USBMonitor                            │
│                                                         │
│  Purpose: Monitor USB device connections                │
│                                                         │
│  Responsibilities:                                      │
│  • Detect USB device attach/detach                      │
│  • Request USB permissions                              │
│  • Manage device connections                            │
│  • Filter UVC devices                                   │
│                                                         │
│  Callbacks:                                             │
│  • onAttachDev() - Device attached                      │
│  • onDetachDec() - Device detached                      │
│  • onConnectDev() - Device connected                    │
│  • onDisConnectDev() - Device disconnected              │
│  • onCancelDev() - Permission denied                    │
│                                                         │
│  Device Filter:                                         │
│  • USB Class: 14 (Video)                                │
│  • Subclass: 1 (Video Control)                          │
│  • Subclass: 2 (Video Streaming)                        │
└─────────────────────────────────────────────────────────┘
```

### Native Layer

```
┌─────────────────────────────────────────────────────────┐
│                      libuvc                             │
│                                                         │
│  Purpose: Native UVC protocol implementation            │
│                                                         │
│  Language: C/C++                                        │
│  Interface: JNI (Java Native Interface)                 │
│                                                         │
│  Responsibilities:                                      │
│  • Implement UVC specification                          │
│  • Handle isochronous transfers                         │
│  • Decode video formats (MJPEG, YUV, etc.)              │
│  • Manage camera controls                               │
│                                                         │
│  Supported Formats:                                     │
│  • MJPEG - Motion JPEG                                  │
│  • YUYV - YUV 4:2:2                                     │
│  • NV12 - YUV 4:2:0                                     │
│  • H.264 - Hardware encoded (if supported)              │
└─────────────────────────────────────────────────────────┘
                        │
                        │ uses
                        ↓
┌─────────────────────────────────────────────────────────┐
│                     libusb                              │
│                                                         │
│  Purpose: Low-level USB communication                   │
│                                                         │
│  Language: C                                            │
│  Interface: System calls                                │
│                                                         │
│  Responsibilities:                                      │
│  • USB device enumeration                               │
│  • USB bulk transfers                                   │
│  • USB isochronous transfers                            │
│  • USB control transfers                                │
│  • USB interrupt transfers                              │
│                                                         │
│  Transfer Types:                                        │
│  • Control - Device configuration                       │
│  • Bulk - Large data transfers                          │
│  • Isochronous - Real-time video streaming              │
│  • Interrupt - Status updates                           │
└─────────────────────────────────────────────────────────┘
```

---

## Data Flow

### Video Streaming Flow

```
USB Camera (Hardware)
    │
    │ USB Isochronous Transfer
    ↓
libusb (Native)
    │
    │ Raw USB packets
    ↓
libuvc (Native)
    │
    │ Decoded video frames (MJPEG/YUV)
    ↓
USBMonitor (Java)
    │
    │ Frame callbacks
    ↓
CameraUvcStrategy (Java)
    │
    │ Processed frames
    ↓
CameraClient (Java)
    │
    │ OpenGL textures
    ↓
RenderManager (OpenGL ES)
    │
    │ Rendered frames
    ↓
AspectRatioTextureView (UI)
    │
    │ Display
    ↓
Screen (User sees video)
```

### User Input Flow

```
User Interaction (Touch)
    │
    ↓
UI Component (Button/Spinner/SeekBar)
    │
    │ Event callback
    ↓
UsbCameraFragment
    │
    │ Process input
    ↓
┌───┴────────────────────────┐
│                            │
↓                            ↓
SettingsManager         CameraClient
│                            │
│ Save setting               │ Apply control
↓                            ↓
SharedPreferences       CameraUvcStrategy
                             │
                             │ USB control command
                             ↓
                        USB Camera (Hardware)
```

### Settings Persistence Flow

```
User Changes Setting
    │
    ↓
UsbCameraFragment
    │
    │ Create new config
    ↓
SettingsManager.saveConfig()
    │
    │ Serialize data
    ↓
SharedPreferences.edit()
    │
    │ Write to disk (async)
    ↓
/data/data/.../shared_prefs/camera_settings.xml

--- App Restart ---

UsbCameraFragment.initData()
    │
    ↓
SettingsManager.loadConfig()
    │
    │ Read from disk
    ↓
SharedPreferences.getInt/getBoolean()
    │
    │ Deserialize data
    ↓
CameraConfig object
    │
    │ Apply to UI
    ↓
UI Components updated
```

---

## Threading Model

```
┌─────────────────────────────────────────────────────────┐
│                    Main Thread (UI)                     │
│                                                         │
│  • Activity lifecycle                                   │
│  • Fragment lifecycle                                   │
│  • UI updates                                           │
│  • User interactions                                    │
│  • Toast messages                                       │
│  • Status text updates                                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              Background Thread (AUSBC)                  │
│                                                         │
│  • USB communication                                    │
│  • Frame capture                                        │
│  • Video encoding                                       │
│  • Image processing                                     │
│  • File I/O                                             │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│               OpenGL Thread (Rendering)                 │
│                                                         │
│  • Frame rendering                                      │
│  • Texture updates                                      │
│  • Rotation/flip effects                                │
│  • Color adjustments                                    │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              Native Thread (libuvc)                     │
│                                                         │
│  • USB packet processing                                │
│  • Frame decoding                                       │
│  • Format conversion                                    │
└─────────────────────────────────────────────────────────┘
```

---

## Memory Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Heap Memory                           │
│                                                         │
│  Application Objects:                                   │
│  ┌─────────────────────────────────────────────┐       │
│  │  UsbCameraActivity          ~1 KB           │       │
│  │  UsbCameraFragment          ~5 KB           │       │
│  │  UI Components              ~10 KB          │       │
│  │  SettingsManager            ~1 KB           │       │
│  │  AppLogger + Logs           ~50 KB          │       │
│  └─────────────────────────────────────────────┘       │
│                                                         │
│  AUSBC Library:                                         │
│  ┌─────────────────────────────────────────────┐       │
│  │  CameraClient               ~5 KB           │       │
│  │  CameraUvcStrategy          ~5 KB           │       │
│  │  USBMonitor                 ~2 KB           │       │
│  │  Video Buffers              ~20 MB          │       │
│  │  OpenGL Textures            ~10 MB          │       │
│  └─────────────────────────────────────────────┘       │
│                                                         │
│  Total: ~50 MB                                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Native Memory                          │
│                                                         │
│  libuvc:                                                │
│  ┌─────────────────────────────────────────────┐       │
│  │  Frame buffers              ~15 MB          │       │
│  │  Decode buffers             ~5 MB           │       │
│  │  USB transfer buffers       ~2 MB           │       │
│  └─────────────────────────────────────────────┘       │
│                                                         │
│  Total: ~22 MB                                          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Storage (Disk)                         │
│                                                         │
│  APK: 8.6 MB                                            │
│  Settings: ~1 KB                                        │
│  Logs: In-memory only                                   │
│                                                         │
│  Total: ~8.6 MB                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Security Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Permission Model                       │
│                                                         │
│  Manifest Permissions:                                  │
│  • CAMERA (dangerous) - Android 9+                      │
│                                                         │
│  Runtime Permissions:                                   │
│  • USB_PERMISSION - Per device                          │
│    Requested automatically by USBMonitor                │
│    User must grant for each camera                      │
│                                                         │
│  No Network Permissions:                                │
│  • No internet access                                   │
│  • No data transmission                                 │
│  • Fully offline operation                              │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   Data Privacy                          │
│                                                         │
│  No Data Collection:                                    │
│  • No analytics                                         │
│  • No crash reporting                                   │
│  • No user tracking                                     │
│                                                         │
│  Local Storage Only:                                    │
│  • Settings in SharedPreferences                        │
│  • Logs in memory (not persisted)                       │
│  • No external storage access                           │
└─────────────────────────────────────────────────────────┘
```

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Build Process                        │
│                                                         │
│  Source Code (Kotlin)                                   │
│         │                                               │
│         ↓                                               │
│  Kotlin Compiler                                        │
│         │                                               │
│         ↓                                               │
│  Java Bytecode (.class)                                 │
│         │                                               │
│         ↓                                               │
│  D8 Compiler (DEX)                                      │
│         │                                               │
│         ↓                                               │
│  DEX Files (.dex)                                       │
│         │                                               │
│         ↓                                               │
│  APK Builder                                            │
│         │                                               │
│         ├─→ Resources                                   │
│         ├─→ Native Libraries (.so)                      │
│         ├─→ Manifest                                    │
│         └─→ Assets                                      │
│         │                                               │
│         ↓                                               │
│  APK File (8.6 MB)                                      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  APK Structure                          │
│                                                         │
│  app-debug.apk (8.6 MB)                                 │
│  ├── AndroidManifest.xml                                │
│  ├── classes.dex (2 MB)                                 │
│  ├── resources.arsc                                     │
│  ├── res/ (layouts, values)                             │
│  └── lib/                                               │
│      ├── armeabi-v7a/                                   │
│      │   ├── libUVCCamera.so                            │
│      │   ├── libuvc.so                                  │
│      │   ├── libusb100.so                               │
│      │   └── libnativelib.so                            │
│      └── arm64-v8a/                                     │
│          ├── libUVCCamera.so                            │
│          ├── libuvc.so                                  │
│          ├── libusb100.so                               │
│          └── libnativelib.so                            │
└─────────────────────────────────────────────────────────┘
```

---

## Summary

The USB Camera Viewer follows a clean, layered architecture:

1. **Presentation Layer**: Activity and Fragment for UI
2. **Business Logic**: Camera controls and settings management
3. **Data Layer**: Settings persistence and logging
4. **Library Layer**: AUSBC for camera operations
5. **Native Layer**: libuvc and libusb for USB communication
6. **Hardware Layer**: Physical USB camera device

This separation ensures:
- **Maintainability**: Clear responsibilities
- **Testability**: Independent components
- **Scalability**: Easy to add features
- **Performance**: Efficient resource usage
- **Reliability**: Proper error handling

---

**Last Updated**: November 2024  
**Version**: 1.2
