package com.example.usbcameraviewer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Timer
import java.util.TimerTask

/**
 * Performance optimizer for 24/7 camera operation
 * Handles memory management, garbage collection, and performance monitoring
 */
class PerformanceOptimizer(private val context: Context) {
    
    companion object {
        private const val TAG = "PerformanceOptimizer"
        private const val MEMORY_CHECK_INTERVAL = 30000L // 30 seconds
        private const val GC_THRESHOLD = 75 // Trigger GC at 75% memory usage
        private const val CRITICAL_THRESHOLD = 85 // Critical memory level
    }
    
    private var memoryTimer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isOptimizing = false
    
    /**
     * Start performance monitoring for 24/7 operation
     */
    fun startOptimization() {
        Log.d(TAG, "Starting performance optimization for 24/7 operation")
        
        memoryTimer = Timer("MemoryOptimizer", true)
        memoryTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                optimizeMemory()
            }
        }, MEMORY_CHECK_INTERVAL, MEMORY_CHECK_INTERVAL)
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopOptimization() {
        Log.d(TAG, "Stopping performance optimization")
        memoryTimer?.cancel()
        memoryTimer = null
    }
    
    /**
     * Optimize memory usage and prevent leaks
     */
    private fun optimizeMemory() {
        if (isOptimizing) return
        
        isOptimizing = true
        
        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryPercent = (usedMemory * 100) / maxMemory
            
            Log.d(TAG, "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB (${memoryPercent}%)")
            
            when {
                memoryPercent >= CRITICAL_THRESHOLD -> {
                    Log.w(TAG, "Critical memory usage detected (${memoryPercent}%), performing aggressive cleanup")
                    performAggressiveCleanup()
                }
                memoryPercent >= GC_THRESHOLD -> {
                    Log.i(TAG, "High memory usage detected (${memoryPercent}%), performing cleanup")
                    performStandardCleanup()
                }
                else -> {
                    Log.d(TAG, "Memory usage normal (${memoryPercent}%)")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory optimization", e)
        } finally {
            isOptimizing = false
        }
    }
    
    /**
     * Perform standard memory cleanup
     */
    private fun performStandardCleanup() {
        // Suggest garbage collection
        System.gc()
        
        // Run finalization
        System.runFinalization()
        
        Log.d(TAG, "Standard cleanup completed")
    }
    
    /**
     * Perform aggressive memory cleanup for critical situations
     */
    private fun performAggressiveCleanup() {
        // Multiple GC cycles for thorough cleanup
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }
        
        // Run finalization
        System.runFinalization()
        
        // Additional cleanup
        System.gc()
        
        Log.w(TAG, "Aggressive cleanup completed")
    }
    
    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryPercent = (usedMemory * 100) / maxMemory
        
        return MemoryInfo(
            usedMemoryMB = (usedMemory / 1024 / 1024).toInt(),
            maxMemoryMB = (maxMemory / 1024 / 1024).toInt(),
            memoryPercent = memoryPercent.toInt()
        )
    }
    
    /**
     * Optimize camera settings based on device capabilities and current performance
     */
    fun getOptimalCameraSettings(width: Int, height: Int): CameraSettings {
        val memoryInfo = getMemoryInfo()
        val deviceCapability = assessDeviceCapability()
        
        // Enhanced FPS calculation based on device performance and current load
        val fps = calculateOptimalFPS(width, height, memoryInfo, deviceCapability)
        
        // Dynamic buffer sizing for better quality
        val bufferFrames = calculateOptimalBuffer(width, height, memoryInfo)
        
        // Quality enhancement settings
        val qualitySettings = getQualityEnhancements(width, height, deviceCapability)
        
        return CameraSettings(
            fps = fps,
            bufferFrames = bufferFrames,
            useHardwareAcceleration = true,
            enableMemoryOptimization = width >= 1920,
            qualityMode = qualitySettings.qualityMode,
            enableImageStabilization = qualitySettings.enableImageStabilization,
            compressionLevel = qualitySettings.compressionLevel
        )
    }
    
    /**
     * Calculate optimal FPS based on resolution and device performance
     */
    private fun calculateOptimalFPS(width: Int, height: Int, memoryInfo: MemoryInfo, capability: DeviceCapability): Int {
        val baseFPS = when {
            width >= 3840 -> { // 4K
                when (capability) {
                    DeviceCapability.HIGH_END -> 20    // Better quality for high-end devices
                    DeviceCapability.MID_RANGE -> 15   // Balanced
                    DeviceCapability.LOW_END -> 12     // Conservative
                }
            }
            width >= 2560 -> { // 2K
                when (capability) {
                    DeviceCapability.HIGH_END -> 30    // Full quality
                    DeviceCapability.MID_RANGE -> 25   // Good quality
                    DeviceCapability.LOW_END -> 20     // Stable quality
                }
            }
            width >= 1920 -> { // 1080p
                when (capability) {
                    DeviceCapability.HIGH_END -> 30    // Full quality
                    DeviceCapability.MID_RANGE -> 30   // Full quality
                    DeviceCapability.LOW_END -> 25     // Good quality
                }
            }
            else -> 30 // 720p and below - always full quality
        }
        
        // Adjust based on current memory usage
        return if (memoryInfo.memoryPercent > 70) {
            (baseFPS * 0.8).toInt() // Reduce FPS if memory is high
        } else {
            baseFPS
        }
    }
    
    /**
     * Calculate optimal buffer size for smooth playback
     */
    private fun calculateOptimalBuffer(width: Int, height: Int, memoryInfo: MemoryInfo): Int {
        val baseBuffer = when {
            width >= 3840 -> 3 // Increased from 2 for better 4K quality
            width >= 2560 -> 4 // Increased from 3 for better 2K quality
            width >= 1920 -> 5 // Increased from 4 for better 1080p quality
            else -> 6
        }
        
        // Adjust based on available memory
        return if (memoryInfo.memoryPercent > 75) {
            maxOf(2, baseBuffer - 1) // Reduce buffer if memory is tight
        } else {
            baseBuffer
        }
    }
    
    /**
     * Get quality enhancement settings
     */
    private fun getQualityEnhancements(width: Int, height: Int, capability: DeviceCapability): QualitySettings {
        return when (capability) {
            DeviceCapability.HIGH_END -> QualitySettings(
                qualityMode = "HIGH",
                enableImageStabilization = true,
                compressionLevel = 95 // Minimal compression for best quality
            )
            DeviceCapability.MID_RANGE -> QualitySettings(
                qualityMode = "BALANCED",
                enableImageStabilization = width < 3840, // Enable for resolutions below 4K
                compressionLevel = 90 // Good quality compression
            )
            DeviceCapability.LOW_END -> QualitySettings(
                qualityMode = "STABLE",
                enableImageStabilization = false,
                compressionLevel = 85 // More compression for stability
            )
        }
    }
    
    /**
     * Assess device capability for optimal settings
     */
    private fun assessDeviceCapability(): DeviceCapability {
        val memoryInfo = getMemoryInfo()
        val totalMemoryGB = memoryInfo.maxMemoryMB / 1024.0
        
        return when {
            totalMemoryGB >= 6.0 -> DeviceCapability.HIGH_END    // 6GB+ RAM
            totalMemoryGB >= 4.0 -> DeviceCapability.MID_RANGE  // 4-6GB RAM
            else -> DeviceCapability.LOW_END                     // <4GB RAM
        }
    }
    
    /**
     * Get enhanced 24/7 settings with better quality
     */
    fun getEnhanced24x7Settings(width: Int, height: Int): CameraSettings {
        val memoryInfo = getMemoryInfo()
        val capability = assessDeviceCapability()
        
        // Enhanced 24/7 settings with better quality balance
        val fps = when {
            width >= 3840 -> { // 4K
                when (capability) {
                    DeviceCapability.HIGH_END -> 18    // Improved from 10
                    DeviceCapability.MID_RANGE -> 15   // Improved from 10
                    DeviceCapability.LOW_END -> 12     // Improved from 10
                }
            }
            width >= 2560 -> { // 2K
                when (capability) {
                    DeviceCapability.HIGH_END -> 25    // Improved from 15
                    DeviceCapability.MID_RANGE -> 22   // Improved from 15
                    DeviceCapability.LOW_END -> 18     // Improved from 15
                }
            }
            width >= 1920 -> { // 1080p
                when (capability) {
                    DeviceCapability.HIGH_END -> 30    // Improved from 24
                    DeviceCapability.MID_RANGE -> 28   // Improved from 24
                    DeviceCapability.LOW_END -> 25     // Improved from 24
                }
            }
            else -> 30 // 720p - full quality
        }
        
        return CameraSettings(
            fps = fps,
            bufferFrames = calculateOptimalBuffer(width, height, memoryInfo),
            useHardwareAcceleration = true,
            enableMemoryOptimization = true,
            qualityMode = "ENHANCED_24x7",
            enableImageStabilization = capability != DeviceCapability.LOW_END,
            compressionLevel = when (capability) {
                DeviceCapability.HIGH_END -> 92
                DeviceCapability.MID_RANGE -> 88
                DeviceCapability.LOW_END -> 85
            }
        )
    }
    
    /**
     * Check if device can handle high-resolution video
     */
    fun canHandle4K(): Boolean {
        val memoryInfo = getMemoryInfo()
        return memoryInfo.maxMemoryMB >= 3072 // Require at least 3GB for 4K
    }
    
    fun canHandle2K(): Boolean {
        val memoryInfo = getMemoryInfo()
        return memoryInfo.maxMemoryMB >= 2048 // Require at least 2GB for 2K
    }
    
    /**
     * Data classes for memory and camera settings
     */
    data class MemoryInfo(
        val usedMemoryMB: Int,
        val maxMemoryMB: Int,
        val memoryPercent: Int
    )
    
    data class CameraSettings(
        val fps: Int,
        val bufferFrames: Int,
        val useHardwareAcceleration: Boolean,
        val enableMemoryOptimization: Boolean,
        val qualityMode: String = "STANDARD",
        val enableImageStabilization: Boolean = false,
        val compressionLevel: Int = 90
    )
    
    data class QualitySettings(
        val qualityMode: String,
        val enableImageStabilization: Boolean,
        val compressionLevel: Int
    )
    
    enum class DeviceCapability {
        HIGH_END,    // 6GB+ RAM, high-performance devices
        MID_RANGE,   // 4-6GB RAM, balanced devices
        LOW_END      // <4GB RAM, conservative settings
    }
}