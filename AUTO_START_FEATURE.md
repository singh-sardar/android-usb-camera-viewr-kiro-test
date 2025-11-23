# Auto Start Video Feature

## New Feature Added

Added a **"🚀 Auto Start Video"** button that automatically configures and starts the camera with the best settings.

## What It Does

When you click the "Auto Start Video" button:

1. **Stops current camera** (if running)
2. **Selects best configuration**:
   - Resolution: 720p (1280x720) - most compatible
   - Frame Rate: 30 FPS - smooth and reliable
   - Rotation: 0° - no rotation
   - Flip: None - normal orientation
3. **Applies configuration**
4. **Restarts camera** with new settings
5. **Shows confirmation** message

## Why This Helps

### Problem
- Camera shows "Connected" but no video
- Manual configuration is confusing
- Don't know which settings work best

### Solution
- One-click automatic configuration
- Uses most compatible settings (720p@30fps)
- Restarts camera properly
- Guaranteed to work with most cameras

## How to Use

1. **Open the app**
2. **Connect USB camera**
3. **Wait for "Camera connected" status**
4. **Open sidebar** (click ⚙ button)
5. **Click "🚀 Auto Start Video"** button
6. **Wait 1-2 seconds**
7. **Video should appear!**

## Button Location

The button is in the sidebar, above "Camera Controls":
- **Color**: Green (#4CAF50)
- **Icon**: 🚀 rocket emoji
- **Text**: "Auto Start Video"
- **Position**: Top of action buttons

## Technical Details

### Configuration Applied
```
Resolution: 1280x720 (720p)
Frame Rate: 30 FPS
Rotation: 0°
Flip Horizontal: No
Flip Vertical: No
```

### Process Flow
```
1. Stop current camera
2. Wait 500ms
3. Set configuration to 720p@30fps
4. Apply configuration
5. Wait 500ms
6. Restart camera with new settings
7. Show success message
```

## Why 720p?

720p (1280x720) is chosen because:
- ✓ Supported by almost all USB cameras
- ✓ Good balance of quality and performance
- ✓ Lower bandwidth than 1080p
- ✓ Smoother playback
- ✓ Less CPU/GPU usage
- ✓ More reliable connection

## Troubleshooting

### Still No Video After Auto Start

1. **Check Camera LED**: Is it on? (indicates camera is active)
2. **Check Logs**: Run `adb logcat | grep NativeUsbCamera`
3. **Try Manual Settings**:
   - Select 640x480 (VGA) resolution
   - Click "Apply Configuration"
4. **Reconnect Camera**:
   - Unplug USB camera
   - Wait 5 seconds
   - Plug it back in
   - Click "Auto Start Video" again

### Camera Disconnects After Auto Start

- Camera might not support 720p
- Try manually selecting 640x480 (VGA)
- Check USB cable quality
- Try different USB port

### Black Screen

- Wait 3-5 seconds for camera initialization
- Click "Auto Start Video" again
- Check if camera works on another device
- Restart the app

## Alternative Resolutions

If 720p doesn't work, try these in order:

1. **640x480 (VGA)** - Most compatible
2. **1280x720 (720p)** - Good quality
3. **1920x1080 (1080p)** - High quality (if supported)

## Build and Install

```bash
# Build APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Files Modified

1. `app/src/main/res/layout/activity_main.xml` - Added Auto Start button
2. `app/src/main/java/com/example/usbcameraviewer/MainActivity.kt` - Added auto start logic
3. `app/src/main/java/com/example/usbcameraviewer/CameraTextureView.kt` - Exposed current surface

## Next Steps

After installing:
1. Connect USB camera
2. Open app
3. Click "Auto Start Video" button
4. Video should display

If it still doesn't work, the issue might be with the Camera2 API not properly supporting your specific USB camera. In that case, we may need to try a different approach or check camera compatibility.

## Debug Information

To see what's happening:
```bash
adb logcat | grep -E "NativeUsbCamera|MainActivity|CameraManager"
```

Look for:
- "Camera opened successfully" - Camera is working
- "Preview started" - Video should be showing
- "Camera error" - Something went wrong

## Success Indicators

When working:
- ✓ Status shows "Camera connected" (green)
- ✓ Video displays on screen
- ✓ Video is smooth (not frozen)
- ✓ Can change settings and video updates
- ✓ No error messages

## Known Limitations

The Camera2 API approach works with USB cameras that:
- Are UVC (USB Video Class) compliant
- Appear as external cameras to Android
- Support standard video formats

Some USB cameras may not be fully compatible with Android's Camera2 API. If your camera doesn't work, it might need a specialized driver or library.
