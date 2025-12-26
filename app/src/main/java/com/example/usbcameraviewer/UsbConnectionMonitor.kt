package com.example.usbcameraviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Timer
import java.util.TimerTask

/**
 * Monitors USB camera connections and handles automatic permission renewal
 * Designed for 24/7 operation to prevent permission timeouts and connection drops
 */
class UsbConnectionMonitor(
    private val context: Context,
    private val permissionManager: UsbPermissionManager
) {
    
    companion object {
        private const val TAG = "UsbConnectionMonitor"
        private const val PERMISSION_CHECK_INTERVAL = 30000L // Check every 30 seconds
        private const val CONNECTION_TIMEOUT = 5000L // 5 seconds timeout for reconnection
    }
    
    private var isMonitoring = false
    private var permissionTimer: Timer? = null
    private var currentCamera: UsbDevice? = null
    private var connectionCallback: ((Boolean, UsbDevice?) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    
                    device?.let { usbDevice ->
                        if (isUvcCamera(usbDevice)) {
                            Log.d(TAG, "USB camera attached: ${usbDevice.deviceName}")
                            handleCameraAttached(usbDevice)
                        }
                    }
                }
                
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    
                    device?.let { usbDevice ->
                        if (isUvcCamera(usbDevice)) {
                            Log.d(TAG, "USB camera detached: ${usbDevice.deviceName}")
                            handleCameraDetached(usbDevice)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Start monitoring USB connections and permissions
     */
    fun startMonitoring(callback: (Boolean, UsbDevice?) -> Unit) {
        if (isMonitoring) return
        
        Log.d(TAG, "Starting USB connection monitoring for 24/7 operation")
        
        connectionCallback = callback
        isMonitoring = true
        
        // Register USB broadcast receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
        
        // Start periodic permission checking
        startPermissionMonitoring()
        
        // Check for already connected cameras
        checkExistingCameras()
    }
    
    /**
     * Stop monitoring USB connections
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        Log.d(TAG, "Stopping USB connection monitoring")
        
        isMonitoring = false
        connectionCallback = null
        
        // Stop permission monitoring
        permissionTimer?.cancel()
        permissionTimer = null
        
        // Unregister receiver
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering USB receiver: ${e.message}")
        }
    }
    
    /**
     * Start periodic permission checking to prevent timeouts
     */
    private fun startPermissionMonitoring() {
        permissionTimer = Timer("UsbPermissionMonitor", true)
        permissionTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkAndRenewPermissions()
            }
        }, PERMISSION_CHECK_INTERVAL, PERMISSION_CHECK_INTERVAL)
    }
    
    /**
     * Check and renew permissions for connected cameras
     */
    private fun checkAndRenewPermissions() {
        if (!isMonitoring) return
        
        val cameras = permissionManager.getConnectedCameras()
        
        for (camera in cameras) {
            val hasPermission = permissionManager.hasPermission(camera)
            
            if (!hasPermission && permissionManager.isAlwaysAllowEnabled()) {
                Log.d(TAG, "Permission lost for camera ${camera.deviceName}, attempting silent renewal")
                
                // Attempt silent permission renewal
                permissionManager.requestPermission(camera) { granted ->
                    if (granted) {
                        Log.d(TAG, "Successfully renewed permission for ${camera.deviceName}")
                        handler.post {
                            connectionCallback?.invoke(true, camera)
                        }
                    } else {
                        Log.w(TAG, "Failed to renew permission for ${camera.deviceName}")
                        handler.post {
                            connectionCallback?.invoke(false, camera)
                        }
                    }
                }
            } else if (hasPermission && currentCamera != camera) {
                // Update current camera reference
                currentCamera = camera
                Log.d(TAG, "Updated current camera to ${camera.deviceName}")
            }
        }
        
        // Check if current camera is still connected
        if (currentCamera != null && !cameras.contains(currentCamera)) {
            Log.w(TAG, "Current camera ${currentCamera?.deviceName} is no longer connected")
            currentCamera = null
            handler.post {
                connectionCallback?.invoke(false, null)
            }
        }
    }
    
    /**
     * Handle camera attached event
     */
    private fun handleCameraAttached(device: UsbDevice) {
        currentCamera = device
        
        if (permissionManager.isAlwaysAllowEnabled()) {
            // Auto-request permission if always allow is enabled
            permissionManager.requestPermission(device) { granted ->
                handler.post {
                    connectionCallback?.invoke(granted, device)
                }
            }
        } else {
            // Check if we already have permission
            val hasPermission = permissionManager.hasPermission(device)
            handler.post {
                connectionCallback?.invoke(hasPermission, device)
            }
        }
    }
    
    /**
     * Handle camera detached event
     */
    private fun handleCameraDetached(device: UsbDevice) {
        if (currentCamera == device) {
            currentCamera = null
        }
        
        handler.post {
            connectionCallback?.invoke(false, device)
        }
        
        // Schedule reconnection check
        handler.postDelayed({
            checkForReconnection(device)
        }, CONNECTION_TIMEOUT)
    }
    
    /**
     * Check for camera reconnection after brief disconnection
     */
    private fun checkForReconnection(originalDevice: UsbDevice) {
        val cameras = permissionManager.getConnectedCameras()
        
        // Look for a camera with the same vendor/product ID
        val reconnectedCamera = cameras.find { camera ->
            camera.vendorId == originalDevice.vendorId && 
            camera.productId == originalDevice.productId
        }
        
        if (reconnectedCamera != null) {
            Log.d(TAG, "Camera reconnected: ${reconnectedCamera.deviceName}")
            handleCameraAttached(reconnectedCamera)
        }
    }
    
    /**
     * Check for already connected cameras on startup
     */
    private fun checkExistingCameras() {
        val cameras = permissionManager.getConnectedCameras()
        
        if (cameras.isNotEmpty()) {
            val camera = cameras.first() // Use first available camera
            currentCamera = camera
            
            val hasPermission = permissionManager.hasPermission(camera)
            
            if (hasPermission) {
                Log.d(TAG, "Found existing camera with permission: ${camera.deviceName}")
                handler.post {
                    connectionCallback?.invoke(true, camera)
                }
            } else if (permissionManager.isAlwaysAllowEnabled()) {
                Log.d(TAG, "Found existing camera, requesting permission: ${camera.deviceName}")
                permissionManager.requestPermission(camera) { granted ->
                    handler.post {
                        connectionCallback?.invoke(granted, camera)
                    }
                }
            } else {
                Log.d(TAG, "Found existing camera without permission: ${camera.deviceName}")
                handler.post {
                    connectionCallback?.invoke(false, camera)
                }
            }
        }
    }
    
    /**
     * Force permission renewal for current camera
     */
    fun renewCurrentCameraPermission() {
        val camera = currentCamera ?: return
        
        Log.d(TAG, "Force renewing permission for current camera: ${camera.deviceName}")
        
        permissionManager.requestPermission(camera) { granted ->
            handler.post {
                connectionCallback?.invoke(granted, camera)
            }
        }
    }
    
    /**
     * Get current connected camera
     */
    fun getCurrentCamera(): UsbDevice? = currentCamera
    
    /**
     * Check if a USB device is a UVC camera
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
}