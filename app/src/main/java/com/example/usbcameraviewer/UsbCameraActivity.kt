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
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import com.jiangdg.ausbc.widget.IAspectRatio

/**
 * Main activity for USB camera viewing
 * Hosts the camera fragment and handles lifecycle
 */
class UsbCameraActivity : AppCompatActivity() {
    
    private lateinit var usbPermissionManager: UsbPermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WebcamViewerNative)
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        usbPermissionManager = UsbPermissionManager(this)
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
            device?.let { if (isUvcCamera(it)) handleCameraPermission(it) }
        }
    }
    
    private fun isUvcCamera(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
             if (device.getInterface(i).interfaceClass == 14) return true
        }
        return false
    }
    
    private fun handleCameraPermission(device: UsbDevice) {
        usbPermissionManager.requestPermission(device) { granted ->
            if (granted) android.util.Log.d("UsbCameraActivity", "Permission granted")
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::usbPermissionManager.isInitialized) usbPermissionManager.cleanup()
    }
}

class UsbCameraFragment : CameraFragment() {
    
    private lateinit var mainLayout: FrameLayout
    private lateinit var cameraContainer: FrameLayout
    private lateinit var sidebarScroll: ScrollView
    private lateinit var sidebarLayout: LinearLayout
    private lateinit var toggleButton: Button
    private lateinit var statusText: TextView
    
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
    
    private var cameraView: IAspectRatio? = null
    private var settingsManager: SettingsManager? = null
    private var usbPermissionManager: UsbPermissionManager? = null
    private var usbConnectionMonitor: UsbConnectionMonitor? = null
    private var cameraWatchdog: CameraWatchdog? = null
    private var periodicRefreshTimer: java.util.Timer? = null
    
    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mainLayout = FrameLayout(requireContext()).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        
        cameraContainer = FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        mainLayout.addView(cameraContainer)
        
        toggleButton = Button(requireContext()).apply {
            text = "⚙"
            textSize = 24f
            setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = FrameLayout.LayoutParams((56 * resources.displayMetrics.density).toInt(), (56 * resources.displayMetrics.density).toInt(), Gravity.TOP or Gravity.END).apply { setMargins(0, 16, 16, 0) }
            elevation = 6f
            stateListAnimator = null
            setOnClickListener { toggleSidebar() }
        }
        toggleButton.post {
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(android.graphics.Color.parseColor("#2196F3"))
            toggleButton.background = drawable
        }
        mainLayout.addView(toggleButton)
        
