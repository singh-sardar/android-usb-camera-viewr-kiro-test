# Application Flow Diagram

## App Launch Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      App Launch                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                  MainActivity.onCreate()                     │
│  • Initialize views                                          │
│  • Create UsbCameraManager                                   │
│  • Request camera permission                                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Permission Granted?                             │
└─────────────┬───────────────────────┬───────────────────────┘
              │ YES                   │ NO
              ▼                       ▼
┌─────────────────────────┐  ┌──────────────────────────────┐
│  initializeCamera()     │  │  Show permission error       │
│  • Scan USB devices     │  │  Request permission again    │
│  • Setup UI spinners    │  └──────────────────────────────┘
│  • Observe state flows  │
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│           CameraTextureView Surface Ready                    │
│  • Surface created callback                                  │
│  • Call cameraManager.startCamera(surface)                   │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              UsbCameraManager.startCamera()                  │
│  • Set isRunning = true                                      │
│  • Launch coroutine                                          │
│  • Call connectToCamera()                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│           UsbCameraManager.connectToCamera()                 │
│  1. Set state to Connecting                                  │
│  2. Get USB devices from UsbManager                          │
│  3. Filter for webcams (UVC class 14)                        │
│  4. Select device from config or first available             │
└─────────────────────┬───────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
┌──────────────────┐    ┌──────────────────────────┐
│  Devices Found   │    │   No Devices Found       │
└────────┬─────────┘    └────────┬─────────────────┘
         │                       │
         ▼                       ▼
┌──────────────────┐    ┌──────────────────────────┐
│ Open Device      │    │  Set state to Error      │
│ • Create Native  │    │  Schedule retry (15s)    │
│   UsbCamera      │    └──────────────────────────┘
│ • Call open()    │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│         NativeUsbCamera.open()                   │
│  • Check USB permission                          │
│  • Open USB connection                           │
│  • Claim interface                               │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│    NativeUsbCamera.startStreaming()              │
│  • Set preview size (width, height, fps)         │
│  • Start frame capture                           │
│  • Render to surface                             │
└────────┬─────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│         Set state to Connected                   │
│  • Update UI status (green)                      │
│  • Display camera feed                           │
└──────────────────────────────────────────────────┘
```

## Configuration Change Flow

```
┌─────────────────────────────────────────────────────────────┐
│              User Changes Configuration                      │
│  • Select different resolution                               │
│  • Change FPS                                                │
│  • Adjust rotation                                           │
│  • Toggle flip settings                                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│           Spinner/Checkbox Change Listener                   │
│  • Auto-triggered on value change                            │
│  • Calls applyConfiguration()                                │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│            MainActivity.applyConfiguration()                 │
│  • Read all spinner/checkbox values                          │
│  • Create new CameraConfig object                            │
│  • Call cameraManager.updateConfig(config)                   │
│  • Call cameraView.updateTransform()                         │
└─────────────────────┬───────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
┌──────────────────────┐  ┌──────────────────────────┐
│ Update Camera Config │  │  Update View Transform   │
│ • Store new config   │  │  • Apply rotation        │
│ • Restart streaming  │  │  • Apply flip H/V        │
│   with new params    │  │  • Update matrix         │
└──────────────────────┘  └──────────────────────────┘
```

## USB Device Hotplug Flow

```
┌─────────────────────────────────────────────────────────────┐
│              USB Device Attached/Detached                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│            Android Broadcasts USB Intent                     │
│  • ACTION_USB_DEVICE_ATTACHED                                │
│  • ACTION_USB_DEVICE_DETACHED                                │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│           UsbDeviceReceiver.onReceive()                      │
│  • Extract UsbDevice from intent                             │
│  • Call appropriate callback                                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
┌──────────────────┐    ┌──────────────────────────┐
│  Device Attached │    │   Device Detached        │
│  • Show toast    │    │   • Show toast           │
│  • Scan devices  │    │   • Scan devices         │
│  • Update list   │    │   • Update list          │
│  • Auto-connect  │    │   • Trigger retry        │
└──────────────────┘    └──────────────────────────┘
```

## Auto-Retry Flow

```
┌─────────────────────────────────────────────────────────────┐
│              Camera Connection Failed                        │
│  • No devices found                                          │
│  • Device open failed                                        │
│  • Streaming start failed                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Set State to Error                              │
│  • Update UI status (red)                                    │
│  • Display error message                                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              scheduleRetry(surface)                          │
│  • Cancel existing retry job                                 │
│  • Launch new coroutine                                      │
│  • Delay 15 seconds                                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
                  Wait 15s
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Check if Still Running                          │
│  • Is app still active?                                      │
│  • Is state still Error/Disconnected?                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          │ YES                   │ NO
          ▼                       ▼
