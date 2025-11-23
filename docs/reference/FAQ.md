# Frequently Asked Questions (FAQ)

## General Questions

### What is USB Camera Viewer?
USB Camera Viewer is an Android app that displays video from USB cameras connected to Android devices. It's designed for Android TV boxes, tablets, and phones with USB Host support.

### Is it free?
Yes, the app is provided as-is for both personal and commercial use.

### What devices are supported?
Any Android device running Android 7.0 or higher with USB Host support.

### What cameras are supported?
Any UVC-compliant USB camera. Most modern USB webcams are UVC-compliant.

## Installation Questions

### How do I install the app?
Download the APK and install it on your Android device. You may need to enable "Install from unknown sources" in settings.

### Do I need root access?
No, root access is not required.

### Can I install from Google Play Store?
Not yet. Currently, the app is distributed as an APK file.

### What permissions does the app need?
- Camera permission (required)
- USB access permission (required)
- Storage permission (for settings)

## Camera Questions

### My camera isn't detected. Why?
- Check USB connection
- Verify camera is UVC-compliant
- Grant USB permission
- Try different USB port/cable

### What is UVC?
UVC (USB Video Class) is a standard for USB cameras. Most modern webcams support it.

### Can I use multiple cameras?
Yes, you can connect multiple cameras and switch between them in settings.

### What resolutions are supported?
4K (3840x2160), 1080p (1920x1080), 720p (1280x720), and VGA (640x480).

### What frame rates are supported?
60, 30, 24, and 15 FPS, depending on camera capabilities.

## Feature Questions

### Does it record video?
Not yet. Recording functionality is planned for future versions.

### Can I take snapshots?
Not yet. Snapshot functionality is planned for future versions.

### Does it support audio?
Audio recording permission is requested but audio streaming is not yet implemented.

### Can I zoom?
Not yet. Zoom functionality is planned for future versions.

### Does it work with wireless cameras?
No, only USB cameras are supported.

## Settings Questions

### Are settings saved?
Yes, all settings are automatically saved and restored when you reopen the app.

### Can I reset settings?
Yes, clear app data in Android settings to reset all settings to defaults.

### What is auto-launch?
Auto-launch opens the app automatically when you connect a USB camera.

### How do I disable auto-launch?
Open settings sidebar and uncheck "Launch app when camera connected".

## Performance Questions

### Why is the video laggy?
- Try lower resolution (720p or VGA)
- Try lower frame rate (30 or 15 FPS)
- Close other apps
- Check device performance

### Why is my device getting hot?
High resolution and frame rate require more processing power. Try lower settings.

### How much battery does it use?
Battery usage depends on resolution and frame rate. Lower settings use less battery.

### Can I use it for hours?
Yes, the app is designed for long-running use. Use lower settings for extended use.

## Technical Questions

### What Android version is required?
Android 7.0 (API 24) or higher.

### Does it work on Android TV?
Yes, it's optimized for Android TV boxes.

### Does it work on tablets?
Yes, if the tablet has USB Host support.

### Does it work on phones?
Yes, with a USB OTG adapter.

### What is USB OTG?
USB On-The-Go allows Android devices to act as USB hosts and connect USB devices.

### Does it require internet?
No, the app works completely offline.

## Troubleshooting Questions

### App crashes on launch. What do I do?
- Check Android version (7.0+ required)
- Grant all permissions
- Clear app data
- Reinstall app

### Black screen. What's wrong?
- Wait 15 seconds for auto-retry
- Try different resolution
- Check camera power supply
- Restart camera

### Settings don't save. Why?
- Grant storage permission
- Clear app cache
- Reinstall app

### Auto-launch doesn't work. Why?
- Enable auto-launch in settings
- Grant USB permission
- Check "Always allow" for USB device

## Development Questions

### Is the source code available?
Check the repository for source code availability.

### Can I contribute?
Yes, contributions are welcome. Check the contributing guidelines.

### What language is it written in?
Kotlin, the modern language for Android development.

### What architecture does it use?
MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow.

### Can I use this code in my project?
Check the license file for usage terms.

## Future Features

### Will you add recording?
Yes, video recording is planned for a future version.

### Will you add snapshots?
Yes, snapshot functionality is planned.

### Will you add zoom?
Yes, zoom controls are planned.

### Will you add filters?
Possibly, depending on user demand.

### Will you support RTSP/IP cameras?
Not planned currently. The app focuses on USB cameras.

## Support Questions

### How do I report a bug?
Create an issue on GitHub with details about the problem.

### How do I request a feature?
Create a feature request on GitHub.

### How do I get help?
- Check this FAQ
- Read the [User Manual](../user-guide/USER_MANUAL.md)
- Check [Troubleshooting Guide](../user-guide/TROUBLESHOOTING.md)
- Contact support

### Is there a user community?
Check GitHub discussions or create one if interested.

---

**Question not answered?** Check the [User Manual](../user-guide/USER_MANUAL.md) or [Troubleshooting Guide](../user-guide/TROUBLESHOOTING.md)
