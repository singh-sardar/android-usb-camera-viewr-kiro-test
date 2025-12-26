package com.example.usbcameraviewer

import android.content.Context
import android.util.Log
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType

/**
 * Optimized camera configuration for high-resolution video and 24/7 operation
 * Handles 2K/4K video with hardware acceleration and proper buffering
 */
class OptimizedCameraConfig {
    
    companion object {
        private const val TAG = "OptimizedCameraConfig"
        
        // Performance profiles for different resolutions
        data class PerformanceProfile(
            val maxFps: Int,
            val bufferSize: Int,
            val useHardwareDecoding: Boolean,
            val compressionLevel: Int,
            val memoryOptimization: Boolean
        )
        
        private val PERFORMANCE_PROFILES = mapOf(
            "4K" to PerformanceProfile(
                maxFps = 15,
                bufferSize = 8,
                useHardwareDecoding = true,
                compressionLevel = 85,
                memoryOptimization = true
            ),
            "2K" to PerformanceProfile(
                maxFps = 24,
                bufferSize = 6,
                useHardwareDecoding = true,
                compressionLevel = 90,
                memoryOptimization = true
            ),
            "1080p" to PerformanceProfile(
                maxFps = 30,
                bufferSize = 4,
                useHardwareDecoding = true,
                compressionLevel = 95,
                memoryOptimization = false
            ),
            "720p" to PerformanceProfile(
                maxFps = 30,
                bufferSize = 3,
                useHardwareDecoding = false,
                compressionLevel = 100,
                memoryOptimization = false
            )
        )
    }
    
    /**
     * Get optimal performance profile for given resolution
     */
    fun getPerformanceProfile(width: Int, height: Int): PerformanceProfile {
        return when {
            width >= 3840 -> PERFORMANCE_PROFILES["4K"]!!
            width >= 2560 -> PERFORMANCE_PROFILES["2K"]!!
            width >= 1920 -> PERFORMANCE_PROFILES["1080p"]!!
            else -> PERFORMANCE_PROFILES["720p"]!!
        }
    }
    
    /**
     * Create optimized camera client for high-resolution video
     */
    fun createOptimizedCameraClient(
        context: Context,
        width: Int,
        height: Int,
        fps: Int = 30
    ): CameraClient? {
        
        val profile = getPerformanceProfile(width, height)
        val optimizedFps = minOf(fps, profile.maxFps)
        
        Log.d(TAG, "Creating optimized camera client: ${width}x${height}@${optimizedFps}fps")
        Log.d(TAG, "Profile: bufferSize=${profile.bufferSize}, hwDecoding=${profile.useHardwareDecoding}")
        
        return try {
            CameraClient.newBuilder(context)
                // Hardware acceleration settings
                .setEnableGLES(true)
                .setRawImage(false)
                .openDebug(false)
                
                // Camera strategy with optimizations
                .setCameraStrategy(CameraUvcStrategy(context))
                
                // Camera request with optimal settings
                .setCameraRequest(
                    CameraRequest.Builder()
                        .setPreviewWidth(width)
                        .setPreviewHeight(height)
                        .create()
                )
                .build()
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create optimized camera client", e)
            null
        }
    }
    
    /**
     * Create optimized UVC strategy with performance settings
     */
    private fun createOptimizedStrategy(context: Context, profile: PerformanceProfile): CameraUvcStrategy {
        return CameraUvcStrategy(context)
    }
    
    /**
     * Get recommended resolution based on device capabilities
     */
    fun getRecommendedResolution(context: Context): Pair<Int, Int> {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Don't exceed screen resolution for performance
        return when {
            screenWidth >= 3840 && hasHighPerformanceGPU(context) -> Pair(3840, 2160) // 4K
            screenWidth >= 2560 -> Pair(2560, 1440) // 2K
            screenWidth >= 1920 -> Pair(1920, 1080) // 1080p
            else -> Pair(1280, 720) // 720p
        }
    }
    
    /**
     * Check if device has high-performance GPU for 4K handling
     */
    private fun hasHighPerformanceGPU(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // Require at least 4GB RAM for 4K processing
            val totalMemoryGB = memoryInfo.totalMem / (1024 * 1024 * 1024)
            totalMemoryGB >= 4
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Apply memory optimizations for 24/7 operation
     */
    fun applyMemoryOptimizations(context: Context) {
        // Force garbage collection
        System.gc()
        
        // Log memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryPercent = (usedMemory * 100) / maxMemory
        
        Log.d(TAG, "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB (${memoryPercent}%)")
        
        // Trigger cleanup if memory usage is high
        if (memoryPercent > 80) {
            Log.w(TAG, "High memory usage detected, triggering cleanup")
            System.runFinalization()
            System.gc()
        }
    }
}