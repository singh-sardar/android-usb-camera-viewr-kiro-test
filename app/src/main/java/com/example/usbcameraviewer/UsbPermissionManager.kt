package com.example.usbcameraviewer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * Manages USB permissions and prevents repeated permission dialogs
 * Stores user preferences to avoid asking repeatedly for the same device
 */
class UsbPermissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbPermissionManager"
        private const val USB_PERMISSION = "com.example.usbcameraviewer.USB_PERMISSION"
        private const val PREFS_NAME = "usb_permissions"
        private const val PREF_ALWAYS_ALLOW = "always_allow_usb"
    }
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        
                        if (granted && device != null) {
                            Log.d(TAG, "USB permission granted for device: ${device.deviceName}")
                            // Save permission preference to avoid asking again
                            savePermissionPreference(device, true)
                            permissionCallback?.invoke(true)
                        } else {
                            Log.d(TAG, "USB permission denied")
                            permissionCallback?.invoke(false)
                        }
                    }
                }
            }
        }
    }
    
    init {
        // Register the broadcast receiver
        val filter = IntentFilter(USB_PERMISSION)
        context.registerReceiver(usbReceiver, filter)
    }
    
    /**
     * Check if we should always allow USB cameras without asking
     */
    fun isAlwaysAllowEnabled(): Boolean {
        return prefs.getBoolean(PREF_ALWAYS_ALLOW, false)
    }
    
    /**
     * Set the always allow preference
     */
    fun setAlwaysAllow(allow: Boolean) {
        prefs.edit().putBoolean(PREF_ALWAYS_ALLOW, allow).apply()
        Log.d(TAG, "Always allow USB cameras set to: $allow")
    }
    
    /**
     * Check if permission is already granted for a specific device
     */
    fun hasPermission(device: UsbDevice): Boolean {
        val hasSystemPermission = usbManager.hasPermission(device)
        val hasStoredPermission = getStoredPermission(device)
        
        Log.d(TAG, "Device ${device.deviceName}: system=$hasSystemPermission, stored=$hasStoredPermission")
        
        return hasSystemPermission || (hasStoredPermission && isAlwaysAllowEnabled())
    }
    
    /**
     * Request permission for a USB device
     * Will skip dialog if user previously chose "always allow"
     */
    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit) {
        // Check if we already have permission
        if (hasPermission(device)) {
            Log.d(TAG, "Permission already granted for device: ${device.deviceName}")
            callback(true)
            return
        }
        
        // Check if user previously chose "always allow"
        if (isAlwaysAllowEnabled() && getStoredPermission(device)) {
            Log.d(TAG, "Auto-granting permission due to always allow preference")
            callback(true)
            return
        }
        
        // Need to request permission
        Log.d(TAG, "Requesting USB permission for device: ${device.deviceName}")
        permissionCallback = callback
        
        val permissionIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            Intent(USB_PERMISSION), 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        usbManager.requestPermission(device, permissionIntent)
    }
    
    /**
     * Get all connected USB camera devices
     */
    fun getConnectedCameras(): List<UsbDevice> {
        val cameras = mutableListOf<UsbDevice>()
        
        usbManager.deviceList.values.forEach { device ->
            if (isUvcCamera(device)) {
                cameras.add(device)
            }
        }
        
        Log.d(TAG, "Found ${cameras.size} USB cameras")
        return cameras
    }
    
    /**
     * Check if a USB device is a UVC (USB Video Class) camera
     */
    private fun isUvcCamera(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == 14) { // USB_CLASS_VIDEO
                return true
            }
        }
        return false
    }
    
    /**
     * Save permission preference for a specific device
     */
    private fun savePermissionPreference(device: UsbDevice, granted: Boolean) {
        val deviceKey = getDeviceKey(device)
        prefs.edit().putBoolean(deviceKey, granted).apply()
        Log.d(TAG, "Saved permission preference for $deviceKey: $granted")
    }
    
    /**
     * Get stored permission preference for a specific device
     */
    private fun getStoredPermission(device: UsbDevice): Boolean {
        val deviceKey = getDeviceKey(device)
        return prefs.getBoolean(deviceKey, false)
    }
    
    /**
     * Generate a unique key for a USB device
     */
    private fun getDeviceKey(device: UsbDevice): String {
        return "device_${device.vendorId}_${device.productId}_${device.deviceName}"
    }
    
    /**
     * Clear all stored permissions (for settings/reset)
     */
    fun clearAllPermissions() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all USB permission preferences")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering USB receiver: ${e.message}")
        }
    }
}