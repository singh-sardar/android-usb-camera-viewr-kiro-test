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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash theme to normal theme
        setTheme(R.style.Theme_WebcamViewerNative)
        
        super.onCreate(savedInstanceState)
        
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
                // Enable hardware acceleration for better performance
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        }
        return cameraView!!
    }
    
    override fun getCameraViewContainer(): ViewGroup = cameraContainer
    
    override fun getCameraClient(): CameraClient? {
        settingsManager = SettingsManager(requireContext())
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        return CameraClient.newBuilder(requireContext())
            .setEnableGLES(true)  // Hardware-accelerated OpenGL ES rendering
            .setRawImage(false)   // Use compressed format for better performance
            .openDebug(false)     // Disable debug for production performance
            .setCameraStrategy(CameraUvcStrategy(requireContext()))
            .setCameraRequest(
                CameraRequest.Builder()
                    .setPreviewWidth(config.width)
                    .setPreviewHeight(config.height)
                    .create()
            )
            .build()
    }
    
    override fun initData() {
        super.initData()
        
        // Initialize USB permission manager
        usbPermissionManager = UsbPermissionManager(requireContext())
        
        setupSpinners()
        loadSavedConfig()
        updateCameraList()
        
        // Load USB permission preference
        alwaysAllowUsbCheck.isChecked = usbPermissionManager?.isAlwaysAllowEnabled() ?: false
        
        statusText.text = "✓ Ready - Connect USB camera"
        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
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
     * Optimizes FPS for high resolutions to prevent lag
     */
    private fun changeResolution(width: Int, height: Int) {
        // Auto-adjust FPS for high resolutions to prevent lag
        val optimalFps = when {
            width >= 3840 -> 15  // 4K: use 15fps for stability
            width >= 2560 -> 24  // 2K: use 24fps
            width >= 1920 -> 30  // 1080p: use 30fps
            else -> 30           // Lower resolutions: 30fps
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
        
        statusText.text = "✓ ${width}x${height} @ ${optimalFps}fps"
        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        
        // Show performance tip for 4K
        if (width >= 3840) {
            Toast.makeText(requireContext(), "4K mode: Using 15fps for optimal performance", Toast.LENGTH_LONG).show()
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
        usbPermissionManager?.cleanup()
    }
    
    override fun getGravity(): Int = Gravity.CENTER
}
