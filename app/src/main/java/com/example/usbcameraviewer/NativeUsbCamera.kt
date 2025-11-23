package com.example.usbcameraviewer

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.*
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame

/**
 * Native USB Camera implementation using JavaCV/FFmpeg
 * Directly accesses USB cameras via V4L2 (/dev/video*)
 */
class NativeUsbCamera(private val context: Context) {
    
    private val TAG = "NativeUsbCamera"
    private var device: UsbDevice? = null
    private var isStreaming = false
    private var currentSurface: Surface? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var grabberJob: Job? = null
    private var frameGrabber: FFmpegFrameGrabber? = null
    private val converter = AndroidFrameConverter()
    
    // Error callback
    var onError: ((String) -> Unit)? = null
    
    // Camera controls
    private var brightness = 0
    private var contrast = 0
    private var saturation = 0
    
    fun open(device: UsbDevice): Boolean {
        try {
            this.device = device
            
            AppLogger.i(TAG, "USB Device: ${device.productName ?: device.deviceName}")
            AppLogger.i(TAG, "VID: ${device.vendorId}, PID: ${device.productId}")
            
            // Find V4L2 device
            val videoDevice = findVideoDevice()
            if (videoDevice == null) {
                val error = "No /dev/video* device found"
                AppLogger.e(TAG, error)
                onError?.invoke(error)
                return false
            }
            
            AppLogger.i(TAG, "Found video device: $videoDevice")
            
            try {
                frameGrabber = FFmpegFrameGrabber(videoDevice).apply {
                    format = "v4l2"
                    imageWidth = 640
                    imageHeight = 480
                    frameRate = 30.0
                }
                
                AppLogger.i(TAG, "Initializing frame grabber...")
                frameGrabber?.start()
                AppLogger.i(TAG, "✓ Frame grabber started successfully!")
                
                return true
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error starting frame grabber", e)
                onError?.invoke("Error: ${e.message}")
                return false
            }
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error opening camera", e)
            onError?.invoke("Error: ${e.message}")
            return false
        }
    }
    
    private fun findVideoDevice(): String? {
        // Try common video device paths
        val devices = listOf(
            "/dev/video0",
            "/dev/video1",
            "/dev/video2",
            "/dev/video3"
        )
        
        for (dev in devices) {
            val file = java.io.File(dev)
            if (file.exists()) {
                AppLogger.d(TAG, "Found device: $dev")
                return dev
            }
        }
        
        return null
    }
    
    fun startStreaming(surface: Surface, width: Int, height: Int, fps: Int): Boolean {
        try {
            currentSurface = surface
            isStreaming = true
            
            if (frameGrabber == null) {
                AppLogger.w(TAG, "Frame grabber not initialized")
                return false
            }
            
            // Update settings
            try {
                frameGrabber?.imageWidth = width
                frameGrabber?.imageHeight = height
                frameGrabber?.frameRate = fps.toDouble()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Could not update grabber settings: ${e.message}")
            }
            
            // Start frame capture loop
            grabberJob = scope.launch {
                captureFrames(surface)
            }
            
            AppLogger.i(TAG, "Started streaming ${width}x${height}@${fps}fps")
            return true
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error starting stream", e)
            return false
        }
    }
    
    private suspend fun captureFrames(surface: Surface) = withContext(Dispatchers.Default) {
        AppLogger.i(TAG, "Starting frame capture loop...")
        
        while (isStreaming && isActive) {
            try {
                val frame: Frame? = frameGrabber?.grab()
                
                if (frame != null && frame.image != null) {
                    // Convert frame to bitmap
                    val bitmap: Bitmap? = converter.convert(frame)
                    
                    if (bitmap != null) {
                        // Draw to surface
                        val canvas = surface.lockCanvas(null)
                        canvas?.let {
                            it.drawBitmap(bitmap, 0f, 0f, null)
                            surface.unlockCanvasAndPost(it)
                        }
                    }
                }
                
                delay(33) // ~30fps
                
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    AppLogger.e(TAG, "Error capturing frame", e)
                    delay(1000) // Wait before retry
                }
            }
        }
        
        AppLogger.i(TAG, "Frame capture loop ended")
    }
    
    fun stopStreaming() {
        try {
            isStreaming = false
            grabberJob?.cancel()
            grabberJob = null
            currentSurface = null
            AppLogger.d(TAG, "Stopped streaming")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping stream", e)
        }
    }
    
    fun close() {
        try {
            stopStreaming()
            frameGrabber?.stop()
            frameGrabber?.release()
            frameGrabber = null
            device = null
            scope.cancel()
            AppLogger.d(TAG, "Closed camera")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error closing camera", e)
        }
    }
    
    fun getSupportedFormats(): List<CameraFormat> {
        return listOf(
            CameraFormat(1920, 1080, 30, "MJPEG"),
            CameraFormat(1280, 720, 30, "MJPEG"),
            CameraFormat(640, 480, 30, "MJPEG")
        )
    }
    
    fun isOpen(): Boolean = frameGrabber != null
    
    fun isStreaming(): Boolean = isStreaming
    
    fun setBrightness(value: Int) {
        brightness = value
        AppLogger.d(TAG, "Set brightness: $value")
    }
    
    fun setContrast(value: Int) {
        contrast = value
        AppLogger.d(TAG, "Set contrast: $value")
    }
    
    fun setSaturation(value: Int) {
        saturation = value
        AppLogger.d(TAG, "Set saturation: $value")
    }
    
    fun setAutoFocus(enabled: Boolean) {
        AppLogger.d(TAG, "Set auto focus: $enabled")
    }
    
    fun setAutoWhiteBalance(enabled: Boolean) {
        AppLogger.d(TAG, "Set auto white balance: $enabled")
    }
    
    fun resetControls() {
        brightness = 0
        contrast = 0
        saturation = 0
        AppLogger.d(TAG, "Reset camera controls")
    }
    
    data class CameraFormat(
        val width: Int,
        val height: Int,
        val fps: Int,
        val format: String
    )
}