┌──────────────────┐    ┌──────────────────────────┐
│ Retry Connection │    │   Cancel Retry           │
│ • Call connect   │    │   • Do nothing           │
│ • Repeat cycle   │    └──────────────────────────┘
└──────────────────┘
```

## State Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    CameraState (Sealed Class)                │
└─────────────────────┬───────────────────────────────────────┘
                      │
          ┌───────────┼───────────┬───────────┐
          │           │           │           │
          ▼           ▼           ▼           ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
    │Disconnect│ │Connecting│ │Connected │ │  Error   │
    └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
         │            │            │            │
         ▼            ▼            ▼            ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
    │UI: Gray  │ │UI: Orange│ │UI: Green │ │UI: Red   │
    │Text: Off │ │Text: ...│ │Text: OK  │ │Text: Err │
    └──────────┘ └──────────┘ └──────────┘ └──────────┘
         │            │            │            │
         └────────────┴────────────┴────────────┘
                      │
                      ▼
         ┌────────────────────────────┐
         │  StateFlow Emission        │
         │  • Collected in MainActivity│
         │  • Updates UI automatically │
         └────────────────────────────┘
```

## UI Component Hierarchy

```
MainActivity (AppCompatActivity)
│
├── FrameLayout (Root)
│   │
│   ├── CameraTextureView (Full screen)
│   │   └── Surface (Camera rendering)
│   │
│   ├── Button (Toggle Sidebar - Top Right)
│   │   └── Text: "⚙"
│   │
│   └── ScrollView (Sidebar - Right side)
│       └── LinearLayout (Vertical)
│           │
│           ├── TextView (Title: "Camera Configuration")
│           ├── TextView (Status indicator)
│           │
│           ├── TextView (Label: "Camera Device")
│           ├── Spinner (Device selection)
│           │
│           ├── TextView (Label: "Resolution")
│           ├── Spinner (Resolution selection)
│           │
│           ├── TextView (Label: "Frame Rate")
│           ├── Spinner (FPS selection)
│           │
│           ├── TextView (Label: "Rotation")
│           ├── Spinner (Rotation selection)
│           │
│           ├── TextView (Label: "Image Flip")
│           ├── CheckBox (Flip Horizontal)
│           ├── CheckBox (Flip Vertical)
│           │
│           └── Button (Apply Configuration)
```

## Data Flow

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│   USB        │─────▶│  UsbCameraManager│─────▶│  StateFlow   │
│   Device     │      │  • Scan devices  │      │  • State     │
└──────────────┘      │  • Connect       │      │  • Devices   │
                      │  • Configure     │      │  • Config    │
                      └────────┬─────────┘      └──────┬───────┘
                               │                       │
                               ▼                       ▼
                      ┌──────────────────┐    ┌──────────────┐
                      │  NativeUsbCamera │    │  MainActivity│
                      │  • Open device   │    │  • Observe   │
                      │  • Stream frames │    │  • Update UI │
                      └────────┬─────────┘    └──────┬───────┘
                               │                     │
                               ▼                     ▼
                      ┌──────────────────┐    ┌──────────────┐
                      │     Surface      │    │   UI Views   │
                      │  • Render frames │    │  • Spinners  │
                      └──────────────────┘    │  • Checkboxes│
                                              └──────────────┘
```

## Thread Model

```
┌─────────────────────────────────────────────────────────────┐
│                        Main Thread                           │
│  • UI updates                                                │
│  • User interactions                                         │
│  • StateFlow collection                                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   Coroutine Scope                            │
│  • Camera operations (Dispatchers.IO)                        │
│  • USB communication                                         │
│  • Retry scheduling                                          │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   Background Threads                         │
│  • Frame capture                                             │
│  • Frame decoding                                            │
│  • USB bulk/iso transfers                                    │
└─────────────────────────────────────────────────────────────┘
```

This architecture ensures:
- **Responsive UI** - No blocking on main thread
- **Proper lifecycle** - Resources cleaned up correctly
- **State consistency** - Single source of truth via StateFlow
- **Error recovery** - Automatic retry mechanism
- **Extensibility** - Easy to add features
