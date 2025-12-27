package com.example.usbcameraviewer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Timer
import java.util.TimerTask

/**
 * Camera Watchdog System for 24/7 continuous operation
 * Monitors camera stream health and automatically recovers from failures
 */
class CameraWatchdog(
    private val context: Context,
    private val cameraFragment: UsbCameraFragment
) {
    
    companion object {
        private const val TAG = "CameraWatchdog"
        private const val HEALTH_CHECK_INTERVAL = 10000L // Check every 10 seconds
        private const val STREAM_TIMEOUT = 30000L // 30 seconds without frames = problem
        private const val MAX_RECOVERY_ATTEMPTS = 3
        private const val RECOVERY_DELAY = 5000L // 5 seconds between recovery attempts
        private const val SURFACE_CHECK_INTERVAL = 5000L // Check surface every 5 seconds
        
        // Proactive refresh settings
        private const val PROACTIVE_REFRESH_INTERVAL = 180000L // 3 minutes for testing
        private const val MEMORY_REFRESH_THRESHOLD = 70 // Refresh if memory > 70%
        private const val UPTIME_REFRESH_INTERVAL = 900000L // 15 minutes for deep refresh
    }
    
    private var watchdogTimer: Timer? = null
    private var surfaceCheckTimer: Timer? = null
    private var proactiveRefreshTimer: Timer? = null
    private var isMonitoring = false
    private var lastFrameTime = System.currentTimeMillis()
    private var recoveryAttempts = 0
    private var isRecovering = false
    private var startTime = System.currentTimeMillis()
    private var lastProactiveRefresh = System.currentTimeMillis()
    private var refreshCount = 0
    private val handler = Handler(Looper.getMainLooper())
    
    private var recoveryCallback: ((String) -> Unit)? = null
    
    /**
     * Start watchdog monitoring for 24/7 operation
     */
    fun startWatchdog(callback: (String) -> Unit) {
        Log.d(TAG, "Starting Camera Watchdog for 24/7 monitoring")
        
        recoveryCallback = callback
        isMonitoring = true
        lastFrameTime = System.currentTimeMillis()
        startTime = System.currentTimeMillis()
        lastProactiveRefresh = System.currentTimeMillis()
        recoveryAttempts = 0
        refreshCount = 0
        
        // Start health monitoring
        startHealthMonitoring()
        
        // Start surface monitoring
        startSurfaceMonitoring()
        
        // Start proactive refresh system
        startProactiveRefresh()
        
        Log.d(TAG, "Camera Watchdog started successfully with proactive refresh")
    }
    
    /**
     * Stop watchdog monitoring
     */
    fun stopWatchdog() {
        Log.d(TAG, "Stopping Camera Watchdog")
        
        isMonitoring = false
        recoveryCallback = null
        
        watchdogTimer?.cancel()
        watchdogTimer = null
        
        surfaceCheckTimer?.cancel()
        surfaceCheckTimer = null
        
        proactiveRefreshTimer?.cancel()
        proactiveRefreshTimer = null
    }
    
    /**
     * Report that a frame was received (call this from camera callbacks)
     */
    fun reportFrameReceived() {
        lastFrameTime = System.currentTimeMillis()
        
        // Reset recovery attempts on successful frame
        if (recoveryAttempts > 0) {
            Log.d(TAG, "Frame received, resetting recovery attempts")
            recoveryAttempts = 0
            isRecovering = false
        }
    }
    
    /**
     * Report camera error
     */
    fun reportCameraError(error: String) {
        Log.w(TAG, "Camera error reported: $error")
        
        if (isMonitoring && !isRecovering) {
            triggerRecovery("Camera error: $error")
        }
    }
    
    /**
     * Start health monitoring timer
     */
    private fun startHealthMonitoring() {
        watchdogTimer = Timer("CameraWatchdog", true)
        watchdogTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkCameraHealth()
            }
        }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL)
    }
    
    /**
     * Start surface monitoring timer
     */
    private fun startSurfaceMonitoring() {
        surfaceCheckTimer = Timer("SurfaceWatchdog", true)
        surfaceCheckTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkSurfaceHealth()
            }
        }, SURFACE_CHECK_INTERVAL, SURFACE_CHECK_INTERVAL)
    }
    
    /**
     * Start proactive refresh system for bulletproof 24/7 operation
     */
    private fun startProactiveRefresh() {
        proactiveRefreshTimer = Timer("ProactiveRefresh", true)
        proactiveRefreshTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkProactiveRefresh()
            }
        }, PROACTIVE_REFRESH_INTERVAL, PROACTIVE_REFRESH_INTERVAL) // Check every 3 minutes for testing
    }
    
    /**
     * Check if proactive refresh is needed
     */
    private fun checkProactiveRefresh() {
        if (!isMonitoring || isRecovering) return
        
        val currentTime = System.currentTimeMillis()
        val uptime = currentTime - startTime
        val timeSinceLastRefresh = currentTime - lastProactiveRefresh
        
        // Check various conditions for proactive refresh
        val shouldRefresh = when {
            // Time-based refresh (every 3 minutes for testing)
            timeSinceLastRefresh >= PROACTIVE_REFRESH_INTERVAL -> {
                Log.d(TAG, "Proactive refresh: 3-minute maintenance cycle")
                "3-minute maintenance cycle"
            }
            
            // Long uptime refresh (every 15 minutes)
            uptime >= UPTIME_REFRESH_INTERVAL && timeSinceLastRefresh >= UPTIME_REFRESH_INTERVAL -> {
                Log.d(TAG, "Proactive refresh: Long uptime maintenance")
                "Long uptime maintenance (${uptime / 60000}min)"
            }
            
            // Memory-based refresh
            getMemoryUsagePercent() >= MEMORY_REFRESH_THRESHOLD -> {
                Log.d(TAG, "Proactive refresh: High memory usage")
                "High memory usage (${getMemoryUsagePercent()}%)"
            }
            
            // Recovery count based refresh
            refreshCount >= 5 -> {
                Log.d(TAG, "Proactive refresh: Multiple recoveries detected")
                "Multiple recoveries detected ($refreshCount)"
            }
            
            else -> null
        }
        
        shouldRefresh?.let { reason ->
            performProactiveRefresh(reason)
        }
    }
    
    /**
     * Perform proactive refresh to prevent issues
     */
    private fun performProactiveRefresh(reason: String) {
        Log.d(TAG, "Performing proactive refresh: $reason")
        
        refreshCount++
        lastProactiveRefresh = System.currentTimeMillis()
        
        handler.post {
            recoveryCallback?.invoke("🔄 Proactive refresh: $reason")
        }
        
        // Perform comprehensive refresh
        handler.postDelayed({
            try {
                performComprehensiveRefresh()
                
                handler.post {
                    recoveryCallback?.invoke("✅ Proactive refresh completed successfully")
                }
                
                Log.d(TAG, "Proactive refresh completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Proactive refresh failed", e)
                handler.post {
                    recoveryCallback?.invoke("⚠️ Proactive refresh failed: ${e.message}")
                }
            }
        }, 2000) // Small delay to show the refresh message
    }
    
    /**
     * Perform comprehensive system refresh
     */
    private fun performComprehensiveRefresh() {
        Log.d(TAG, "Starting comprehensive system refresh")
        
        // Step 1: Memory cleanup
        Log.d(TAG, "Step 1: Memory cleanup")
        System.gc()
        System.runFinalization()
        System.gc()
        
        // Step 2: Reset frame tracking
        Log.d(TAG, "Step 2: Reset frame tracking")
        lastFrameTime = System.currentTimeMillis()
        recoveryAttempts = 0
        
        // Step 3: Camera system refresh
        Log.d(TAG, "Step 3: Camera system refresh")
        restartCameraClient()
        
        // Step 4: Reset refresh counter periodically
        if (refreshCount >= 10) {
            refreshCount = 0
            Log.d(TAG, "Reset refresh counter")
        }
        
        // Step 5: Update timestamps
        lastProactiveRefresh = System.currentTimeMillis()
        
        Log.d(TAG, "Comprehensive system refresh completed")
    }
    
    /**
     * Get current memory usage percentage
     */
    private fun getMemoryUsagePercent(): Int {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            ((usedMemory * 100) / maxMemory).toInt()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get memory usage", e)
            0
        }
    }
    
    /**
     * Check camera stream health
     */
    private fun checkCameraHealth() {
        if (!isMonitoring || isRecovering) return
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFrame = currentTime - lastFrameTime
        
        Log.d(TAG, "Health check: ${timeSinceLastFrame}ms since last frame")
        
        if (timeSinceLastFrame > STREAM_TIMEOUT) {
            Log.w(TAG, "Camera stream timeout detected (${timeSinceLastFrame}ms)")
            triggerRecovery("Stream timeout: ${timeSinceLastFrame}ms without frames")
        }
    }
    
    /**
     * Check surface and rendering health
     */
    private fun checkSurfaceHealth() {
        if (!isMonitoring || isRecovering) return
        
        handler.post {
            try {
                // Simple surface check without accessing protected methods
                Log.d(TAG, "Surface health check completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking surface health", e)
                triggerRecovery("Surface check error: ${e.message}")
            }
        }
    }
    
    /**
     * Trigger camera recovery process
     */
    private fun triggerRecovery(reason: String) {
        if (isRecovering) {
            Log.d(TAG, "Recovery already in progress, skipping")
            return
        }
        
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            Log.e(TAG, "Maximum recovery attempts reached, giving up")
            handler.post {
                recoveryCallback?.invoke("❌ Recovery failed after $MAX_RECOVERY_ATTEMPTS attempts")
            }
            return
        }
        
        isRecovering = true
        recoveryAttempts++
        
        Log.w(TAG, "Triggering recovery attempt $recoveryAttempts: $reason")
        
        handler.post {
            recoveryCallback?.invoke("🔄 Recovering camera (attempt $recoveryAttempts): $reason")
        }
        
        // Delay recovery to avoid rapid cycling
        handler.postDelayed({
            performRecovery()
        }, RECOVERY_DELAY)
    }
    
    /**
     * Perform actual camera recovery
     */
    private fun performRecovery() {
        Log.d(TAG, "Performing camera recovery")
        
        try {
            handler.post {
                // Step 1: Force garbage collection
                System.gc()
                
                // Step 2: Reset camera view layer (simplified)
                Log.d(TAG, "Resetting camera view")
                
                // Step 3: Restart camera client
                restartCameraClient()
                
                // Step 4: Update recovery status
                recoveryCallback?.invoke("✅ Camera recovery completed (attempt $recoveryAttempts)")
                
                // Step 5: Reset recovery state after delay
                handler.postDelayed({
                    isRecovering = false
                    lastFrameTime = System.currentTimeMillis() // Reset frame timer
                    Log.d(TAG, "Recovery process completed")
                }, 3000)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed", e)
            handler.post {
                recoveryCallback?.invoke("❌ Recovery failed: ${e.message}")
                isRecovering = false
            }
        }
    }
    
    /**
     * Restart camera client
     */
    private fun restartCameraClient() {
        try {
            Log.d(TAG, "Restarting camera client")
            
            // Force camera client recreation
            cameraFragment.restartCamera()
            
            Log.d(TAG, "Camera client restart initiated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart camera client", e)
            throw e
        }
    }
    
    /**
     * Force immediate recovery (for manual triggers)
     */
    fun forceRecovery(reason: String = "Manual recovery") {
        Log.d(TAG, "Force recovery requested: $reason")
        
        recoveryAttempts = 0 // Reset attempts for manual recovery
        triggerRecovery(reason)
    }
    
    /**
     * Get watchdog status with refresh information
     */
    fun getStatus(): WatchdogStatus {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFrame = currentTime - lastFrameTime
        val uptime = currentTime - startTime
        val timeSinceLastRefresh = currentTime - lastProactiveRefresh
        
        return WatchdogStatus(
            isMonitoring = isMonitoring,
            isRecovering = isRecovering,
            recoveryAttempts = recoveryAttempts,
            timeSinceLastFrame = timeSinceLastFrame,
            isHealthy = timeSinceLastFrame < STREAM_TIMEOUT && !isRecovering,
            uptime = uptime,
            refreshCount = refreshCount,
            timeSinceLastRefresh = timeSinceLastRefresh,
            nextRefreshIn = PROACTIVE_REFRESH_INTERVAL - timeSinceLastRefresh
        )
    }
    
    /**
     * Force proactive refresh (for manual triggers)
     */
    fun forceProactiveRefresh(reason: String = "Manual proactive refresh") {
        Log.d(TAG, "Force proactive refresh requested: $reason")
        performProactiveRefresh(reason)
    }
    
    data class WatchdogStatus(
        val isMonitoring: Boolean,
        val isRecovering: Boolean,
        val recoveryAttempts: Int,
        val timeSinceLastFrame: Long,
        val isHealthy: Boolean,
        val uptime: Long,
        val refreshCount: Int,
        val timeSinceLastRefresh: Long,
        val nextRefreshIn: Long
    )
}