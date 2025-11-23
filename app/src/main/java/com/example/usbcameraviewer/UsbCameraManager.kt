package com.example.usbcameraviewer

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CameraConfig(
    val deviceName: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

data class CameraResolution(val width: Int, val height: Int) {
    override fun toString() = "${width}x${height}"
}

class UsbCameraManager(private val context: Context) {
    
    private val TAG = "UsbCameraManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Disconnected)
    val cameraState: StateFlow<CameraState> = _cameraState
    
    private val _availableDevices = MutableStateFlow<List<UsbDevice>>(emptyList())
    val availableDevices: StateFlow<List<UsbDevice>> = _availableDevices
    
    private val _config = MutableStateFlow(CameraConfig())
    val config: StateFlow<CameraConfig> = _config
    
    private var currentDevice: UsbDevice? = null
    private var retryJob: Job? = null
    private var isRunning = false
    private var nativeCamera: NativeUsbCamera? = null
    private var currentSurface: Surface? = null
    
    sealed class CameraState {
        object Disconnected : CameraState()
        object Connecting : CameraState()
        data class Connected(val device: UsbDevice) : CameraState()
        data class Error(val message: String) : CameraState()
    }
    
    fun updateConfig(newConfig: CameraConfig) {
        _config.value = newConfig
        if (isRunning) {
            scope.launch {
                restartCamera()
            }
        }
    }
    
    fun scanDevices() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.values.filter { isWebcam(it) }
        _availableDevices.value = devices
        Log.d(TAG, "Found ${devices.size} USB camera devices")
    }
    
    private fun isWebcam(device: UsbDevice): Boolean {
        // USB Video Class (UVC) devices have class 14 (0x0E)
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == 14) { // USB_CLASS_VIDEO
                return true
            }
        }
        return false
    }
    
    fun startCamera(surface: Surface) {
        isRunning = true
        scope.launch {
            connectToCamera(surface)
        }
    }
    
    private suspend fun connectToCamera(surface: Surface) {
        withContext(Dispatchers.IO) {
            try {
                _cameraState.value = CameraState.Connecting
                currentSurface = surface
                
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val devices = usbManager.deviceList.values.filter { isWebcam(it) }
                
                if (devices.isEmpty()) {
                    _cameraState.value = CameraState.Error("No USB camera found")
                    scheduleRetry(surface)
                    return@withContext
                }
                
                val device = devices.firstOrNull { it.deviceName == _config.value.deviceName }
                    ?: devices.first()
                
                currentDevice = device
                
                // Initialize native camera
                nativeCamera = NativeUsbCamera(context).apply {
                    onError = { error ->
                        _cameraState.value = CameraState.Error(error)
                    }
                }
                if (!nativeCamera!!.open(device)) {
                    _cameraState.value = CameraState.Error("Failed to open camera")
                    scheduleRetry(surface)
                    return@withContext
                }
                
                // Start streaming
                val config = _config.value
                if (!nativeCamera!!.startStreaming(surface, config.width, config.height, config.fps)) {
                    _cameraState.value = CameraState.Error("Failed to start streaming")
                    nativeCamera?.close()
                    nativeCamera = null
                    scheduleRetry(surface)
                    return@withContext
                }
                
                _cameraState.value = CameraState.Connected(device)
                Log.d(TAG, "Camera connected: ${device.deviceName}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to camera", e)
                _cameraState.value = CameraState.Error(e.message ?: "Unknown error")
                nativeCamera?.close()
                nativeCamera = null
                scheduleRetry(surface)
            }
        }
    }
    
    private fun scheduleRetry(surface: Surface) {
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(15000) // 15 seconds
            if (isRunning && _cameraState.value is CameraState.Error || _cameraState.value is CameraState.Disconnected) {
                Log.d(TAG, "Retrying camera connection...")
                connectToCamera(surface)
            }
        }
    }
    
    private suspend fun restartCamera() {
        Log.d(TAG, "Restarting camera with new config")
        nativeCamera?.stopStreaming()
        currentSurface?.let { surface ->
            val config = _config.value
            nativeCamera?.startStreaming(surface, config.width, config.height, config.fps)
        }
    }
    
    fun stopCamera() {
        isRunning = false
        retryJob?.cancel()
        nativeCamera?.close()
        nativeCamera = null
        currentDevice = null
        currentSurface = null
        _cameraState.value = CameraState.Disconnected
    }
    
    fun getSupportedResolutions(): List<CameraResolution> {
        // Try to get from camera, fallback to defaults
        val formats = nativeCamera?.getSupportedFormats() ?: emptyList()
        
        if (formats.isNotEmpty()) {
            // Get unique resolutions from camera
            return formats.map { CameraResolution(it.width, it.height) }
                .distinctBy { "${it.width}x${it.height}" }
                .sortedByDescending { it.width * it.height }
        }
        
        // Default resolutions if camera not available
        return listOf(
            CameraResolution(3840, 2160), // 4K
            CameraResolution(1920, 1080), // 1080p
            CameraResolution(1280, 720),  // 720p
            CameraResolution(640, 480)    // VGA
        )
    }
    
    fun getSupportedFps(): List<Int> {
        // Try to get from camera, fallback to defaults
        val formats = nativeCamera?.getSupportedFormats() ?: emptyList()
        
        if (formats.isNotEmpty()) {
            // Get unique FPS values from camera
            return formats.map { it.fps }
                .distinct()
                .sortedDescending()
        }
        
        // Default FPS if camera not available
        return listOf(60, 30, 24, 15)
    }
    
    fun applyCameraControls(controls: CameraControls) {
        nativeCamera?.apply {
            setBrightness(controls.brightness)
            setContrast(controls.contrast)
            setSaturation(controls.saturation)
            setAutoFocus(controls.autoFocus)
            setAutoWhiteBalance(controls.autoWhiteBalance)
        }
    }
    
    fun resetCameraControls() {
        nativeCamera?.resetControls()
    }
    
    fun release() {
        stopCamera()
        scope.cancel()
    }
}
