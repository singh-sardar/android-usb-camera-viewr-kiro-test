package com.example.usbcameraviewer

import android.content.Context
import android.content.SharedPreferences

/**
 * Camera configuration data class
 * Stores all camera settings that can be persisted
 */
data class CameraConfig(
    val deviceName: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

/**
 * Manages persistent storage of camera settings
 * Uses SharedPreferences to save and restore user preferences
 */
class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "camera_settings", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_FPS = "fps"
        private const val KEY_ROTATION = "rotation"
        private const val KEY_FLIP_HORIZONTAL = "flip_horizontal"
        private const val KEY_FLIP_VERTICAL = "flip_vertical"
        private const val KEY_AUTO_LAUNCH = "auto_launch"
        private const val KEY_LAST_DEVICE = "last_device"
    }
    
    fun saveConfig(config: CameraConfig) {
        prefs.edit().apply {
            putString(KEY_DEVICE_NAME, config.deviceName)
            putInt(KEY_WIDTH, config.width)
            putInt(KEY_HEIGHT, config.height)
            putInt(KEY_FPS, config.fps)
            putInt(KEY_ROTATION, config.rotation)
            putBoolean(KEY_FLIP_HORIZONTAL, config.flipHorizontal)
            putBoolean(KEY_FLIP_VERTICAL, config.flipVertical)
            apply()
        }
    }
    
    fun loadConfig(): CameraConfig {
        return CameraConfig(
            deviceName = prefs.getString(KEY_DEVICE_NAME, "") ?: "",
            width = prefs.getInt(KEY_WIDTH, 1920),
            height = prefs.getInt(KEY_HEIGHT, 1080),
            fps = prefs.getInt(KEY_FPS, 30),
            rotation = prefs.getInt(KEY_ROTATION, 0),
            flipHorizontal = prefs.getBoolean(KEY_FLIP_HORIZONTAL, false),
            flipVertical = prefs.getBoolean(KEY_FLIP_VERTICAL, false)
        )
    }
    
    fun setAutoLaunch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LAUNCH, enabled).apply()
    }
    
    fun isAutoLaunchEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LAUNCH, true) // Default to true
    }
    
    fun saveLastDevice(deviceName: String) {
        prefs.edit().putString(KEY_LAST_DEVICE, deviceName).apply()
    }
    
    fun getLastDevice(): String? {
        return prefs.getString(KEY_LAST_DEVICE, null)
    }
    
    fun clearSettings() {
        prefs.edit().clear().apply()
    }
}
