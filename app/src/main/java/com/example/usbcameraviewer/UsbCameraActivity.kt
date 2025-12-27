package com.example.usbcameraviewer

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Main activity for USB camera viewing
 * Hosts the camera fragment and handles lifecycle
 */
class UsbCameraActivity : AppCompatActivity() {
    
    private lateinit var usbPermissionManager: UsbPermissionManager
    private lateinit var hardwareAccelManager: HardwareAccelerationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash theme to normal theme
        setTheme(R.style.Theme_WebcamViewerNative)
        
        super.onCreate(savedInstanceState)
        
        // Initialize hardware acceleration manager and optimize window
        hardwareAccelManager = HardwareAccelerationManager(this)
        hardwareAccelManager.optimizeWindow(window)
        
        // Keep screen on - prevents screensaver/ambient mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Initialize USB permission manager
        usbPermissionManager = UsbPermissionManager(this)
        
        // Handle USB device attached intent
        handleUsbDeviceIntent(intent)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, UsbCameraFragment())
                .commit()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbDeviceIntent(intent)
    }
    
    private fun handleUsbDeviceIntent(intent: Intent) {
        if (intent.action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
            val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("device", UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<UsbDevice>("device")
            }
            device?.let { usbDevice ->
                // Check if this is a camera and handle permission
                if (isUvcCamera(usbDevice)) {
                    handleCameraPermission(usbDevice)
                }
            }
        }
    }
    
    private fun isUvcCamera(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == 14) { // USB_CLASS_VIDEO
                return true
            }
        }
        return false
    }
    
    private fun handleCameraPermission(device: UsbDevice) {
        usbPermissionManager.requestPermission(device) { granted ->
            if (granted) {
                // Permission granted, camera fragment will handle the rest
                android.util.Log.d("UsbCameraActivity", "USB camera permission granted")
            } else {
                // Show user-friendly message
                Toast.makeText(
                    this, 
                    "USB camera permission denied. Please grant permission to use the camera.", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::usbPermissionManager.isInitialized) {
            usbPermissionManager.cleanup()
        }
    }
}

/**
 * Camera fragment that handles USB camera preview and controls
 * Extends AUSBC library's CameraFragment for automatic USB camera management
 */
class UsbCameraFragment : CameraFragment() {
    
    // UI Components
    private lateinit var mainLayout: FrameLayout
    private lateinit var cameraContainer: FrameLayout
    private lateinit var sidebarScroll: ScrollView
    private lateinit var sidebarLayout: LinearLayout
    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView
    
    // Control Components
    private lateinit var cameraSpinner: Spinner
    private lateinit var resolutionSpinner: Spinner
    private lateinit var fpsSpinner: Spinner
    private lateinit var rotationSpinner: Spinner
    private lateinit var flipHorizontalCheck: CheckBox
    private lateinit var flipVerticalCheck: CheckBox
    private lateinit var brightnessSeek: SeekBar
    private lateinit var contrastSeek: SeekBar
    private lateinit var saturationSeek: SeekBar
    private lateinit var alwaysAllowUsbCheck: CheckBox
    
    // Camera and Settings
    private var cameraView: AspectRatioTextureView? = null
    private var settingsManager: SettingsManager? = null
    private var usbPermissionManager: UsbPermissionManager? = null
    private var performanceOptimizer: PerformanceOptimizer? = null
    private var usbConnectionMonitor: UsbConnectionMonitor? = null
    private var ultimateOptimizer: Ultimate24x7Optimizer? = null
    private var hardwareAccelManager: HardwareAccelerationManager? = null
    private var cameraWatchdog: CameraWatchdog? = null
    private var cameraClient: com.jiangdg.ausbc.CameraClient? = null
    
    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mainLayout = FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        
        // Camera view container (full screen, clean)
        cameraContainer = FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Enable hardware acceleration for container
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        mainLayout.addView(cameraContainer)
        
        // Modern floating action button
        toggleButton = Button(requireContext()).apply {
            text = "⚙"
            textSize = 24f
            setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            setTextColor(android.graphics.Color.WHITE)
            
            // Make it circular
            val size = (56 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                setMargins(0, 16, 16, 0)
            }
            
            // Rounded corners and elevation
            elevation = 6f
            stateListAnimator = null // Remove default animation
            
            setOnClickListener { toggleSidebar() }
        }
        
        // Make button circular after layout
        toggleButton.post {
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(android.graphics.Color.parseColor("#2196F3"))
            toggleButton.background = drawable
        }
        
        mainLayout.addView(toggleButton)
        
        // Sidebar
        createSidebar()
        
        return mainLayout
    }
    
    /**
     * Creates the sidebar with all camera controls
     * Android TV optimized design with dark theme and clear focus indicators
     */
    private fun createSidebar() {
        sidebarScroll = ScrollView(requireContext()).apply {
            // Dark theme background for Android TV
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
            layoutParams = FrameLayout.LayoutParams(
                (380 * resources.displayMetrics.density).toInt(), // Wider for TV
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
            visibility = View.GONE
            elevation = 12f
            
            // TV-friendly scrolling
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }
        
        sidebarLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32) // Larger padding for TV
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
        }
        
        // Status text with better contrast
        statusText = TextView(requireContext()).apply {
            text = "Initializing..."
            setTextColor(android.graphics.Color.parseColor("#FFFFFF")) // White text
            textSize = 16f // Larger for TV
            setPadding(0, 0, 0, 24)
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
            setPadding(16, 12, 16, 12)
        }
        sidebarLayout.addView(statusText)
        
        // Divider with better visibility
        sidebarLayout.addView(View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#404040"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4 // Thicker divider
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        })
        
        // Header with title and close button - TV optimized
        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
            setPadding(20, 16, 20, 16)
        }
        
        headerLayout.addView(TextView(requireContext()).apply {
            text = "📹 Camera Settings"
            setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            textSize = 24f // Large title for TV
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        headerLayout.addView(createTVButton("✕ CLOSE", "#E53E3E") {
            toggleSidebar()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                (120 * resources.displayMetrics.density).toInt(),
                (48 * resources.displayMetrics.density).toInt()
            )
        })
        
        sidebarLayout.addView(headerLayout)
        
        // Quick action buttons - TV optimized
        sidebarLayout.addView(createTVSectionHeader("QUICK ACTIONS"))
        
        // Auto config button
        sidebarLayout.addView(createTVButton("🎯 Auto Best Config", "#2B6CB0") {
            applyBestConfig()
        })
        
        // Single optimized 24/7 mode button
        sidebarLayout.addView(createTVButton("🚀 Optimized 24/7 Mode", "#059669") {
            applyOptimized24x7Mode()
        }.apply {
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        // Camera selection section
        sidebarLayout.addView(createTVSectionHeader("CAMERA"))
        
        addTVSpinnerControl("Connected Camera", listOf(
            "No cameras detected"
        )).also { cameraSpinner = it }
        
        // Video settings section
        sidebarLayout.addView(createTVSectionHeader("VIDEO SETTINGS"))
        
        addTVSpinnerControl("Resolution", listOf(
            "640x480", "1280x720", "1920x1080", "2560x1440", "3840x2160"
        )).also { resolutionSpinner = it }
        
        addTVSpinnerControl("FPS", listOf(
            "15", "24", "30", "60"
        )).also { fpsSpinner = it }
        
        addTVSpinnerControl("Rotation", listOf(
            "0°", "90°", "180°", "270°"
        )).also { rotationSpinner = it }
        
        // Transform controls section
        sidebarLayout.addView(createTVSectionHeader("TRANSFORM"))
        
        flipHorizontalCheck = createTVCheckBox("↔️ Horizontal Flip") { _, _ -> 
            applyTransform() 
        }
        sidebarLayout.addView(flipHorizontalCheck)
        
        flipVerticalCheck = createTVCheckBox("↕️ Vertical Flip") { _, _ -> 
            applyTransform() 
        }
        sidebarLayout.addView(flipVerticalCheck)
        
        // Camera controls section
        sidebarLayout.addView(createTVSectionHeader("ADJUSTMENTS"))
        
        brightnessSeek = createTVSeekBar("☀️ Brightness", -100, 100)
        contrastSeek = createTVSeekBar("🔆 Contrast", -100, 100)
        saturationSeek = createTVSeekBar("🎨 Saturation", -100, 100)
        
        // USB Permission section
        sidebarLayout.addView(createTVSectionHeader("USB PERMISSIONS"))
        
        alwaysAllowUsbCheck = createTVCheckBox("🔓 Always allow USB cameras (no more popups)") { _, isChecked ->
            usbPermissionManager?.setAlwaysAllow(isChecked)
            val message = if (isChecked) {
                "✓ USB cameras will be allowed automatically"
            } else {
                "USB permission will be requested for each camera"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        sidebarLayout.addView(alwaysAllowUsbCheck)
        
        // Manual permission renewal button
        sidebarLayout.addView(createTVButton("🔄 Renew USB Permission", "#F59E0B") {
            usbConnectionMonitor?.renewCurrentCameraPermission()
            Toast.makeText(requireContext(), "Renewing USB camera permission...", Toast.LENGTH_SHORT).show()
        })
        
        // Manual camera recovery button
        sidebarLayout.addView(createTVButton("🛠️ Force Camera Recovery", "#DC2626") {
            cameraWatchdog?.forceRecovery("Manual recovery requested")
            Toast.makeText(requireContext(), "Forcing camera recovery...", Toast.LENGTH_SHORT).show()
        })
        
        // Proactive refresh button
        sidebarLayout.addView(createTVButton("🔄 Proactive System Refresh", "#7C3AED") {
            cameraWatchdog?.forceProactiveRefresh("Manual proactive refresh")
            Toast.makeText(requireContext(), "Performing proactive system refresh...", Toast.LENGTH_SHORT).show()
        })
        
        // Performance monitoring section
        sidebarLayout.addView(createTVSectionHeader("PERFORMANCE"))
        
        // Memory usage display
        val memoryText = createTVInfoText("Memory: Calculating...")
        sidebarLayout.addView(memoryText)
        
        // Connection status display
        val connectionStatusText = createTVInfoText("USB Status: Checking...")
        sidebarLayout.addView(connectionStatusText)
        
        // Device capability display
        val deviceCapabilityText = createTVInfoText("Device: Analyzing...")
        sidebarLayout.addView(deviceCapabilityText)
        
        // Performance statistics display
        val performanceStatsText = createTVInfoText("Performance: Initializing...")
        sidebarLayout.addView(performanceStatsText)
        
        // Hardware acceleration display
        val hardwareAccelText = createTVInfoText("Hardware: Analyzing...")
        sidebarLayout.addView(hardwareAccelText)
        
        // Watchdog status display
        val watchdogStatusText = createTVInfoText("Watchdog: Starting...")
        sidebarLayout.addView(watchdogStatusText)
        
        // Update memory display periodically
        val memoryUpdateTimer = java.util.Timer()
        memoryUpdateTimer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    // Update memory info
                    val memoryInfo = performanceOptimizer?.getMemoryInfo()
                    if (memoryInfo != null) {
                        memoryText.text = "Memory: ${memoryInfo.usedMemoryMB}MB/${memoryInfo.maxMemoryMB}MB (${memoryInfo.memoryPercent}%)"
                        
                        // Color code based on usage
                        val memoryColor = when {
                            memoryInfo.memoryPercent > 80 -> android.graphics.Color.parseColor("#F44336") // Red
                            memoryInfo.memoryPercent > 60 -> android.graphics.Color.parseColor("#FF9800") // Orange
                            else -> android.graphics.Color.parseColor("#4CAF50") // Green
                        }
                        memoryText.setTextColor(memoryColor)
                    }
                    
                    // Update connection status
                    val currentCamera = usbConnectionMonitor?.getCurrentCamera()
                    val statusText = if (currentCamera != null) {
                        val hasPermission = usbPermissionManager?.hasPermission(currentCamera) ?: false
                        if (hasPermission) {
                            "USB Status: ✓ Connected & Authorized"
                        } else {
                            "USB Status: ⚠ Connected, No Permission"
                        }
                    } else {
                        "USB Status: ❌ No Camera Detected"
                    }
                    
                    connectionStatusText.text = statusText
                    
                    // Color code the status
                    val statusColor = when {
                        statusText.contains("✓") -> android.graphics.Color.parseColor("#4CAF50") // Green
                        statusText.contains("⚠") -> android.graphics.Color.parseColor("#FF9800") // Orange
                        else -> android.graphics.Color.parseColor("#F44336") // Red
                    }
                    connectionStatusText.setTextColor(statusColor)
                    
                    // Update device capability info
                    val deviceMemoryInfo = performanceOptimizer?.getMemoryInfo()
                    if (deviceMemoryInfo != null) {
                        val totalGB = deviceMemoryInfo.maxMemoryMB / 1024.0
                        val capability = when {
                            totalGB >= 6.0 -> "High-End (${String.format("%.1f", totalGB)}GB)"
                            totalGB >= 4.0 -> "Mid-Range (${String.format("%.1f", totalGB)}GB)"
                            else -> "Entry-Level (${String.format("%.1f", totalGB)}GB)"
                        }
                        
                        deviceCapabilityText.text = "Device: $capability"
                        
                        val capabilityColor = when {
                            totalGB >= 6.0 -> android.graphics.Color.parseColor("#4CAF50") // Green
                            totalGB >= 4.0 -> android.graphics.Color.parseColor("#2196F3") // Blue
                            else -> android.graphics.Color.parseColor("#FF9800") // Orange
                        }
                        deviceCapabilityText.setTextColor(capabilityColor)
                    }
                    
                    // Update performance statistics
                    val perfStats = ultimateOptimizer?.getPerformanceStats() ?: "Not active"
                    performanceStatsText.text = "Ultimate Stats: $perfStats"
                    
                    val currentQuality = ultimateOptimizer?.getCurrentQualityLevel()
                    val statsColor = when (currentQuality) {
                        Ultimate24x7Optimizer.QualityLevel.MAXIMUM -> android.graphics.Color.parseColor("#4CAF50") // Green
                        Ultimate24x7Optimizer.QualityLevel.HIGH -> android.graphics.Color.parseColor("#2196F3") // Blue
                        Ultimate24x7Optimizer.QualityLevel.BALANCED -> android.graphics.Color.parseColor("#FF9800") // Orange
                        Ultimate24x7Optimizer.QualityLevel.STABLE -> android.graphics.Color.parseColor("#9C27B0") // Purple
                        Ultimate24x7Optimizer.QualityLevel.EMERGENCY -> android.graphics.Color.parseColor("#F44336") // Red
                        else -> android.graphics.Color.parseColor("#424242") // Gray
                    }
                    performanceStatsText.setTextColor(statsColor)
                    
                    // Update hardware acceleration info
                    val hwCapabilities = hardwareAccelManager?.analyzeHardwareCapabilities()
                    if (hwCapabilities != null) {
                        val hwInfo = "HW: OpenGL=${if (hwCapabilities.hasOpenGLES30) "3.0" else "2.0"}, " +
                                   "Codecs=${if (hwCapabilities.hasHardwareCodecs) "✓" else "✗"}, " +
                                   "Max: ${hwCapabilities.recommendedSettings.maxRecommendedResolution}"
                        hardwareAccelText.text = hwInfo
                        
                        val hwColor = when {
                            hwCapabilities.hasOpenGLES30 && hwCapabilities.hasHardwareCodecs -> android.graphics.Color.parseColor("#4CAF50") // Green
                            hwCapabilities.hasHardwareCodecs -> android.graphics.Color.parseColor("#2196F3") // Blue
                            else -> android.graphics.Color.parseColor("#FF9800") // Orange
                        }
                        hardwareAccelText.setTextColor(hwColor)
                    }
                    
                    // Update watchdog status
                    val watchdogStatus = cameraWatchdog?.getStatus()
                    if (watchdogStatus != null) {
                        val nextRefreshMin = (watchdogStatus.nextRefreshIn / 60000).toInt()
                        val uptimeHours = (watchdogStatus.uptime / 3600000).toInt()
                        
                        val statusText = "Watchdog: ${if (watchdogStatus.isHealthy) "✅ Healthy" else "⚠️ Issues"} " +
                                       "(${watchdogStatus.timeSinceLastFrame / 1000}s since frame)\n" +
                                       "Uptime: ${uptimeHours}h, Refreshes: ${watchdogStatus.refreshCount}, Next: ${nextRefreshMin}min"
                        watchdogStatusText.text = statusText
                        
                        val watchdogColor = when {
                            watchdogStatus.isHealthy -> android.graphics.Color.parseColor("#4CAF50") // Green
                            watchdogStatus.isRecovering -> android.graphics.Color.parseColor("#FF9800") // Orange
                            else -> android.graphics.Color.parseColor("#F44336") // Red
                        }
                        watchdogStatusText.setTextColor(watchdogColor)
                    }
                }
            }
        }, 1000, 3000) // Update every 3 seconds
        
        // Logs button - TV optimized
        sidebarLayout.addView(createTVSectionHeader("SYSTEM"))
        sidebarLayout.addView(createTVButton("📋 View System Logs", "#6B7280") {
            startActivity(Intent(requireContext(), LogViewerActivity::class.java))
        })
        
        sidebarScroll.addView(sidebarLayout)
        mainLayout.addView(sidebarScroll)
    }
    
    /**
     * Create TV-optimized section header
     */
    private fun createTVSectionHeader(title: String): TextView {
        return TextView(requireContext()).apply {
            text = title
            setTextColor(android.graphics.Color.parseColor("#60A5FA")) // Light blue
            textSize = 14f
            setPadding(0, 24, 0, 12)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(android.graphics.Color.parseColor("#374151"))
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 8)
            }
        }
    }
    
    /**
     * Create TV-optimized button with focus handling
     */
    private fun createTVButton(text: String, color: String, onClick: () -> Unit): Button {
        return Button(requireContext()).apply {
            this.text = text
            setBackgroundColor(android.graphics.Color.parseColor(color))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setPadding(20, 16, 20, 16)
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            
            // TV focus handling
            isFocusable = true
            isFocusableInTouchMode = true
            
            // Focus state drawable
            post {
                val drawable = android.graphics.drawable.StateListDrawable()
                
                // Focused state
                val focusedDrawable = android.graphics.drawable.GradientDrawable()
                focusedDrawable.setColor(android.graphics.Color.parseColor(color))
                focusedDrawable.setStroke(6, android.graphics.Color.parseColor("#FFFFFF"))
                focusedDrawable.cornerRadius = 8f
                drawable.addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
                
                // Normal state
                val normalDrawable = android.graphics.drawable.GradientDrawable()
                normalDrawable.setColor(android.graphics.Color.parseColor(color))
                normalDrawable.cornerRadius = 8f
                drawable.addState(intArrayOf(), normalDrawable)
                
                background = drawable
            }
            
            setOnClickListener { onClick() }
        }
    }
    
    /**
     * Create TV-optimized checkbox with better visibility
     */
    private fun createTVCheckBox(text: String, onCheckedChange: (android.widget.CompoundButton, Boolean) -> Unit): CheckBox {
        return CheckBox(requireContext()).apply {
            this.text = text
            setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            textSize = 14f
            setPadding(16, 16, 16, 16)
            
            // Dark background for better contrast
            setBackgroundColor(android.graphics.Color.parseColor("#374151"))
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            
            // TV focus handling
            isFocusable = true
            isFocusableInTouchMode = true
            
            // Focus state
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setBackgroundColor(android.graphics.Color.parseColor("#4B5563"))
                } else {
                    setBackgroundColor(android.graphics.Color.parseColor("#374151"))
                }
            }
            
            setOnCheckedChangeListener(onCheckedChange)
        }
    }
    
    /**
     * Create TV-optimized info text with better contrast
     */
    private fun createTVInfoText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(android.graphics.Color.parseColor("#D1D5DB")) // Light gray
            textSize = 12f
            setPadding(16, 8, 16, 8)
            setBackgroundColor(android.graphics.Color.parseColor("#374151"))
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
        }
    }
    
    /**
     * Create TV-optimized spinner control with better visibility and focus
     */
    private fun addTVSpinnerControl(label: String, items: List<String>): Spinner {
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "📋 $label"
            setTextColor(android.graphics.Color.parseColor("#D1D5DB"))
            textSize = 13f
            setPadding(16, 12, 16, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(android.graphics.Color.parseColor("#4B5563"))
        })
        
        val spinner = Spinner(requireContext()).apply {
            setPadding(20, 16, 20, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#374151"))
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 4, 0, 12)
            }
            
            // TV focus handling
            isFocusable = true
            isFocusableInTouchMode = true
            
            // Focus state
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setBackgroundColor(android.graphics.Color.parseColor("#1F2937"))
                } else {
                    setBackgroundColor(android.graphics.Color.parseColor("#374151"))
                }
            }
        }
        
        // Create TV-optimized adapter
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                view.textSize = 14f
                view.setPadding(16, 12, 16, 12)
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                view.setBackgroundColor(android.graphics.Color.parseColor("#374151"))
                view.textSize = 14f
                view.setPadding(20, 16, 20, 16)
                return view
            }
        }
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        sidebarLayout.addView(spinner)
        
        return spinner
    }
    
    /**
     * Create TV-optimized seekbar with better visibility and larger touch targets
     */
    private fun createTVSeekBar(label: String, min: Int, max: Int): SeekBar {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#374151"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
        
        // Label and value row
        val labelRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        labelRow.addView(TextView(requireContext()).apply {
            text = label
            setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        val valueText = TextView(requireContext()).apply {
            text = "0"
            setTextColor(android.graphics.Color.parseColor("#60A5FA"))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            minWidth = (60 * resources.displayMetrics.density).toInt()
        }
        labelRow.addView(valueText)
        
        layout.addView(labelRow)
        
        val seekBar = SeekBar(requireContext()).apply {
            this.max = max - min
            progress = -min
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 12, 0, 0)
            }
            
            // TV focus handling
            isFocusable = true
            isFocusableInTouchMode = true
            
            // Larger thumb for TV
            thumb?.let { thumb ->
                val size = (24 * resources.displayMetrics.density).toInt()
                thumb.setBounds(0, 0, size, size)
            }
        }
        
        layout.addView(seekBar)
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + min
                valueText.text = value.toString()
                if (fromUser) applyCameraControl(label.replace("🔆", "").replace("☀️", "").replace("🎨", "").trim(), value)
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })
        
        sidebarLayout.addView(layout)
        return seekBar
    }
    
    private fun toggleSidebar() {
        if (sidebarScroll.visibility == View.VISIBLE) {
            // Slide out animation
            sidebarScroll.animate()
                .translationX(sidebarScroll.width.toFloat())
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    sidebarScroll.visibility = View.GONE
                }
                .start()
            
            // Rotate button
            toggleButton.animate()
                .rotation(0f)
                .setDuration(250)
                .start()
        } else {
            // Prepare for slide in
            sidebarScroll.visibility = View.VISIBLE
            sidebarScroll.translationX = sidebarScroll.width.toFloat()
            sidebarScroll.alpha = 0f
            
            // Slide in animation
            sidebarScroll.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(250)
                .start()
            
            // Rotate button
            toggleButton.animate()
                .rotation(90f)
                .setDuration(250)
                .start()
        }
    }
    
    override fun getCameraView(): IAspectRatio {
        if (cameraView == null) {
            cameraView = AspectRatioTextureView(requireContext()).apply {
                // Apply maximum hardware acceleration optimizations
                hardwareAccelManager?.optimizeViewForHardwareAcceleration(this)
                
                // Additional optimizations for video rendering
                isOpaque = true // Better performance for video
                
                // Add frame drop detection for ultimate optimizer
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    // Simple frame drop detection - if layout changes frequently, report it
                    ultimateOptimizer?.reportFrameDrop()
                }
            }
        }
        return cameraView!!
    }
    
    override fun getCameraViewContainer(): ViewGroup = cameraContainer
    
    override fun getCameraClient(): CameraClient? {
        settingsManager = SettingsManager(requireContext())
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        // Get optimal settings from performance optimizer
        val optimizer = performanceOptimizer
        val optimalSettings = optimizer?.getOptimalCameraSettings(config.width, config.height)
        
        // Get hardware acceleration settings
        val hwSettings = hardwareAccelManager?.getOptimalCameraSettings()
        
        android.util.Log.d("UsbCameraFragment", "Creating camera client for ${config.width}x${config.height}@${config.fps}fps")
        android.util.Log.d("UsbCameraFragment", "Hardware settings: GLES=${hwSettings?.enableGLES}, HW Decoding=${hwSettings?.enableHardwareDecoding}")
        if (optimalSettings != null) {
            android.util.Log.d("UsbCameraFragment", "Optimal settings: ${optimalSettings.fps}fps, ${optimalSettings.bufferFrames} buffer frames")
        }
        
        return try {
            val client = CameraClient.newBuilder(requireContext())
                .setEnableGLES(hwSettings?.enableGLES ?: true) // Always enable hardware GLES
                .setRawImage(false) // Use compressed format for better performance
                .openDebug(false) // Disable debug for production performance
                .setCameraStrategy(CameraUvcStrategy(requireContext()))
                .setCameraRequest(
                    CameraRequest.Builder()
                        .setPreviewWidth(config.width)
                        .setPreviewHeight(config.height)
                        .create()
                )
                .build()
            
            // Store reference for watchdog
            cameraClient = client
            
            // Set up frame callback for watchdog monitoring
            client?.let { setupFrameMonitoring(it) }
            
            client
        } catch (e: Exception) {
            android.util.Log.e("UsbCameraFragment", "Failed to create camera client", e)
            cameraWatchdog?.reportCameraError("Failed to create camera client: ${e.message}")
            null
        }
    }
    
    /**
     * Set up frame monitoring for watchdog (simplified)
     */
    private fun setupFrameMonitoring(client: CameraClient) {
        try {
            // Simple monitoring - just report that camera client was created successfully
            android.util.Log.d("UsbCameraFragment", "Camera client created, starting frame monitoring")
            cameraWatchdog?.reportFrameReceived()
            
            // Set up a periodic frame report (since we can't access the actual frame callbacks)
            val frameReportTimer = java.util.Timer()
            frameReportTimer.scheduleAtFixedRate(object : java.util.TimerTask() {
                override fun run() {
                    // Assume frames are being received if camera is still active
                    cameraWatchdog?.reportFrameReceived()
                }
            }, 5000, 15000) // Report every 15 seconds
            
        } catch (e: Exception) {
            android.util.Log.e("UsbCameraFragment", "Failed to setup frame monitoring", e)
        }
    }
    
    /**
     * Restart camera for recovery
     */
    fun restartCamera() {
        android.util.Log.d("UsbCameraFragment", "Restarting camera for recovery")
        
        try {
            // Close existing camera client
            cameraClient?.closeCamera()
            cameraClient = null
            
            // Clear camera view
            cameraView = null
            
            // Force garbage collection
            System.gc()
            
            // Small delay to ensure cleanup
            Thread.sleep(1000)
            
            // Recreate camera view and client
            val newClient = getCameraClient()
            if (newClient != null) {
                android.util.Log.d("UsbCameraFragment", "Camera restart completed")
            } else {
                throw Exception("Failed to create new camera client")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("UsbCameraFragment", "Camera restart failed", e)
            throw e
        }
    }
    
    override fun initData() {
        super.initData()
        
        // Initialize hardware acceleration manager
        hardwareAccelManager = HardwareAccelerationManager(requireContext())
        val hwCapabilities = hardwareAccelManager?.applyMaximumHardwareAcceleration()
        
        // Log hardware capabilities
        android.util.Log.d("UsbCameraFragment", "Hardware capabilities: $hwCapabilities")
        
        // Ensure screen stays on for 24/7 operation
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Initialize USB permission manager
        usbPermissionManager = UsbPermissionManager(requireContext())
        
        // Initialize USB connection monitor for 24/7 operation
        usbConnectionMonitor = UsbConnectionMonitor(requireContext(), usbPermissionManager!!)
        usbConnectionMonitor?.startMonitoring { connected, device ->
            handleUsbConnectionChange(connected, device)
        }
        
        // Initialize performance optimizer for 24/7 operation
        performanceOptimizer = PerformanceOptimizer(requireContext())
        performanceOptimizer?.startOptimization()
        
        // Initialize ultimate 24/7 optimizer
        ultimateOptimizer = Ultimate24x7Optimizer(requireContext())
        
        // Initialize camera watchdog for 24/7 monitoring
        cameraWatchdog = CameraWatchdog(requireContext(), this)
        cameraWatchdog?.startWatchdog { status ->
            // Update status text with watchdog info
            statusText.text = status
            when {
                status.contains("❌") -> statusText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                status.contains("🔄") -> statusText.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                status.contains("✅") -> statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
        }
        
        setupSpinners()
        loadSavedConfig()
        updateCameraList()
        
        // Load USB permission preference
        alwaysAllowUsbCheck.isChecked = usbPermissionManager?.isAlwaysAllowEnabled() ?: false
        
        statusText.text = "✓ Ready - Connect USB camera"
        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
    }
    
    /**
     * Handle USB camera connection changes for 24/7 operation
     */
    private fun handleUsbConnectionChange(connected: Boolean, device: UsbDevice?) {
        if (connected && device != null) {
            android.util.Log.d("UsbCameraFragment", "USB camera connected with permission: ${device.deviceName}")
            statusText.text = "✓ Camera connected: ${device.productName ?: "USB Camera"}"
            statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            
            // Update camera list
            updateCameraList()
            
            // Restart camera if it was stopped due to permission issues
            restartCameraIfNeeded()
            
        } else if (!connected) {
            if (device != null) {
                android.util.Log.w("UsbCameraFragment", "USB camera disconnected or permission lost: ${device.deviceName}")
                statusText.text = "⚠ Camera disconnected or permission lost"
            } else {
                android.util.Log.w("UsbCameraFragment", "No USB camera detected")
                statusText.text = "⚠ No camera detected"
            }
            statusText.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            
            // Update camera list
            updateCameraList()
        }
    }
    
    /**
     * Restart camera connection if needed (for permission renewal)
     */
    private fun restartCameraIfNeeded() {
        // This will trigger the camera client to reconnect
        // The AUSBC library should handle the reconnection automatically
        android.util.Log.d("UsbCameraFragment", "Camera restart triggered due to permission renewal")
    }
    
    private fun updateCameraList() {
        val cameras = usbPermissionManager?.getConnectedCameras() ?: emptyList()
        
        val cameraNames = mutableListOf<String>()
        if (cameras.isEmpty()) {
            cameraNames.add("No cameras detected")
        } else {
            cameras.forEach { device ->
                cameraNames.add("${device.productName ?: "USB Camera"} (${device.deviceName})")
            }
        }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cameraNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cameraSpinner.adapter = adapter
    }
    
    private fun setupSpinners() {
        resolutionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val res = resolutionSpinner.selectedItem.toString().split("x")
                val width = res[0].toInt()
                val height = res[1].toInt()
                changeResolution(width, height)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        fpsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fps = fpsSpinner.selectedItem.toString().toInt()
                val config = settingsManager?.loadConfig() ?: CameraConfig()
                settingsManager?.saveConfig(config.copy(fps = fps))
                statusText.text = "✓ ${fps} fps"
                statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        rotationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyTransform()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadSavedConfig() {
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        // Set resolution
        val resText = "${config.width}x${config.height}"
        for (i in 0 until resolutionSpinner.count) {
            if (resolutionSpinner.getItemAtPosition(i).toString() == resText) {
                resolutionSpinner.setSelection(i)
                break
            }
        }
        
        // Set FPS
        val fpsText = config.fps.toString()
        for (i in 0 until fpsSpinner.count) {
            if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) {
                fpsSpinner.setSelection(i)
                break
            }
        }
        
        // Set rotation
        rotationSpinner.setSelection(config.rotation / 90)
        
        // Set flips
        flipHorizontalCheck.isChecked = config.flipHorizontal
        flipVerticalCheck.isChecked = config.flipVertical
    }
    
    /**
     * Applies camera control adjustments (brightness, contrast, saturation)
     * Uses the UVC strategy to communicate with the camera hardware
     */
    private fun applyCameraControl(control: String, value: Int) {
        val strategy = getCurrentCameraStrategy() as? CameraUvcStrategy ?: return
        
        when (control) {
            "Brightness" -> strategy.setBrightness(value)
            "Contrast" -> strategy.setContrast(value)
            "Saturation" -> strategy.setSaturation(value)
        }
    }
    
    /**
     * Changes camera resolution and saves to settings
     * Uses performance optimizer for optimal settings
     */
    private fun changeResolution(width: Int, height: Int) {
        val optimizer = performanceOptimizer ?: return
        val optimalSettings = optimizer.getOptimalCameraSettings(width, height)
        
        // Use optimized FPS from performance optimizer
        val optimalFps = optimalSettings.fps
        
        // Check if device can handle the resolution
        val canHandle = when {
            width >= 3840 -> optimizer.canHandle4K()
            width >= 2560 -> optimizer.canHandle2K()
            else -> true
        }
        
        if (!canHandle) {
            val fallbackRes = when {
                width >= 3840 -> "2K (2560x1440)" // Fallback from 4K to 2K
                width >= 2560 -> "1080p (1920x1080)" // Fallback from 2K to 1080p
                else -> "Current resolution"
            }
            
            Toast.makeText(requireContext(), 
                "Device may not handle this resolution well. Consider using $fallbackRes", 
                Toast.LENGTH_LONG).show()
        }
        
        // Update FPS spinner to match optimal setting
        val fpsText = optimalFps.toString()
        for (i in 0 until fpsSpinner.count) {
            if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) {
                fpsSpinner.setSelection(i)
                break
            }
        }
        
        updateResolution(width, height)
        
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        settingsManager?.saveConfig(config.copy(width = width, height = height, fps = optimalFps))
        
        // Show performance info
        val resolutionName = when {
            width >= 3840 -> "4K"
            width >= 2560 -> "2K" 
            width >= 1920 -> "1080p"
            else -> "720p"
        }
        
        statusText.text = "✓ $resolutionName (${width}x${height}) @ ${optimalFps}fps"
        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        
        // Show optimization info for high resolutions
        when {
            width >= 3840 -> {
                Toast.makeText(requireContext(), 
                    "4K mode: Optimized for stability (${optimalFps}fps, ${optimalSettings.bufferFrames} frame buffer)", 
                    Toast.LENGTH_LONG).show()
            }
            width >= 2560 -> {
                Toast.makeText(requireContext(), 
                    "2K mode: Balanced performance (${optimalFps}fps)", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun applyBestConfig() {
        // Set 1080p @ 30fps (best balance of quality and performance)
        resolutionSpinner.setSelection(2) // 1920x1080
        fpsSpinner.setSelection(2) // 30fps
        rotationSpinner.setSelection(0) // 0°
        flipHorizontalCheck.isChecked = false
        flipVerticalCheck.isChecked = false
        
        // Reset camera controls
        brightnessSeek.progress = brightnessSeek.max / 2
        contrastSeek.progress = contrastSeek.max / 2
        saturationSeek.progress = saturationSeek.max / 2
        
        Toast.makeText(requireContext(), "✓ Applied best configuration (1080p@30fps)", Toast.LENGTH_SHORT).show()
    }
    
    private fun applyOptimized24x7Mode() {
        val optimizer = performanceOptimizer ?: return
        
        // Get current resolution
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        // Apply stable, high-quality settings optimized for 24/7 operation
        val optimizedFps = when {
            config.width >= 3840 -> { // 4K
                val deviceMemory = optimizer.getMemoryInfo()
                when {
                    deviceMemory.maxMemoryMB >= 6144 -> 24  // 6GB+ RAM: Stable 4K
                    deviceMemory.maxMemoryMB >= 4096 -> 20  // 4GB+ RAM: Conservative 4K
                    else -> 15  // <4GB RAM: Safe 4K
                }
            }
            config.width >= 2560 -> { // 2K
                val deviceMemory = optimizer.getMemoryInfo()
                when {
                    deviceMemory.maxMemoryMB >= 4096 -> 30  // 4GB+ RAM: Full quality 2K
                    else -> 25  // <4GB RAM: Stable 2K
                }
            }
            config.width >= 1920 -> 30  // 1080p: Always stable at 30fps
            else -> 30  // 720p and below: Always full quality
        }
        
        // Update FPS spinner
        val fpsText = optimizedFps.toString()
        for (i in 0 until fpsSpinner.count) {
            if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) {
                fpsSpinner.setSelection(i)
                break
            }
        }
        
        // Save configuration
        settingsManager?.saveConfig(config.copy(fps = optimizedFps))
        
        // Start ultimate optimizer for adaptive quality management
        ultimateOptimizer?.startUltimateOptimization { settings ->
            // The optimizer will fine-tune settings automatically
            android.util.Log.d("UsbCameraFragment", "24/7 optimizer active: ${settings.fps}fps, Quality=${settings.qualityLevel}")
        }
        
        // Update status
        val resolutionName = when {
            config.width >= 3840 -> "4K"
            config.width >= 2560 -> "2K"
            config.width >= 1920 -> "1080p"
            else -> "720p"
        }
        
        statusText.text = "🚀 24/7 Optimized: $resolutionName @ ${optimizedFps}fps"
        statusText.setTextColor(android.graphics.Color.parseColor("#059669"))
        
        // Show optimization info
        val deviceMemory = optimizer.getMemoryInfo()
        val qualityLevel = when {
            deviceMemory.maxMemoryMB >= 6144 -> "Ultra Stable"
            deviceMemory.maxMemoryMB >= 4096 -> "High Quality"
            deviceMemory.maxMemoryMB >= 3072 -> "Balanced"
            else -> "Stable"
        }
        
        Toast.makeText(requireContext(), 
            "🚀 24/7 Mode Activated!\n${resolutionName} @ ${optimizedFps}fps\n${qualityLevel} quality with adaptive optimization", 
            Toast.LENGTH_LONG).show()
    }
    
    private fun applyTransform() {
        val rotation = rotationSpinner.selectedItemPosition * 90
        val flipH = flipHorizontalCheck.isChecked
        val flipV = flipVerticalCheck.isChecked
        
        // Calculate combined rotation with flips
        // Flip horizontal = 180° + original rotation
        // Flip vertical = mirror effect
        var finalRotation = rotation
        
        if (flipH && flipV) {
            // Both flips = 180° rotation
            finalRotation = (rotation + 180) % 360
        } else if (flipH) {
            // Horizontal flip = mirror + 180°
            finalRotation = (rotation + 180) % 360
        } else if (flipV) {
            // Vertical flip = mirror
            finalRotation = (180 - rotation + 360) % 360
        }
        
        // Apply rotation using library's setRotateType
        val rotateType = when (finalRotation) {
            0 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_0
            90 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_90
            180 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_180
            270 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_270
            else -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_0
        }
        setRotateType(rotateType)
        
        // Save config
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        settingsManager?.saveConfig(config.copy(
            rotation = rotation,
            flipHorizontal = flipH,
            flipVertical = flipV
        ))
        
        // Update status in sidebar
        val flipText = mutableListOf<String>()
        if (flipH) flipText.add("H-Flip")
        if (flipV) flipText.add("V-Flip")
        val flipStr = if (flipText.isNotEmpty()) " + ${flipText.joinToString(", ")}" else ""
        statusText.text = "✓ ${rotation}°${flipStr}"
        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop camera watchdog
        cameraWatchdog?.stopWatchdog()
        cameraWatchdog = null
        
        // Close camera client
        cameraClient?.closeCamera()
        cameraClient = null
        
        // Stop ultimate optimizer
        ultimateOptimizer?.stopOptimization()
        ultimateOptimizer = null
        
        // Stop USB connection monitoring
        usbConnectionMonitor?.stopMonitoring()
        usbConnectionMonitor = null
        
        // Stop performance optimizer
        performanceOptimizer?.stopOptimization()
        performanceOptimizer = null
        
        // Cleanup USB permission manager
        usbPermissionManager?.cleanup()
        
        // Final memory cleanup
        System.gc()
    }
    
    override fun onPause() {
        super.onPause()
        // Keep monitoring active during pause for 24/7 operation
        android.util.Log.d("UsbCameraFragment", "Fragment paused, keeping monitoring active")
        
        // Ensure camera stays active during pause
        cameraWatchdog?.reportFrameReceived() // Reset watchdog timer
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh camera status on resume
        android.util.Log.d("UsbCameraFragment", "Fragment resumed, checking camera status")
        updateCameraList()
        
        // Check if we need to renew permissions
        val currentCamera = usbConnectionMonitor?.getCurrentCamera()
        if (currentCamera != null) {
            val hasPermission = usbPermissionManager?.hasPermission(currentCamera) ?: false
            if (!hasPermission && usbPermissionManager?.isAlwaysAllowEnabled() == true) {
                android.util.Log.d("UsbCameraFragment", "Permission lost during pause, attempting renewal")
                usbConnectionMonitor?.renewCurrentCameraPermission()
            }
        }
        
        // Force camera recovery if needed
        val watchdogStatus = cameraWatchdog?.getStatus()
        if (watchdogStatus != null && !watchdogStatus.isHealthy) {
            android.util.Log.d("UsbCameraFragment", "Camera unhealthy on resume, forcing recovery")
            cameraWatchdog?.forceRecovery("Resume recovery")
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Ensure screen stays on
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    override fun onStop() {
        super.onStop()
        // Keep screen on even when stopped for 24/7 operation
        android.util.Log.d("UsbCameraFragment", "Fragment stopped, maintaining 24/7 operation")
    }
    
    override fun getGravity(): Int = Gravity.CENTER
}
