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
     * Optimize camera settings based on device capabilities
     */
    fun getOptimalCameraSettings(width: Int, height: Int): CameraSettings {
        val memoryInfo = getMemoryInfo()
        
        // Adjust settings based on memory availability and resolution
        val fps = when {
            width >= 3840 -> if (memoryInfo.maxMemoryMB >= 4096) 15 else 10 // 4K
            width >= 2560 -> if (memoryInfo.maxMemoryMB >= 3072) 24 else 15 // 2K
            width >= 1920 -> if (memoryInfo.maxMemoryMB >= 2048) 30 else 24 // 1080p
            else -> 30 // 720p and below
        }
        
        val bufferFrames = when {
            width >= 3840 -> 2 // Minimal buffering for 4K
            width >= 2560 -> 3 // Small buffer for 2K
            width >= 1920 -> 4 // Standard buffer for 1080p
            else -> 6 // Larger buffer for lower resolutions
        }
        
        return CameraSettings(
            fps = fps,
            bufferFrames = bufferFrames,
            useHardwareAcceleration = true,
            enableMemoryOptimization = width >= 1920
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
        val enableMemoryOptimization: Boolean
    )
}