package com.example.usbcameraviewer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Timer
import java.util.TimerTask

/**
 * Ultimate 24/7 optimization system for maximum quality with rock-solid stability
 * Implements advanced performance monitoring, adaptive quality, and predictive optimization
 */
class Ultimate24x7Optimizer(private val context: Context) {
    
    companion object {
        private const val TAG = "Ultimate24x7Optimizer"
        private const val PERFORMANCE_CHECK_INTERVAL = 15000L // Check every 15 seconds
        private const val QUALITY_ADJUSTMENT_THRESHOLD = 5 // Adjust after 5 consecutive issues
        private const val MEMORY_CRITICAL_THRESHOLD = 85
        private const val MEMORY_WARNING_THRESHOLD = 75
        private const val FRAME_DROP_THRESHOLD = 3 // Max consecutive frame drops
    }
    
    private var isOptimizing = false
    private var performanceTimer: Timer? = null
    private var currentQualityLevel = QualityLevel.MAXIMUM
    private var consecutiveIssues = 0
    private var frameDropCount = 0
    private var lastMemoryPercent = 0
    private var performanceHistory = mutableListOf<PerformanceSnapshot>()
    private val handler = Handler(Looper.getMainLooper())
    
    private var qualityCallback: ((QualitySettings) -> Unit)? = null
    
    enum class QualityLevel {
        MAXIMUM,     // Best possible quality
        HIGH,        // High quality with safety margin
        BALANCED,    // Balanced quality and stability
        STABLE,      // Conservative for guaranteed stability
        EMERGENCY    // Minimal settings for recovery
    }
    
    data class PerformanceSnapshot(
        val timestamp: Long,
        val memoryPercent: Int,
        val qualityLevel: QualityLevel,
        val frameDrops: Int,
        val isStable: Boolean
    )
    
    data class QualitySettings(
        val fps: Int,
        val bufferFrames: Int,
        val compressionLevel: Int,
        val enableStabilization: Boolean,
        val enableAdvancedFiltering: Boolean,
        val qualityLevel: QualityLevel,
        val memoryOptimizationLevel: Int // 0-3, higher = more aggressive
    )
    
