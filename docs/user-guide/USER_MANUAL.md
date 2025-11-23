# User Manual

Complete guide to using USB Camera Viewer.

## Table of Contents

1. [Installation](#installation)
2. [Getting Started](#getting-started)
3. [Configuration](#configuration)
4. [Camera Controls](#camera-controls)
5. [Advanced Features](#advanced-features)
6. [Tips & Tricks](#tips--tricks)

## Installation

### Requirements
- Android 7.0 or higher
- USB Host support
- UVC-compliant USB camera
- USB OTG adapter (for phones/tablets)

### Install Steps
1. Download APK file
2. Enable "Install from unknown sources" if needed
3. Open APK file
4. Tap "Install"
5. Wait for installation to complete

## Getting Started

### First Launch
1. Open "USB Camera Viewer"
2. Grant camera permission
3. Connect USB camera
4. Grant USB permission
5. Camera view appears automatically

### Interface Overview
- **Main View**: Full-screen camera display
- **Settings Button** (⚙): Top-right corner
- **Status Indicator**: Shows connection status
- **Live Badge**: Indicates active streaming

## Configuration

### Opening Settings
Tap the ⚙ icon in the top-right corner to open the configuration sidebar.

### Camera Selection
**Purpose**: Choose which camera to use (if multiple connected)

**Steps**:
1. Open settings sidebar
2. Find "Camera Device" dropdown
3. Select desired camera
4. Camera switches automatically

### Resolution Settings
**Purpose**: Control video quality and performance

**Options**:
- **4K (3840x2160)** - Highest quality, requires powerful device
- **1080p (1920x1080)** - High quality, balanced performance
- **720p (1280x720)** - Good quality, better performance
- **VGA (640x480)** - Lower quality, best performance

**Steps**:
1. Open settings sidebar
2. Find "Resolution" dropdown
3. Select desired resolution
4. Video restarts with new resolution

### Frame Rate Settings
**Purpose**: Control video smoothness

**Options**:
- **60 FPS** - Smoothest, requires good camera
- **30 FPS** - Standard, works with most cameras
- **24 FPS** - Cinematic look
- **15 FPS** - Lower bandwidth

**Steps**:
1. Open settings sidebar
2. Find "Frame Rate" dropdown
3. Select desired FPS
4. Changes apply immediately

### Rotation Settings
**Purpose**: Rotate image for different mounting angles

**Options**:
- **0°** - Normal orientation
- **90°** - Rotated right
- **180°** - Upside down
- **270°** - Rotated left

**Steps**:
1. Open settings sidebar
2. Find "Rotation" dropdown
3. Select angle
4. Image rotates immediately

### Flip Settings
**Purpose**: Mirror image horizontally or vertically

**Options**:
- **Flip Horizontal** - Mirror left-right
- **Flip Vertical** - Mirror top-bottom
- **Both** - Mirror both ways

**Steps**:
1. Open settings sidebar
2. Check "Flip Horizontal" or "Flip Vertical"
3. Image flips immediately

## Camera Controls

### Opening Camera Controls
1. Open settings sidebar
2. Tap "Camera Controls" (red button)
3. Dialog appears with controls

### Brightness
**Purpose**: Adjust image brightness

**Range**: 0-100
- **Low (0-30)**: Darker image
- **Medium (40-60)**: Balanced
- **High (70-100)**: Brighter image

**Steps**:
1. Open camera controls
2. Drag "Brightness" slider
3. Preview updates in real-time

### Contrast
**Purpose**: Adjust difference between light and dark

**Range**: 0-100
- **Low**: Flat, washed out
- **Medium**: Balanced
- **High**: Sharp, vivid

### Saturation
**Purpose**: Adjust color intensity

**Range**: 0-100
- **Low**: Less colorful, grayscale
- **Medium**: Natural colors
- **High**: Vivid, saturated colors

### Auto Focus
**Purpose**: Automatically adjust focus

**Options**:
- **On**: Camera adjusts focus automatically
- **Off**: Manual focus (if supported)

### Auto White Balance
**Purpose**: Automatically adjust color temperature

**Options**:
- **On**: Camera adjusts colors automatically
- **Off**: Manual white balance

## Advanced Features

### Settings Persistence
**What it does**: Saves all your settings automatically

**How it works**:
- Settings save when you change them
- Settings restore when you reopen app
- Works across app restarts
- Works across device reboots

**Saved settings**:
- Camera device
- Resolution
- Frame rate
- Rotation
- Flip settings
- Auto-launch preference

### Auto-Launch
**What it does**: Opens app automatically when camera connected

**How to enable**:
1. Open settings sidebar
2. Check "Launch app when camera connected"
3. Close app
4. Connect camera
5. App opens automatically

**How to disable**:
1. Open settings sidebar
2. Uncheck "Launch app when camera connected"

### Auto-Retry
**What it does**: Automatically reconnects if camera disconnects

**How it works**:
- Detects disconnection
- Waits 15 seconds
- Attempts reconnection
- Repeats until connected

**Status messages**:
- "Camera disconnected" - Connection lost
- "Connecting..." - Attempting to connect
- "Camera connected" - Successfully connected

### USB Hotplug
**What it does**: Detects when cameras are connected/disconnected

**Features**:
- Toast notification on attach/detach
- Automatic device list update
- Auto-reconnect on attach
- Graceful disconnect handling

## Tips & Tricks

### Best Performance
1. Use 1080p @ 30fps for balanced quality/performance
2. Close other apps to free resources
3. Use powered USB hub for multiple cameras
4. Keep device cool for sustained use

### Best Quality
1. Use 4K resolution if device supports it
2. Use 60fps for smooth motion
3. Adjust brightness and contrast for your lighting
4. Enable auto-focus and auto white balance

### Battery Saving
1. Use lower resolution (720p or VGA)
2. Use lower frame rate (24 or 15 fps)
3. Reduce brightness
4. Close app when not in use

### Troubleshooting
1. Check [Troubleshooting Guide](TROUBLESHOOTING.md)
2. View logs: `adb logcat | grep UsbCamera`
3. Clear app data if issues persist
4. Reinstall app as last resort

## Keyboard Shortcuts

*Not applicable - touch interface only*

## Accessibility

The app supports:
- Large touch targets
- High contrast mode
- Screen reader compatibility
- Simple, clear interface

---

**Need more help?** Check the [FAQ](../reference/FAQ.md) or [Troubleshooting Guide](TROUBLESHOOTING.md)
