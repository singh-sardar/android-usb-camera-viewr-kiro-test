package com.example.usbcameraviewer

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: UsbCameraManager
    private lateinit var audioManager: UsbAudioManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var cameraView: CameraTextureView
    private lateinit var sidebarLayout: ScrollView
    private lateinit var toggleSidebarButton: Button
    private lateinit var closeSidebarButton: Button
    private lateinit var deviceSpinner: Spinner
    private lateinit var resolutionSpinner: Spinner
    private lateinit var fpsSpinner: Spinner
    private lateinit var rotationSpinner: Spinner
    private lateinit var flipHorizontalCheckbox: CheckBox
    private lateinit var flipVerticalCheckbox: CheckBox
    private lateinit var applyButton: Button
    private lateinit var autoStartButton: Button
    private lateinit var cameraControlsButton: Button
    private lateinit var viewLogsButton: Button
    private lateinit var statusText: TextView
    private lateinit var autoLaunchCheckbox: CheckBox
    private lateinit var volumeSeekBar: SeekBar
    private var usbReceiver: UsbDeviceReceiver? = null
    private var isInitializing = true
    private var cameraControls = CameraControls()
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        
        if (cameraGranted) {
            initializeCamera()
            if (!audioGranted) {
                Toast.makeText(this, "Audio permission denied - camera will work without audio", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            
            initializeViews()
            setupListeners()
            
            settingsManager = SettingsManager(this)
            cameraManager = UsbCameraManager(this)
            audioManager = UsbAudioManager(this)
            
            // Load saved settings
            loadSavedSettings()
            
            // Check if launched by USB device attach
            handleUsbIntent(intent)
        
            if (checkPermissions()) {
                initializeCamera()
            } else {
                requestPermissionsLauncher.launch(arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleUsbIntent(it) }
    }
    
    private fun handleUsbIntent(intent: android.content.Intent) {
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            // Check if auto-launch is enabled
            if (settingsManager.isAutoLaunchEnabled()) {
                val device = intent.getParcelableExtra<android.hardware.usb.UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Toast.makeText(this, "USB Camera detected: ${it.productName ?: it.deviceName}", Toast.LENGTH_SHORT).show()
                    // Camera will be detected and connected automatically by the manager
                }
            }
        }
    }
    
    private fun initializeViews() {
        try {
            cameraView = findViewById(R.id.cameraView) ?: throw IllegalStateException("cameraView not found")
            sidebarLayout = findViewById(R.id.sidebarLayout) ?: throw IllegalStateException("sidebarLayout not found")
            toggleSidebarButton = findViewById(R.id.toggleSidebarButton) ?: throw IllegalStateException("toggleSidebarButton not found")
            closeSidebarButton = findViewById(R.id.closeSidebarButton) ?: throw IllegalStateException("closeSidebarButton not found")
            deviceSpinner = findViewById(R.id.deviceSpinner) ?: throw IllegalStateException("deviceSpinner not found")
            resolutionSpinner = findViewById(R.id.resolutionSpinner) ?: throw IllegalStateException("resolutionSpinner not found")
            fpsSpinner = findViewById(R.id.fpsSpinner) ?: throw IllegalStateException("fpsSpinner not found")
            rotationSpinner = findViewById(R.id.rotationSpinner) ?: throw IllegalStateException("rotationSpinner not found")
            flipHorizontalCheckbox = findViewById(R.id.flipHorizontalCheckbox) ?: throw IllegalStateException("flipHorizontalCheckbox not found")
            flipVerticalCheckbox = findViewById(R.id.flipVerticalCheckbox) ?: throw IllegalStateException("flipVerticalCheckbox not found")
            applyButton = findViewById(R.id.applyButton) ?: throw IllegalStateException("applyButton not found")
            autoStartButton = findViewById(R.id.autoStartButton) ?: throw IllegalStateException("autoStartButton not found")
            cameraControlsButton = findViewById(R.id.cameraControlsButton) ?: throw IllegalStateException("cameraControlsButton not found")
            viewLogsButton = findViewById(R.id.viewLogsButton) ?: throw IllegalStateException("viewLogsButton not found")
            statusText = findViewById(R.id.statusText) ?: throw IllegalStateException("statusText not found")
            autoLaunchCheckbox = findViewById(R.id.autoLaunchCheckbox) ?: throw IllegalStateException("autoLaunchCheckbox not found")
            volumeSeekBar = findViewById(R.id.volumeSeekBar) ?: throw IllegalStateException("volumeSeekBar not found")
            
            // Hide sidebar initially
            sidebarLayout.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Error finding views: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }
    
    private fun setupListeners() {
        toggleSidebarButton.setOnClickListener {
            sidebarLayout.visibility = if (sidebarLayout.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        
        closeSidebarButton.setOnClickListener {
            sidebarLayout.visibility = View.GONE
        }
        
        applyButton.setOnClickListener {
            applyConfiguration()
        }
        
        autoStartButton.setOnClickListener {
            autoStartVideo()
        }
        
        cameraControlsButton.setOnClickListener {
            showCameraControls()
        }
        
        viewLogsButton.setOnClickListener {
            startActivity(android.content.Intent(this, LogViewerActivity::class.java))
        }
        
        // Apply changes while changing values
        deviceSpinner.onItemSelectedListener = createAutoApplyListener()
        resolutionSpinner.onItemSelectedListener = createAutoApplyListener()
        fpsSpinner.onItemSelectedListener = createAutoApplyListener()
        rotationSpinner.onItemSelectedListener = createAutoApplyListener()
        
        flipHorizontalCheckbox.setOnCheckedChangeListener { _, _ -> applyConfiguration() }
        flipVerticalCheckbox.setOnCheckedChangeListener { _, _ -> applyConfiguration() }
        
        autoLaunchCheckbox.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setAutoLaunch(isChecked)
        }
        
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volume = progress / 100f
                audioManager.setVolume(volume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun createAutoApplyListener() = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            applyConfiguration()
        }
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    
    private fun initializeCamera() {
        // Check if device supports USB Host
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_USB_HOST)) {
            Toast.makeText(
                this,
                "USB Host not supported on this device. USB camera features will be limited.",
                Toast.LENGTH_LONG
            ).show()
            statusText.text = "USB Host not supported"
            statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
        }
        
        cameraManager.scanDevices()
        
        lifecycleScope.launch {
            cameraManager.availableDevices.collect { devices ->
                updateDeviceSpinner(devices)
            }
        }
        
        lifecycleScope.launch {
            cameraManager.cameraState.collect { state ->
                updateCameraState(state)
            }
        }
        
        lifecycleScope.launch {
            audioManager.audioState.collect { state ->
                updateAudioState(state)
            }
        }
        
        setupSpinners()
        
        cameraView.surfaceAvailableCallback = { surface ->
            cameraManager.startCamera(surface)
        }
    }
    
    private fun setupSpinners() {
        isInitializing = true
        
        // Resolution spinner
        val resolutions = cameraManager.getSupportedResolutions()
        val resAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, 
            resolutions.map { it.toString() })
        resAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        resolutionSpinner.adapter = resAdapter
        
        // FPS spinner
        val fpsList = cameraManager.getSupportedFps()
        val fpsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, 
            fpsList.map { "$it fps" })
        fpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fpsSpinner.adapter = fpsAdapter
        
        // Rotation spinner
        val rotations = listOf("0°", "90°", "180°", "270°")
        val rotAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rotations)
        rotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rotationSpinner.adapter = rotAdapter
        
        // Restore saved configuration
        val savedConfig = settingsManager.loadConfig()
        restoreUIFromConfig(savedConfig)
    }
    
    private fun updateDeviceSpinner(devices: List<android.hardware.usb.UsbDevice>) {
        val deviceNames = if (devices.isEmpty()) {
            listOf("No devices found")
        } else {
            devices.map { "${it.productName ?: it.deviceName}" }
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter
    }
    
    private fun updateCameraState(state: UsbCameraManager.CameraState) {
        when (state) {
            is UsbCameraManager.CameraState.Disconnected -> {
                statusText.text = "Camera disconnected"
                statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                audioManager.stopAudioCapture()
            }
            is UsbCameraManager.CameraState.Connecting -> {
                statusText.text = "Connecting..."
                statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
            is UsbCameraManager.CameraState.Connected -> {
                statusText.text = "Camera connected"
                statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                Toast.makeText(this, "✓ Camera connected successfully", Toast.LENGTH_SHORT).show()
                
                // Check if device has audio and start capture
                if (audioManager.hasAudioInterface(state.device)) {
                    audioManager.startAudioCapture()
                    Toast.makeText(this, "🔊 Audio enabled", Toast.LENGTH_SHORT).show()
                }
            }
            is UsbCameraManager.CameraState.Error -> {
                statusText.text = "Error: ${state.message}"
                statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                Toast.makeText(this, "❌ Error: ${state.message}", Toast.LENGTH_LONG).show()
                audioManager.stopAudioCapture()
            }
        }
    }
    
    private fun updateAudioState(state: UsbAudioManager.AudioState) {
        when (state) {
            is UsbAudioManager.AudioState.Recording -> {
                // Audio is being captured and played back
            }
            is UsbAudioManager.AudioState.Stopped -> {
                // Audio capture stopped
            }
            is UsbAudioManager.AudioState.Error -> {
                // Show audio error as toast
                Toast.makeText(this, "⚠ Audio error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun applyConfiguration() {
        if (isInitializing) return // Don't apply during initialization
        
        try {
            val resolutions = cameraManager.getSupportedResolutions()
            val selectedRes = resolutions.getOrNull(resolutionSpinner.selectedItemPosition) 
                ?: resolutions[1]
            
            val fpsList = cameraManager.getSupportedFps()
            val selectedFps = fpsList.getOrNull(fpsSpinner.selectedItemPosition) ?: 30
            
            val rotation = rotationSpinner.selectedItemPosition * 90
            
            val deviceName = if (cameraManager.availableDevices.value.isNotEmpty()) {
                cameraManager.availableDevices.value[deviceSpinner.selectedItemPosition].deviceName
            } else ""
            
            val config = CameraConfig(
                deviceName = deviceName,
                width = selectedRes.width,
                height = selectedRes.height,
                fps = selectedFps,
                rotation = rotation,
                flipHorizontal = flipHorizontalCheckbox.isChecked,
                flipVertical = flipVerticalCheckbox.isChecked
            )
            
            // Save settings
            settingsManager.saveConfig(config)
            
            cameraManager.updateConfig(config)
            cameraView.updateTransform(rotation, config.flipHorizontal, config.flipVertical)
            
            // Show configuration toast
            val flipInfo = mutableListOf<String>()
            if (config.flipHorizontal) flipInfo.add("H-Flip")
            if (config.flipVertical) flipInfo.add("V-Flip")
            val flipText = if (flipInfo.isNotEmpty()) " + ${flipInfo.joinToString(", ")}" else ""
            
            Toast.makeText(
                this,
                "📹 Running: ${selectedRes.width}x${selectedRes.height} @ ${selectedFps}fps, ${rotation}°$flipText",
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error applying config: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Error applying configuration", e)
        }
    }
    
    private fun loadSavedSettings() {
        val savedConfig = settingsManager.loadConfig()
        
        // These will be applied after spinners are populated
        // Store for later use
        cameraManager.updateConfig(savedConfig)
    }
    
    private fun restoreUIFromConfig(config: CameraConfig) {
        isInitializing = true
        
        // Restore resolution
        val resolutions = cameraManager.getSupportedResolutions()
        val resIndex = resolutions.indexOfFirst { it.width == config.width && it.height == config.height }
        if (resIndex >= 0) {
            resolutionSpinner.setSelection(resIndex)
        }
        
        // Restore FPS
        val fpsList = cameraManager.getSupportedFps()
        val fpsIndex = fpsList.indexOf(config.fps)
        if (fpsIndex >= 0) {
            fpsSpinner.setSelection(fpsIndex)
        }
        
        // Restore rotation
        val rotationIndex = config.rotation / 90
        rotationSpinner.setSelection(rotationIndex)
        
        // Restore flips
        flipHorizontalCheckbox.isChecked = config.flipHorizontal
        flipVerticalCheckbox.isChecked = config.flipVertical
        
        // Restore auto-launch
        autoLaunchCheckbox.isChecked = settingsManager.isAutoLaunchEnabled()
        
        // Apply transformations
        cameraView.updateTransform(config.rotation, config.flipHorizontal, config.flipVertical)
        
        isInitializing = false
    }
    
    private fun checkPermissions(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        val audioGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        return cameraGranted && audioGranted
    }
    
    override fun onResume() {
        super.onResume()
        registerUsbReceiver()
    }
    
    override fun onPause() {
        super.onPause()
        unregisterUsbReceiver()
    }
    
    private fun autoStartVideo() {
        Toast.makeText(this, "Auto-starting video with best configuration...", Toast.LENGTH_SHORT).show()
        
        // Stop current camera
        cameraManager.stopCamera()
        
        // Wait a moment then restart
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            
            // Set best configuration
            isInitializing = true
            
            // Select 720p (most compatible)
            val resolutions = cameraManager.getSupportedResolutions()
            val best720p = resolutions.indexOfFirst { it.width == 1280 && it.height == 720 }
            if (best720p >= 0) {
                resolutionSpinner.setSelection(best720p)
            } else if (resolutions.size > 1) {
                resolutionSpinner.setSelection(1) // Second option
            }
            
            // Select 30fps
            val fpsList = cameraManager.getSupportedFps()
            val fps30 = fpsList.indexOf(30)
            if (fps30 >= 0) {
                fpsSpinner.setSelection(fps30)
            }
            
            // No rotation
            rotationSpinner.setSelection(0)
            
            // No flips
            flipHorizontalCheckbox.isChecked = false
            flipVerticalCheckbox.isChecked = false
            
            isInitializing = false
            
            // Apply configuration
            applyConfiguration()
            
            // Restart camera
            kotlinx.coroutines.delay(500)
            cameraView.surfaceAvailableCallback?.let { callback ->
                cameraManager.startCamera(cameraView.currentSurface ?: return@launch)
            }
            
            Toast.makeText(this@MainActivity, "Video started with 720p@30fps", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showCameraControls() {
        val dialog = CameraControlsFragment.newInstance(cameraControls) { controls ->
            cameraControls = controls
            cameraManager.applyCameraControls(controls)
        }
        dialog.show(supportFragmentManager, "camera_controls")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
        cameraManager.release()
    }
    
    private fun registerUsbReceiver() {
        usbReceiver = UsbDeviceReceiver(
            onDeviceAttached = { device ->
                runOnUiThread {
                    Toast.makeText(this, "USB device attached", Toast.LENGTH_SHORT).show()
                    cameraManager.scanDevices()
                }
            },
            onDeviceDetached = { device ->
                runOnUiThread {
                    Toast.makeText(this, "USB device detached", Toast.LENGTH_SHORT).show()
                    cameraManager.scanDevices()
                }
            }
        )
        
        val filter = android.content.IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)
    }
    
    private fun unregisterUsbReceiver() {
        usbReceiver?.let {
            unregisterReceiver(it)
            usbReceiver = null
        }
    }
}