        createSidebar()
        return mainLayout
    }
    
    private fun createSidebar() {
        sidebarScroll = ScrollView(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
            layoutParams = FrameLayout.LayoutParams((380 * resources.displayMetrics.density).toInt(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END)
            visibility = View.GONE
            elevation = 12f
            isVerticalScrollBarEnabled = true
        }
        
        sidebarLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
        }
        
        statusText = TextView(requireContext()).apply {
            text = "Initializing..."
            setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            textSize = 16f
            setPadding(16, 12, 16, 24)
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
        }
        sidebarLayout.addView(statusText)
        
        sidebarLayout.addView(View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#404040"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4).apply { setMargins(0, 0, 0, 24) }
        })
        
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 16, 20, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"))
        }
        header.addView(TextView(requireContext()).apply {
            text = "📹 Camera Settings"
            setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            textSize = 24f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(createTVButton("✕ CLOSE", "#E53E3E") { toggleSidebar() }.apply {
            layoutParams = LinearLayout.LayoutParams((120 * resources.displayMetrics.density).toInt(), (48 * resources.displayMetrics.density).toInt())
        })
        sidebarLayout.addView(header)
        
        // MODES
        sidebarLayout.addView(createTVSectionHeader("OPTIMIZATION MODES"))
        sidebarLayout.addView(createTVButton("🚀 Enable 24/7 Standard Mode", "#059669") { apply24x7Mode(true) })
        sidebarLayout.addView(createTVButton("📺 Start Xiaomi Optimized Mode", "#7C3AED") { applyXiaomiOptimizedMode(true) })
        
        sidebarLayout.addView(createTVSectionHeader("CAMERA"))
        addTVSpinnerControl("Connected Camera", listOf("No cameras detected")).also { cameraSpinner = it }
        
        sidebarLayout.addView(createTVSectionHeader("VIDEO SETTINGS"))
        addTVSpinnerControl("Resolution", listOf("640x480", "1280x720", "1920x1080", "2560x1440", "3840x2160")).also { resolutionSpinner = it }
        addTVSpinnerControl("FPS", listOf("10", "15", "20", "24", "30", "60")).also { fpsSpinner = it }
        addTVSpinnerControl("Rotation", listOf("0°", "90°", "180°", "270°")).also { rotationSpinner = it }
        
        sidebarLayout.addView(createTVSectionHeader("TRANSFORM"))
        flipHorizontalCheck = createTVCheckBox("↔️ Horizontal Flip") { _, _ -> applyTransform() }
        sidebarLayout.addView(flipHorizontalCheck)
        flipVerticalCheck = createTVCheckBox("↕️ Vertical Flip") { _, _ -> applyTransform() }
        sidebarLayout.addView(flipVerticalCheck)
        
        sidebarLayout.addView(createTVSectionHeader("ADJUSTMENTS"))
        brightnessSeek = createTVSeekBar("☀️ Brightness", -100, 100)
        contrastSeek = createTVSeekBar("🔆 Contrast", -100, 100)
        saturationSeek = createTVSeekBar("🎨 Saturation", -100, 100)
        
        sidebarLayout.addView(createTVSectionHeader("USB PERMISSIONS"))
        alwaysAllowUsbCheck = createTVCheckBox("🔓 Always allow USB cameras") { _, isChecked -> usbPermissionManager?.setAlwaysAllow(isChecked) }
        sidebarLayout.addView(alwaysAllowUsbCheck)
        sidebarLayout.addView(createTVButton("🔄 Renew USB Permission", "#F59E0B") { usbConnectionMonitor?.renewCurrentCameraPermission() })
        
        sidebarLayout.addView(createTVSectionHeader("SYSTEM"))
        sidebarLayout.addView(createTVButton("♻️ Restart App (Recovery)", "#DC2626") { restartCamera() })
        sidebarLayout.addView(createTVButton("📋 View System Logs", "#6B7280") { startActivity(Intent(requireContext(), LogViewerActivity::class.java)) })
        
        sidebarScroll.addView(sidebarLayout)
        mainLayout.addView(sidebarScroll)
    }
    
    private fun createTVSectionHeader(title: String) = TextView(requireContext()).apply {
        text = title
        setTextColor(android.graphics.Color.parseColor("#60A5FA"))
        textSize = 14f
        setPadding(16, 24, 16, 12)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setBackgroundColor(android.graphics.Color.parseColor("#374151"))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }
    
    private fun createTVButton(text: String, color: String, onClick: () -> Unit) = Button(requireContext()).apply {
        this.text = text
        setBackgroundColor(android.graphics.Color.parseColor(color))
        setTextColor(android.graphics.Color.WHITE)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (56 * resources.displayMetrics.density).toInt()).apply { setMargins(0, 8, 0, 8) }
    }
    
    private fun createTVCheckBox(text: String, onCheckedChange: (CompoundButton, Boolean) -> Unit) = CheckBox(requireContext()).apply {
        this.text = text
        setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        setBackgroundColor(android.graphics.Color.parseColor("#374151"))
        setPadding(16, 16, 16, 16)
        setOnCheckedChangeListener(onCheckedChange)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }
    }
    
    private fun createTVInfoText(text: String) = TextView(requireContext()).apply {
        this.text = text
        setTextColor(android.graphics.Color.parseColor("#D1D5DB"))
        setPadding(16, 8, 16, 8)
        setBackgroundColor(android.graphics.Color.parseColor("#374151"))
    }
    
    private fun addTVSpinnerControl(label: String, items: List<String>): Spinner {
        sidebarLayout.addView(createTVInfoText("📋 $label"))
        val spinner = Spinner(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#374151"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * resources.displayMetrics.density).toInt()).apply { setMargins(0, 4, 0, 12) }
        }
        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    setPadding(16, 12, 16, 12)
                }
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                 return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#374151"))
                    setPadding(20, 16, 20, 16)
                }
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        sidebarLayout.addView(spinner)
        return spinner
    }
    
    private fun createTVSeekBar(label: String, min: Int, max: Int): SeekBar {
        sidebarLayout.addView(createTVInfoText(label))
        val seekBar = SeekBar(requireContext()).apply {
            this.max = max - min
            progress = -min
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * resources.displayMetrics.density).toInt())
            thumb?.let { it.setBounds(0, 0, (24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt()) }
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) applyCameraControl(label.replace("☀️", "").replace("🔆", "").replace("🎨", "").trim(), progress + min)
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })
        sidebarLayout.addView(seekBar)
        return seekBar
    }
    
    private fun toggleSidebar() {
        if (sidebarScroll.visibility == View.VISIBLE) {
            sidebarScroll.animate().translationX(sidebarScroll.width.toFloat()).alpha(0f).setDuration(250).withEndAction { sidebarScroll.visibility = View.GONE }.start()
            toggleButton.animate().rotation(0f).setDuration(250).start()
        } else {
            sidebarScroll.visibility = View.VISIBLE
            sidebarScroll.translationX = sidebarScroll.width.toFloat()
            sidebarScroll.alpha = 0f
            sidebarScroll.animate().translationX(0f).alpha(1f).setDuration(250).start()
            toggleButton.animate().rotation(90f).setDuration(250).start()
        }
    }
    
    override fun getCameraView(): IAspectRatio {
        if (cameraView == null) {
            val sm = settingsManager ?: SettingsManager(requireContext())
            val config = sm.loadConfig()
            
            if (config.mode == "xiaomi") {
                cameraView = AspectRatioSurfaceView(requireContext())
            } else {
                cameraView = AspectRatioTextureView(requireContext()).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isOpaque = true
                }
            }
        }
        return cameraView!!
    }
    
    override fun getCameraViewContainer(): ViewGroup = cameraContainer
    
    override fun onCameraState(self: com.jiangdg.ausbc.MultiCameraClient.ICamera, code: com.jiangdg.ausbc.callback.ICameraStateCallBack.State, msg: String?) {
        when (code) {
             com.jiangdg.ausbc.callback.ICameraStateCallBack.State.OPENED -> activity?.runOnUiThread {
                 val mode = settingsManager?.loadConfig()?.mode
                 val suffix = if (mode == "xiaomi") "(Xiaomi Optimized OpenGL)" else if (mode == "24x7") "(24/7 Std)" else ""
                 statusText.text = "Camera: Connected $suffix"
                 statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
             }
             com.jiangdg.ausbc.callback.ICameraStateCallBack.State.CLOSED -> activity?.runOnUiThread {
                 statusText.text = "Camera: Disconnected"
                 statusText.setTextColor(android.graphics.Color.parseColor("#F44336"))
             }
             com.jiangdg.ausbc.callback.ICameraStateCallBack.State.ERROR -> activity?.runOnUiThread {
                 statusText.text = "Camera Error: $msg"
                 statusText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                 cameraWatchdog?.reportCameraError("Camera state error: $msg")
             }
        }
    }
    
    override fun getCameraRequest(): CameraRequest {
        settingsManager = SettingsManager(requireContext())
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        return try {
            CameraRequest.Builder()
                .setPreviewWidth(config.width)
                .setPreviewHeight(config.height)
                .setRenderMode(if (config.useOpengl) CameraRequest.RenderMode.OPENGL else CameraRequest.RenderMode.NORMAL)
                .setDefaultRotateType(com.jiangdg.ausbc.render.env.RotateType.ANGLE_0)
                .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
                .setAspectRatioShow(true)
                .setCaptureRawImage(false)
                .setRawPreviewData(false)
                .create()
        } catch (e: Exception) {
            CameraRequest.Builder().setPreviewWidth(1280).setPreviewHeight(720).create()
        }
    }
    
    fun restartCamera() {
        android.util.Log.d("UsbCameraFragment", "Restarting application for recovery")
        activity?.recreate()
    }
    
    override fun initData() {
        super.initData()
        usbPermissionManager = UsbPermissionManager(requireContext())
        usbConnectionMonitor = UsbConnectionMonitor(requireContext(), usbPermissionManager!!)
        usbConnectionMonitor?.startMonitoring { connected, device -> handleUsbConnectionChange(connected, device) }
        cameraWatchdog = CameraWatchdog(requireContext(), this)
        cameraWatchdog?.startWatchdog { status ->
            if (status.contains("❌") || status.contains("🔄")) activity?.runOnUiThread {
                 statusText.text = status
                 statusText.setTextColor(android.graphics.Color.parseColor(if (status.contains("❌")) "#F44336" else "#FF9800"))
            }
        }
        setupSpinners()
        loadSavedConfig()
        updateCameraList()
        alwaysAllowUsbCheck.isChecked = usbPermissionManager?.isAlwaysAllowEnabled() ?: false
        
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        when (config.mode) {
            "24x7" -> apply24x7Mode(false)
            "xiaomi" -> applyXiaomiOptimizedMode(false)
        }
        if (config.mode == "manual") {
            statusText.text = "✓ Ready - Connect USB camera"
            statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        }
    }
    
    private fun handleUsbConnectionChange(connected: Boolean, device: UsbDevice?) {
        if (connected) {
             updateCameraList()
             restartCameraIfNeeded()
        } else {
             statusText.text = "⚠ Camera disconnected"
             statusText.setTextColor(android.graphics.Color.parseColor("#FF9800"))
             updateCameraList()
        }
    }
    
    private fun restartCameraIfNeeded() {
        android.util.Log.d("UsbCameraFragment", "Permission/Device event. Library handles logic.")
    }
    
    private fun updateCameraList() {
        val cameras = usbPermissionManager?.getConnectedCameras() ?: emptyList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cameras.map { "${it.productName} (${it.deviceName})" })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cameraSpinner.adapter = adapter
    }
    
    private fun setupSpinners() {
        resolutionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                 val res = resolutionSpinner.selectedItem.toString().split("x")
                 if (res.size == 2) changeResolution(res[0].toInt(), res[1].toInt())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        fpsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                 val fps = fpsSpinner.selectedItem.toString().toIntOrNull() ?: 30
                 val config = settingsManager?.loadConfig() ?: CameraConfig()
                 settingsManager?.saveConfig(config.copy(fps = fps))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        rotationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { applyTransform() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadSavedConfig() {
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        val resText = "${config.width}x${config.height}"
        for (i in 0 until resolutionSpinner.count) if (resolutionSpinner.getItemAtPosition(i).toString() == resText) resolutionSpinner.setSelection(i)
        val fpsText = config.fps.toString()
        for (i in 0 until fpsSpinner.count) if (fpsSpinner.getItemAtPosition(i).toString() == fpsText) fpsSpinner.setSelection(i)
        rotationSpinner.setSelection(config.rotation / 90)
        flipHorizontalCheck.isChecked = config.flipHorizontal
        flipVerticalCheck.isChecked = config.flipVertical
    }

    private fun applyCameraControl(control: String, value: Int) {
         when (control) {
            "Brightness" -> setBrightness(value)
            "Contrast" -> setContrast(value)
            "Saturation" -> setSaturation(value)
        }
    }
    
    private fun changeResolution(width: Int, height: Int) {
        updateResolution(width, height)
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        settingsManager?.saveConfig(config.copy(width = width, height = height))
    }
    
    private fun apply24x7Mode(restart: Boolean) {
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        if (restart) {
            settingsManager?.saveConfig(config.copy(fps = if (config.width >= 1920) 15 else 30, useOpengl = false, mode = "24x7"))
            Toast.makeText(requireContext(), "Activating 24/7 Standard Mode...", Toast.LENGTH_SHORT).show()
            restartCamera()
            return
        }
        setupPeriodicRefresh()
        statusText.text = "🚀 24/7 Mode Active (Standard)"
        statusText.setTextColor(android.graphics.Color.parseColor("#059669"))
    }
    
    private fun applyXiaomiOptimizedMode(restart: Boolean) {
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        if (restart) {
            // Optimize engine (OpenGL/SurfaceView) but respect user resolution
            // Enforce 15fps limit only if 4K is selected/exists to prevent bandwidth issues
            val safeFps = if (config.width >= 3840) 15 else config.fps
            settingsManager?.saveConfig(config.copy(fps = safeFps, useOpengl = true, mode = "xiaomi"))
            Toast.makeText(requireContext(), "Activating Xiaomi Optimized Mode (OpenGL)...", Toast.LENGTH_SHORT).show()
            restartCamera()
            return
        }
        setupPeriodicRefresh()
        statusText.text = "🚀 Xiaomi Optimized Mode Active (OpenGL)"
        statusText.setTextColor(android.graphics.Color.parseColor("#7C3AED"))
    }
    
    private fun setupPeriodicRefresh() {
        periodicRefreshTimer?.cancel()
        periodicRefreshTimer = java.util.Timer("PeriodicRefresh", true)
        periodicRefreshTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                android.util.Log.d("UsbCameraFragment", "Periodic Refresh Triggered -> Restarting App")
                activity?.runOnUiThread { restartCamera() }
            }
        }, 7200000, 7200000)
    }

    private fun applyTransform() {
        val rotation = rotationSpinner.selectedItemPosition * 90
        val flipH = flipHorizontalCheck.isChecked
        val flipV = flipVerticalCheck.isChecked
        var finalRotation = rotation
        if (flipH && flipV) finalRotation = (rotation + 180) % 360
        else if (flipH) finalRotation = (rotation + 180) % 360
        else if (flipV) finalRotation = (180 - rotation + 360) % 360
        
        val rotateType = when (finalRotation) {
            0 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_0
            90 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_90
            180 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_180
            270 -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_270
            else -> com.jiangdg.ausbc.render.env.RotateType.ANGLE_0
        }
        setRotateType(rotateType)
        
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        settingsManager?.saveConfig(config.copy(rotation = rotation, flipHorizontal = flipH, flipVertical = flipV))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraWatchdog?.stopWatchdog()
        periodicRefreshTimer?.cancel()
        usbConnectionMonitor?.stopMonitoring()
        usbPermissionManager?.cleanup()
        getCurrentCamera()?.closeCamera()
    }
    
    override fun getGravity(): Int = Gravity.CENTER
}