    /**
     * Start ultimate 24/7 optimization
     */
    fun startUltimateOptimization(callback: (QualitySettings) -> Unit) {
        Log.d(TAG, "Starting Ultimate 24/7 Optimization System")
        
        qualityCallback = callback
        isOptimizing = true
        
        // Start performance monitoring
        performanceTimer = Timer("Ultimate24x7Monitor", true)
        performanceTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                performUltimateOptimization()
            }
        }, 5000, PERFORMANCE_CHECK_INTERVAL) // Start after 5 seconds, then every 15 seconds
        
        // Apply initial optimal settings
        applyOptimalSettings()
    }
    
    /**
     * Stop optimization
     */
    fun stopOptimization() {
        Log.d(TAG, "Stopping Ultimate 24/7 Optimization")
        isOptimizing = false
        performanceTimer?.cancel()
        performanceTimer = null
        qualityCallback = null
    }
    
    /**
     * Perform comprehensive optimization analysis
     */
    private fun performUltimateOptimization() {
        if (!isOptimizing) return
        
        try {
            val memoryInfo = getDetailedMemoryInfo()
            val currentTime = System.currentTimeMillis()
            
            // Analyze current performance
            val isStable = analyzeStability(memoryInfo)
            
            // Record performance snapshot
            val snapshot = PerformanceSnapshot(
                timestamp = currentTime,
                memoryPercent = memoryInfo.memoryPercent,
                qualityLevel = currentQualityLevel,
                frameDrops = frameDropCount,
                isStable = isStable
            )
            
            performanceHistory.add(snapshot)
            
            // Keep only last 20 snapshots (5 minutes of history)
            if (performanceHistory.size > 20) {
                performanceHistory.removeAt(0)
            }
            
            // Predictive quality adjustment
            adjustQualityPredictively(memoryInfo, isStable)
            
            // Apply optimizations
            applyOptimalSettings()
            
            Log.d(TAG, "Performance check: Memory=${memoryInfo.memoryPercent}%, Quality=${currentQualityLevel}, Stable=$isStable")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in ultimate optimization", e)
        }
    }
    
    /**
     * Analyze system stability
     */
    private fun analyzeStability(memoryInfo: DetailedMemoryInfo): Boolean {
        // Check memory stability
        val memoryStable = memoryInfo.memoryPercent < MEMORY_WARNING_THRESHOLD
        
        // Check memory trend
        val memoryTrend = if (performanceHistory.isNotEmpty()) {
            val recentAvg = performanceHistory.takeLast(3).map { it.memoryPercent }.average()
            memoryInfo.memoryPercent <= recentAvg + 5 // Memory not increasing rapidly
        } else true
        
        // Check frame stability
        val frameStable = frameDropCount < FRAME_DROP_THRESHOLD
        
        return memoryStable && memoryTrend && frameStable
    }
    
    /**
     * Predictive quality adjustment based on trends
     */
    private fun adjustQualityPredictively(memoryInfo: DetailedMemoryInfo, isStable: Boolean) {
        if (!isStable) {
            consecutiveIssues++
        } else {
            consecutiveIssues = maxOf(0, consecutiveIssues - 1)
        }
        
        // Predictive adjustment based on memory trend
        val memoryTrend = if (performanceHistory.size >= 3) {
            val recent = performanceHistory.takeLast(3).map { it.memoryPercent }
            when {
                recent.zipWithNext().all { it.second > it.first } -> "INCREASING"
                recent.zipWithNext().all { it.second < it.first } -> "DECREASING"
                else -> "STABLE"
            }
        } else "UNKNOWN"
        
        // Adjust quality level
        val newQualityLevel = when {
            memoryInfo.memoryPercent >= MEMORY_CRITICAL_THRESHOLD -> QualityLevel.EMERGENCY
            consecutiveIssues >= QUALITY_ADJUSTMENT_THRESHOLD -> {
                when (currentQualityLevel) {
                    QualityLevel.MAXIMUM -> QualityLevel.HIGH
                    QualityLevel.HIGH -> QualityLevel.BALANCED
                    QualityLevel.BALANCED -> QualityLevel.STABLE
                    else -> QualityLevel.STABLE
                }
            }
            memoryTrend == "INCREASING" && memoryInfo.memoryPercent > MEMORY_WARNING_THRESHOLD -> {
                // Proactively reduce quality if memory is trending up
                when (currentQualityLevel) {
                    QualityLevel.MAXIMUM -> QualityLevel.HIGH
                    QualityLevel.HIGH -> QualityLevel.BALANCED
                    else -> currentQualityLevel
                }
            }
            isStable && memoryInfo.memoryPercent < 60 && consecutiveIssues == 0 -> {
                // Increase quality if system is very stable
                when (currentQualityLevel) {
                    QualityLevel.STABLE -> QualityLevel.BALANCED
                    QualityLevel.BALANCED -> QualityLevel.HIGH
                    QualityLevel.HIGH -> QualityLevel.MAXIMUM
                    else -> currentQualityLevel
                }
            }
            else -> currentQualityLevel
        }
        
        if (newQualityLevel != currentQualityLevel) {
            Log.d(TAG, "Quality adjustment: ${currentQualityLevel} -> ${newQualityLevel} (Memory: ${memoryInfo.memoryPercent}%, Issues: $consecutiveIssues)")
            currentQualityLevel = newQualityLevel
        }
    }
    
    /**
     * Apply optimal settings based on current quality level
     */
    private fun applyOptimalSettings() {
        val settings = calculateUltimateSettings()
        
        handler.post {
            qualityCallback?.invoke(settings)
        }
    }
    
    /**
     * Calculate ultimate quality settings
     */
    fun calculateUltimateSettings(width: Int = 1920, height: Int = 1080): QualitySettings {
        val memoryInfo = getDetailedMemoryInfo()
        val deviceTier = assessDeviceTier(memoryInfo)
        
        return when (currentQualityLevel) {
            QualityLevel.MAXIMUM -> calculateMaximumQuality(width, height, deviceTier)
            QualityLevel.HIGH -> calculateHighQuality(width, height, deviceTier)
            QualityLevel.BALANCED -> calculateBalancedQuality(width, height, deviceTier)
            QualityLevel.STABLE -> calculateStableQuality(width, height, deviceTier)
            QualityLevel.EMERGENCY -> calculateEmergencyQuality(width, height, deviceTier)
        }
    }
    
    /**
     * Calculate maximum quality settings
     */
    private fun calculateMaximumQuality(width: Int, height: Int, tier: DeviceTier): QualitySettings {
        val fps = when {
            width >= 3840 -> when (tier) {
                DeviceTier.FLAGSHIP -> 30
                DeviceTier.HIGH_END -> 25
                DeviceTier.MID_RANGE -> 20
                DeviceTier.ENTRY -> 15
            }
            width >= 2560 -> when (tier) {
                DeviceTier.FLAGSHIP -> 30
                DeviceTier.HIGH_END -> 30
                DeviceTier.MID_RANGE -> 28
                DeviceTier.ENTRY -> 24
            }
            width >= 1920 -> 30
            else -> 30
        }
        
        return QualitySettings(
            fps = fps,
            bufferFrames = when (tier) {
                DeviceTier.FLAGSHIP -> 6
                DeviceTier.HIGH_END -> 5
                DeviceTier.MID_RANGE -> 4
                DeviceTier.ENTRY -> 3
            },
            compressionLevel = when (tier) {
                DeviceTier.FLAGSHIP -> 98
                DeviceTier.HIGH_END -> 95
                DeviceTier.MID_RANGE -> 92
                DeviceTier.ENTRY -> 90
            },
            enableStabilization = tier != DeviceTier.ENTRY,
            enableAdvancedFiltering = tier == DeviceTier.FLAGSHIP || tier == DeviceTier.HIGH_END,
            qualityLevel = QualityLevel.MAXIMUM,
            memoryOptimizationLevel = 0
        )
    }
    
    /**
     * Calculate high quality settings
     */
    private fun calculateHighQuality(width: Int, height: Int, tier: DeviceTier): QualitySettings {
        val maxSettings = calculateMaximumQuality(width, height, tier)
        return maxSettings.copy(
            fps = (maxSettings.fps * 0.9).toInt(),
            bufferFrames = maxOf(2, maxSettings.bufferFrames - 1),
            compressionLevel = maxSettings.compressionLevel - 3,
            qualityLevel = QualityLevel.HIGH,
            memoryOptimizationLevel = 1
        )
    }
    
    /**
     * Calculate balanced quality settings
     */
    private fun calculateBalancedQuality(width: Int, height: Int, tier: DeviceTier): QualitySettings {
        val fps = when {
            width >= 3840 -> when (tier) {
                DeviceTier.FLAGSHIP -> 20
                DeviceTier.HIGH_END -> 18
                DeviceTier.MID_RANGE -> 15
                DeviceTier.ENTRY -> 12
            }
            width >= 2560 -> when (tier) {
                DeviceTier.FLAGSHIP -> 25
                DeviceTier.HIGH_END -> 24
                DeviceTier.MID_RANGE -> 22
                DeviceTier.ENTRY -> 20
            }
            else -> 30
        }
        
        return QualitySettings(
            fps = fps,
            bufferFrames = 3,
            compressionLevel = 88,
            enableStabilization = tier != DeviceTier.ENTRY,
            enableAdvancedFiltering = false,
            qualityLevel = QualityLevel.BALANCED,
            memoryOptimizationLevel = 2
        )
    }
    
    /**
     * Calculate stable quality settings
     */
    private fun calculateStableQuality(width: Int, height: Int, tier: DeviceTier): QualitySettings {
        val fps = when {
            width >= 3840 -> 15
            width >= 2560 -> 20
            width >= 1920 -> 25
            else -> 30
        }
        
        return QualitySettings(
            fps = fps,
            bufferFrames = 2,
            compressionLevel = 85,
            enableStabilization = false,
            enableAdvancedFiltering = false,
            qualityLevel = QualityLevel.STABLE,
            memoryOptimizationLevel = 3
        )
    }
    
    /**
     * Calculate emergency quality settings
     */
    private fun calculateEmergencyQuality(width: Int, height: Int, tier: DeviceTier): QualitySettings {
        return QualitySettings(
            fps = when {
                width >= 3840 -> 10
                width >= 2560 -> 15
                else -> 20
            },
            bufferFrames = 1,
            compressionLevel = 80,
            enableStabilization = false,
            enableAdvancedFiltering = false,
            qualityLevel = QualityLevel.EMERGENCY,
            memoryOptimizationLevel = 3
        )
    }
    
    /**
     * Get detailed memory information
     */
    private fun getDetailedMemoryInfo(): DetailedMemoryInfo {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryPercent = (usedMemory * 100) / maxMemory
        
        return DetailedMemoryInfo(
            usedMemoryMB = (usedMemory / 1024 / 1024).toInt(),
            maxMemoryMB = (maxMemory / 1024 / 1024).toInt(),
            memoryPercent = memoryPercent.toInt(),
            availableMemoryMB = (freeMemory / 1024 / 1024).toInt()
        )
    }
    
    /**
     * Assess device tier for optimization
     */
    private fun assessDeviceTier(memoryInfo: DetailedMemoryInfo): DeviceTier {
        val totalMemoryGB = memoryInfo.maxMemoryMB / 1024.0
        
        return when {
            totalMemoryGB >= 8.0 -> DeviceTier.FLAGSHIP    // 8GB+ RAM
            totalMemoryGB >= 6.0 -> DeviceTier.HIGH_END    // 6-8GB RAM
            totalMemoryGB >= 4.0 -> DeviceTier.MID_RANGE   // 4-6GB RAM
            else -> DeviceTier.ENTRY                        // <4GB RAM
        }
    }
    
    /**
     * Report frame drop for adaptive optimization
     */
    fun reportFrameDrop() {
        frameDropCount++
        Log.d(TAG, "Frame drop reported, count: $frameDropCount")
    }
    
    /**
     * Reset frame drop counter
     */
    fun resetFrameDrops() {
        frameDropCount = 0
    }
    
    /**
     * Get current quality level
     */
    fun getCurrentQualityLevel(): QualityLevel = currentQualityLevel
    
    /**
     * Get performance statistics
     */
    fun getPerformanceStats(): String {
        val recentSnapshots = performanceHistory.takeLast(5)
        val avgMemory = recentSnapshots.map { it.memoryPercent }.average()
        val stability = recentSnapshots.count { it.isStable }.toDouble() / recentSnapshots.size * 100
        
        return "Quality: $currentQualityLevel, Avg Memory: ${avgMemory.toInt()}%, Stability: ${stability.toInt()}%"
    }
    
    data class DetailedMemoryInfo(
        val usedMemoryMB: Int,
        val maxMemoryMB: Int,
        val memoryPercent: Int,
        val availableMemoryMB: Int
    )
    
    enum class DeviceTier {
        FLAGSHIP,   // 8GB+ RAM, top-tier devices
        HIGH_END,   // 6-8GB RAM, high-performance devices
        MID_RANGE,  // 4-6GB RAM, balanced devices
        ENTRY       // <4GB RAM, entry-level devices
    }
}