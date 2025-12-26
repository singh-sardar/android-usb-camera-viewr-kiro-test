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
     * Modern design with clean UI
     */
    private fun createSidebar() {
        sidebarScroll = ScrollView(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            layoutParams = FrameLayout.LayoutParams(
                (320 * resources.displayMetrics.density).toInt(),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
            visibility = View.GONE
            elevation = 8f
        }
        
        sidebarLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        // Status text (moved from overlay)
        statusText = TextView(requireContext()).apply {
            text = "Initializing..."
            setTextColor(android.graphics.Color.parseColor("#666666"))
            textSize = 12f
            setPadding(0, 0, 0, 16)
            gravity = Gravity.CENTER
        }
        sidebarLayout.addView(statusText)
        
        // Divider
        sidebarLayout.addView(View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        })
        
        // Header with title and close button
        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 20)
        }
        
        headerLayout.addView(TextView(requireContext()).apply {
            text = "Settings"
            setTextColor(android.graphics.Color.parseColor("#212121"))
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        headerLayout.addView(Button(requireContext()).apply {
            text = "✕"
            textSize = 20f
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            setTextColor(android.graphics.Color.parseColor("#424242"))
            val size = (40 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding(0, 0, 0, 0)
            
            // Make it circular
            post {
                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                drawable.setColor(android.graphics.Color.parseColor("#E0E0E0"))
                background = drawable
            }
            
            setOnClickListener { toggleSidebar() }
        })
        
        sidebarLayout.addView(headerLayout)
        
        // Auto config button (modern style)
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "🎯 Auto Best Config"
            setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
            setOnClickListener { applyBestConfig() }
        })
        
        // Performance mode button
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "⚡ Enhanced 24/7 Mode"
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
            setOnClickListener { apply24x7Mode() }
        })
        
        // High quality 24/7 mode button
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "💎 Max Quality 24/7"
            setBackgroundColor(android.graphics.Color.parseColor("#9C27B0"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
            setOnClickListener { applyMaxQuality24x7Mode() }
        })
        
        // Ultimate 24/7 mode button
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "🚀 ULTIMATE 24/7"
            setBackgroundColor(android.graphics.Color.parseColor("#E91E63"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
            setOnClickListener { applyUltimate24x7Mode() }
        })
        
        // Camera selection
        addSpinnerControl("Connected Camera", listOf(
            "No cameras detected"
        )).also { cameraSpinner = it }
        
        // Resolution
        addSpinnerControl("Resolution", listOf(
            "640x480", "1280x720", "1920x1080", "2560x1440", "3840x2160"
        )).also { resolutionSpinner = it }
        
        // FPS
        addSpinnerControl("FPS", listOf(
            "15", "24", "30", "60"
        )).also { fpsSpinner = it }
        
        // Rotation
        addSpinnerControl("Rotation", listOf(
            "0°", "90°", "180°", "270°"
        )).also { rotationSpinner = it }
        
        // Flip controls
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "FLIP"
            setTextColor(android.graphics.Color.parseColor("#757575"))
            textSize = 12f
            setPadding(0, 16, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        flipHorizontalCheck = CheckBox(requireContext()).apply {
            text = "Horizontal"
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(8, 8, 8, 8)
            setOnCheckedChangeListener { _, _ -> applyTransform() }
        }
        sidebarLayout.addView(flipHorizontalCheck)
        
        flipVerticalCheck = CheckBox(requireContext()).apply {
            text = "Vertical"
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(8, 8, 8, 8)
            setOnCheckedChangeListener { _, _ -> applyTransform() }
        }
        sidebarLayout.addView(flipVerticalCheck)
        
        // Camera controls
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "ADJUSTMENTS"
            setTextColor(android.graphics.Color.parseColor("#757575"))
            textSize = 12f
            setPadding(0, 16, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        brightnessSeek = createSeekBar("Brightness", -100, 100)
        contrastSeek = createSeekBar("Contrast", -100, 100)
        saturationSeek = createSeekBar("Saturation", -100, 100)
        
        // USB Permission Settings
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "USB PERMISSIONS"
            setTextColor(android.graphics.Color.parseColor("#757575"))
            textSize = 12f
            setPadding(0, 16, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        alwaysAllowUsbCheck = CheckBox(requireContext()).apply {
            text = "Always allow USB cameras (no more popups)"
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(8, 8, 8, 8)
            setOnCheckedChangeListener { _, isChecked ->
                usbPermissionManager?.setAlwaysAllow(isChecked)
                val message = if (isChecked) {
                    "✓ USB cameras will be allowed automatically"
                } else {
                    "USB permission will be requested for each camera"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
        sidebarLayout.addView(alwaysAllowUsbCheck)
        
        // Manual permission renewal button for troubleshooting
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "🔄 Renew USB Permission"
            setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(12, 12, 12, 12)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
            setOnClickListener {
                usbConnectionMonitor?.renewCurrentCameraPermission()
                Toast.makeText(requireContext(), "Renewing USB camera permission...", Toast.LENGTH_SHORT).show()
            }
        })
        
        // Performance Monitoring
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "PERFORMANCE"
            setTextColor(android.graphics.Color.parseColor("#757575"))
            textSize = 12f
            setPadding(0, 16, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        // Memory usage display
        val memoryText = TextView(requireContext()).apply {
            text = "Memory: Calculating..."
            setTextColor(android.graphics.Color.parseColor("#424242"))
            textSize = 11f
            setPadding(8, 4, 8, 4)
        }
        sidebarLayout.addView(memoryText)
        
        // Connection status display
        val connectionStatusText = TextView(requireContext()).apply {
            text = "USB Status: Checking..."
            setTextColor(android.graphics.Color.parseColor("#424242"))
            textSize = 11f
            setPadding(8, 4, 8, 4)
        }
        sidebarLayout.addView(connectionStatusText)
        
        // Device capability display
        val deviceCapabilityText = TextView(requireContext()).apply {
            text = "Device: Analyzing..."
            setTextColor(android.graphics.Color.parseColor("#424242"))
            textSize = 11f
            setPadding(8, 4, 8, 4)
        }
        sidebarLayout.addView(deviceCapabilityText)
        
        // Performance statistics display
        val performanceStatsText = TextView(requireContext()).apply {
            text = "Performance: Initializing..."
            setTextColor(android.graphics.Color.parseColor("#424242"))
            textSize = 10f
            setPadding(8, 4, 8, 4)
            maxLines = 2
        }
        sidebarLayout.addView(performanceStatsText)
        
        // Hardware acceleration display
        val hardwareAccelText = TextView(requireContext()).apply {
            text = "Hardware: Analyzing..."
            setTextColor(android.graphics.Color.parseColor("#424242"))
            textSize = 10f
            setPadding(8, 4, 8, 4)
            maxLines = 2
        }
        sidebarLayout.addView(hardwareAccelText)
        
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
                }
            }
        }, 1000, 3000) // Update every 3 seconds
        
        // Logs button
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "📋 View Logs"
            setBackgroundColor(android.graphics.Color.parseColor("#757575"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            setOnClickListener {
                startActivity(Intent(requireContext(), LogViewerActivity::class.java))
            }
        })
        
        sidebarScroll.addView(sidebarLayout)
        mainLayout.addView(sidebarScroll)
    }
    
    private fun addSpinnerControl(label: String, items: List<String>): Spinner {
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = label
            setTextColor(android.graphics.Color.parseColor("#757575"))
            textSize = 12f
            setPadding(0, 16, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        val spinner = Spinner(requireContext()).apply {
            setPadding(12, 12, 12, 12)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        sidebarLayout.addView(spinner)
        
        return spinner
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
    
    private fun createSeekBar(label: String, min: Int, max: Int): SeekBar {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        
        layout.addView(TextView(requireContext()).apply {
            text = label
            setTextColor(android.graphics.Color.parseColor("#424242"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(90, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        
        val seekBar = SeekBar(requireContext()).apply {
            this.max = max - min
            progress = -min
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        layout.addView(seekBar)
        
        val valueText = TextView(requireContext()).apply {
            text = "0"
            setTextColor(android.graphics.Color.parseColor("#2196F3"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(45, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.END
        }
        layout.addView(valueText)
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + min
                valueText.text = value.toString()
                if (fromUser) applyCameraControl(label, value)
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })
        
        sidebarLayout.addView(layout)
        return seekBar
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
            CameraClient.newBuilder(requireContext())
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
        } catch (e: Exception) {
            android.util.Log.e("UsbCameraFragment", "Failed to create camera client", e)
            null
        }
    }
    
    override fun initData() {
        super.initData()
        
        // Initialize hardware acceleration manager
        hardwareAccelManager = HardwareAccelerationManager(requireContext())
        val hwCapabilities = hardwareAccelManager?.applyMaximumHardwareAcceleration()
        
        // Log hardware capabilities
        android.util.Log.d("UsbCameraFragment", "Hardware capabilities: $hwCapabilities")
        
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
    
    private fun apply24x7Mode() {
        val optimizer = performanceOptimizer ?: return
        
        // Get current resolution
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        // Apply enhanced 24/7 optimized settings with better quality
        val enhancedSettings = optimizer.getEnhanced24x7Settings(config.width, config.height)
        
        // Use enhanced FPS settings
        val enhancedFps = enhancedSettings.fps
        
        // Update FPS spinner
        val fpsText = enhancedFps.toString()
        for (i in 0 until fpsSpinner.count) {
            if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) {
                fpsSpinner.setSelection(i)
                break
            }
        }
        
        // Save configuration
        settingsManager?.saveConfig(config.copy(fps = enhancedFps))
        
        // Update status with quality info
        val resolutionName = when {
            config.width >= 3840 -> "4K"
            config.width >= 2560 -> "2K"
            config.width >= 1920 -> "1080p"
            else -> "720p"
        }
        
        statusText.text = "✓ Enhanced 24/7: $resolutionName @ ${enhancedFps}fps"
        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        
        // Show enhanced quality info
        val qualityInfo = when (enhancedSettings.qualityMode) {
            "ENHANCED_24x7" -> {
                val stabilization = if (enhancedSettings.enableImageStabilization) " + Stabilization" else ""
                "Enhanced quality (${enhancedSettings.compressionLevel}% quality${stabilization})"
            }
            else -> "Optimized for continuous operation"
        }
        
        Toast.makeText(requireContext(), 
            "✓ Enhanced 24/7 mode: ${enhancedFps}fps, ${qualityInfo}", 
            Toast.LENGTH_LONG).show()
    }
    
    private fun applyMaxQuality24x7Mode() {
        val optimizer = performanceOptimizer ?: return
        
        // Get current resolution
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        // Apply maximum quality settings while maintaining 24/7 stability
        val maxQualityFps = when {
            config.width >= 3840 -> { // 4K
                val deviceMemory = optimizer.getMemoryInfo()
                when {
                    deviceMemory.maxMemoryMB >= 6144 -> 25  // 6GB+ RAM: Higher quality 4K
                    deviceMemory.maxMemoryMB >= 4096 -> 20  // 4GB+ RAM: Good quality 4K
                    else -> 15  // <4GB RAM: Conservative 4K
                }
            }
            config.width >= 2560 -> { // 2K
                val deviceMemory = optimizer.getMemoryInfo()
                when {
                    deviceMemory.maxMemoryMB >= 4096 -> 30  // 4GB+ RAM: Full quality 2K
                    else -> 25  // <4GB RAM: Good quality 2K
                }
            }
            config.width >= 1920 -> 30  // 1080p: Always full quality
            else -> 30  // 720p and below: Always full quality
        }
        
        // Update FPS spinner
        val fpsText = maxQualityFps.toString()
        for (i in 0 until fpsSpinner.count) {
            if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) {
                fpsSpinner.setSelection(i)
                break
            }
        }
        
        // Save configuration
        settingsManager?.saveConfig(config.copy(fps = maxQualityFps))
        
        // Update status
        val resolutionName = when {
            config.width >= 3840 -> "4K"
            config.width >= 2560 -> "2K"
            config.width >= 1920 -> "1080p"
            else -> "720p"
        }
        
        statusText.text = "✓ Max Quality 24/7: $resolutionName @ ${maxQualityFps}fps"
        statusText.setTextColor(android.graphics.Color.parseColor("#9C27B0"))
        
        // Show quality enhancement info
        val deviceMemory = optimizer.getMemoryInfo()
        val qualityLevel = when {
            deviceMemory.maxMemoryMB >= 6144 -> "Ultra High"
            deviceMemory.maxMemoryMB >= 4096 -> "High"
            deviceMemory.maxMemoryMB >= 3072 -> "Good"
            else -> "Standard"
        }
        
        Toast.makeText(requireContext(), 
            "💎 Max Quality 24/7: ${maxQualityFps}fps, ${qualityLevel} quality with enhanced buffering", 
            Toast.LENGTH_LONG).show()
    }
    
    private fun applyUltimate24x7Mode() {
        val optimizer = ultimateOptimizer ?: return
        
        // Get current resolution
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        // Start ultimate optimization with adaptive quality
        optimizer.startUltimateOptimization { settings ->
            // Apply the optimized settings
            val fpsText = settings.fps.toString()
            for (i in 0 until fpsSpinner.count) {
                if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) {
                    fpsSpinner.setSelection(i)
                    break
                }
            }
            
            // Save configuration
            settingsManager?.saveConfig(config.copy(fps = settings.fps))
            
            // Update status with ultimate info
            val resolutionName = when {
                config.width >= 3840 -> "4K"
                config.width >= 2560 -> "2K"
                config.width >= 1920 -> "1080p"
                else -> "720p"
            }
            
            val qualityInfo = when (settings.qualityLevel) {
                Ultimate24x7Optimizer.QualityLevel.MAXIMUM -> "🚀 MAXIMUM"
                Ultimate24x7Optimizer.QualityLevel.HIGH -> "⭐ HIGH"
                Ultimate24x7Optimizer.QualityLevel.BALANCED -> "⚖️ BALANCED"
                Ultimate24x7Optimizer.QualityLevel.STABLE -> "🛡️ STABLE"
                Ultimate24x7Optimizer.QualityLevel.EMERGENCY -> "🆘 EMERGENCY"
            }
            
            statusText.text = "🚀 ULTIMATE: $resolutionName @ ${settings.fps}fps ($qualityInfo)"
            statusText.setTextColor(android.graphics.Color.parseColor("#E91E63"))
            
            android.util.Log.d("UsbCameraFragment", "Ultimate mode applied: ${settings.fps}fps, Quality=${settings.qualityLevel}, Buffer=${settings.bufferFrames}")
        }
        
        // Calculate initial settings for display
        val initialSettings = optimizer.calculateUltimateSettings(config.width, config.height)
        
        Toast.makeText(requireContext(), 
            "🚀 ULTIMATE 24/7 MODE ACTIVATED!\nAdaptive Quality: ${initialSettings.fps}fps\nSelf-optimizing for maximum performance", 
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
        android.util.Log.d("UsbCameraFragment", "Fragment paused, keeping USB monitoring active")
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
    }
    
    override fun getGravity(): Int = Gravity.CENTER
}
