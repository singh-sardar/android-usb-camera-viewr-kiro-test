# Troubleshooting Guide

Solutions to common issues with USB Camera Viewer.

## Camera Issues

### Camera Not Detected

**Symptoms**: No camera appears in device list

**Solutions**:
1. **Check USB connection**
   - Ensure cable is properly connected
   - Try different USB port
   - Try different USB cable

2. **Check USB OTG support**
   - Verify device supports USB Host mode
   - Test with USB OTG checker app
   - Try different OTG adapter

3. **Check camera compatibility**
   - Verify camera is UVC-compliant
   - Test camera on computer first
   - Check camera power requirements

4. **Grant USB permission**
   - Reconnect camera
   - Tap "OK" on permission dialog
   - Check Settings > Apps > USB Camera Viewer > Permissions

5. **Restart app**
   - Force close app
   - Reopen app
   - Reconnect camera

### Black Screen

**Symptoms**: Camera detected but no video

**Solutions**:
1. **Wait for auto-retry**
   - App retries every 15 seconds
   - Check status message
   - Wait up to 30 seconds

2. **Try different resolution**
   - Open settings
   - Select lower resolution (720p or VGA)
   - Check if video appears

3. **Check camera power**
   - Use powered USB hub
   - Check camera LED indicator
   - Try different power source

4. **Restart camera**
   - Disconnect camera
   - Wait 5 seconds
   - Reconnect camera

### Poor Video Quality

**Symptoms**: Blurry, pixelated, or choppy video

**Solutions**:
1. **Increase resolution**
   - Open settings
   - Select higher resolution
   - Check if quality improves

2. **Adjust camera controls**
   - Open camera controls
   - Increase brightness
   - Adjust contrast and saturation
   - Enable auto-focus

3. **Check lighting**
   - Improve room lighting
   - Avoid backlighting
   - Use external lights if needed

4. **Clean camera lens**
   - Gently clean with soft cloth
   - Remove dust and fingerprints

### Video Lag or Stuttering

**Symptoms**: Choppy, delayed, or frozen video

**Solutions**:
1. **Lower resolution**
   - Use 720p instead of 1080p
   - Use VGA for best performance

2. **Lower frame rate**
   - Use 30fps instead of 60fps
   - Use 24fps or 15fps if needed

3. **Close other apps**
   - Free up system resources
   - Check running apps
   - Restart device if needed

4. **Check device performance**
   - Ensure device isn't overheating
   - Check available RAM
   - Update Android OS

## App Issues

### App Crashes on Launch

**Symptoms**: App closes immediately after opening

**Solutions**:
1. **Check Android version**
   - Requires Android 7.0 or higher
   - Update Android if possible

2. **Grant permissions**
   - Settings > Apps > USB Camera Viewer
   - Grant Camera permission
   - Grant Storage permission

3. **Clear app data**
   - Settings > Apps > USB Camera Viewer
   - Tap "Storage"
   - Tap "Clear Data"
   - Reopen app

4. **Reinstall app**
   - Uninstall app
   - Restart device
   - Reinstall APK

### Settings Not Saving

**Symptoms**: Settings reset after closing app

**Solutions**:
1. **Check storage permission**
   - Settings > Apps > USB Camera Viewer
   - Grant Storage permission

2. **Clear app cache**
   - Settings > Apps > USB Camera Viewer
   - Tap "Storage"
   - Tap "Clear Cache"

3. **Reinstall app**
   - Uninstall app
   - Reinstall APK
   - Reconfigure settings

### Auto-Launch Not Working

**Symptoms**: App doesn't open when camera connected

**Solutions**:
1. **Enable auto-launch**
   - Open app
   - Open settings
   - Check "Launch app when camera connected"

2. **Grant USB permission**
   - Connect camera
   - Tap "OK" on permission dialog
   - Check "Always allow"

3. **Check USB defaults**
   - Settings > Connected devices > USB
   - Check default USB app
   - Clear defaults if needed

4. **Restart device**
   - Power off device
   - Power on device
   - Try connecting camera again

## Permission Issues

### Camera Permission Denied

**Symptoms**: "Camera permission required" message

**Solutions**:
1. **Grant permission manually**
   - Settings > Apps > USB Camera Viewer
   - Tap "Permissions"
   - Enable "Camera"

2. **Reinstall app**
   - Uninstall app
   - Reinstall APK
   - Grant permission when prompted

### USB Permission Denied

**Symptoms**: "No permission for device" message

**Solutions**:
1. **Reconnect camera**
   - Disconnect camera
   - Reconnect camera
   - Tap "OK" on permission dialog

2. **Check "Always allow"**
   - When permission dialog appears
   - Check "Always allow for this device"
   - Tap "OK"

3. **Clear USB defaults**
   - Settings > Apps > USB Camera Viewer
   - Tap "Open by default"
   - Tap "Clear defaults"
   - Reconnect camera

## Performance Issues

### High CPU Usage

**Symptoms**: Device gets hot, battery drains fast

**Solutions**:
1. **Lower resolution and FPS**
   - Use 720p @ 30fps
   - Avoid 4K or 60fps

2. **Close other apps**
   - Free up CPU resources
   - Check background apps

3. **Let device cool down**
   - Stop using app
   - Wait for device to cool
   - Resume use

### High Memory Usage

**Symptoms**: Device slows down, other apps close

**Solutions**:
1. **Restart app**
   - Close app completely
   - Clear from recent apps
   - Reopen app

2. **Restart device**
   - Power off device
   - Wait 10 seconds
   - Power on device

3. **Free up RAM**
   - Close unused apps
   - Clear app caches
   - Restart device

## Connection Issues

### Frequent Disconnections

**Symptoms**: Camera disconnects repeatedly

**Solutions**:
1. **Check USB cable**
   - Use high-quality cable
   - Try different cable
   - Check for damage

2. **Check power supply**
   - Use powered USB hub
   - Check camera power requirements
   - Try different power source

3. **Check USB port**
   - Try different USB port
   - Clean USB port
   - Check for damage

### Slow Connection

**Symptoms**: Takes long time to connect

**Solutions**:
1. **Wait for initialization**
   - First connection takes longer
   - Wait up to 30 seconds
   - Check status message

2. **Restart app**
   - Close app
   - Reopen app
   - Reconnect camera

## Error Messages

### "No USB camera found"
- **Cause**: No camera detected
- **Solution**: Check USB connection, verify camera is UVC-compliant

### "Failed to open camera"
- **Cause**: Permission denied or camera in use
- **Solution**: Grant USB permission, close other camera apps

### "Failed to start streaming"
- **Cause**: Unsupported format or camera error
- **Solution**: Try different resolution, restart camera

### "Camera disconnected"
- **Cause**: USB connection lost
- **Solution**: Check cable, wait for auto-retry

## Getting Help

### Check Logs
```bash
adb logcat | grep -E "UsbCamera|MainActivity"
```

### Report Issue
Include:
- Android version
- Device model
- Camera model
- Error message
- Steps to reproduce

### Contact Support
- GitHub Issues
- Email support
- User forum

---

**Still having issues?** Check the [FAQ](../reference/FAQ.md) or contact support.
