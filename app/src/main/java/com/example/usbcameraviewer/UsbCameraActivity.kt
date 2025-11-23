package com.example.usbcameraviewer

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, UsbCameraFragment())
                .commit()
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
    private lateinit var resolutionSpinner: Spinner
    private lateinit var fpsSpinner: Spinner
    private lateinit var rotationSpinner: Spinner
    private lateinit var flipHorizontalCheck: CheckBox
    private lateinit var flipVerticalCheck: CheckBox
    private lateinit var brightnessSeek: SeekBar
    private lateinit var contrastSeek: SeekBar
    private lateinit var saturationSeek: SeekBar
    
    // Camera and Settings
    private var cameraView: AspectRatioTextureView? = null
    private var settingsManager: SettingsManager? = null
    
    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mainLayout = FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        
        // Camera view container (full screen)
        cameraContainer = FrameLayout(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mainLayout.addView(cameraContainer)
        
        // Status bar at top
        statusText = TextView(requireContext()).apply {
            text = "Initializing..."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#AA000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
        }
        mainLayout.addView(statusText)
        
        // Toggle button
        toggleButton = Button(requireContext()).apply {
            text = "⚙️"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                setMargins(0, 60, 16, 0)
            }
            setOnClickListener { toggleSidebar() }
        }
        mainLayout.addView(toggleButton)
        
        // Sidebar
        createSidebar()
        
        return mainLayout
    }
    
    /**
     * Creates the sidebar with all camera controls
     * Sidebar is hidden by default and can be toggled with the settings button
     */
    private fun createSidebar() {
        sidebarScroll = ScrollView(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#DD222222"))
            layoutParams = FrameLayout.LayoutParams(
                (300 * resources.displayMetrics.density).toInt(),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.END
            )
            visibility = View.GONE
        }
        
        sidebarLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // Title
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "Camera Settings"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, 16)
        })
        
        // Auto config button
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "🎯 Auto Best Config"
            setOnClickListener { applyBestConfig() }
        })
        
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
            text = "Flip"
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 16, 0, 8)
        })
        
        flipHorizontalCheck = CheckBox(requireContext()).apply {
            text = "Flip Horizontal"
            setTextColor(android.graphics.Color.WHITE)
            setOnCheckedChangeListener { _, _ -> applyTransform() }
        }
        sidebarLayout.addView(flipHorizontalCheck)
        
        flipVerticalCheck = CheckBox(requireContext()).apply {
            text = "Flip Vertical"
            setTextColor(android.graphics.Color.WHITE)
            setOnCheckedChangeListener { _, _ -> applyTransform() }
        }
        sidebarLayout.addView(flipVerticalCheck)
        
        // Camera controls
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = "Camera Controls"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(0, 16, 0, 8)
        })
        
        brightnessSeek = createSeekBar("Brightness", -100, 100)
        contrastSeek = createSeekBar("Contrast", -100, 100)
        saturationSeek = createSeekBar("Saturation", -100, 100)
        
        // Logs button
        sidebarLayout.addView(Button(requireContext()).apply {
            text = "📋 View Logs"
            setOnClickListener {
                startActivity(android.content.Intent(requireContext(), LogViewerActivity::class.java))
            }
        })
        
        sidebarScroll.addView(sidebarLayout)
        mainLayout.addView(sidebarScroll)
    }
    
    private fun addSpinnerControl(label: String, items: List<String>): Spinner {
        sidebarLayout.addView(TextView(requireContext()).apply {
            text = label
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 16, 0, 8)
        })
        
        val spinner = Spinner(requireContext())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        sidebarLayout.addView(spinner)
        
        return spinner
    }
    
    private fun toggleSidebar() {
        sidebarScroll.visibility = if (sidebarScroll.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
    
    private fun createSeekBar(label: String, min: Int, max: Int): SeekBar {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        
        layout.addView(TextView(requireContext()).apply {
            text = label
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        
        val seekBar = SeekBar(requireContext()).apply {
            this.max = max - min
            progress = -min
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        layout.addView(seekBar)
        
        val valueText = TextView(requireContext()).apply {
            text = "0"
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.WRAP_CONTENT)
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
            cameraView = AspectRatioTextureView(requireContext())
        }
        return cameraView!!
    }
    
    override fun getCameraViewContainer(): ViewGroup = cameraContainer
    
    override fun getCameraClient(): CameraClient? {
        settingsManager = SettingsManager(requireContext())
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        
        return CameraClient.newBuilder(requireContext())
            .setEnableGLES(true)
            .setRawImage(false)
            .openDebug(true)
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
        setupSpinners()
        loadSavedConfig()
        statusText.text = "✓ Camera ready - Connect USB camera"
        statusText.setTextColor(android.graphics.Color.GREEN)
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
                statusText.text = "FPS: $fps"
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
     */
    private fun changeResolution(width: Int, height: Int) {
        updateResolution(width, height)
        
        val config = settingsManager?.loadConfig() ?: CameraConfig()
        settingsManager?.saveConfig(config.copy(width = width, height = height))
        
        statusText.text = "Resolution: ${width}x${height}"
        Toast.makeText(requireContext(), "Resolution updated", Toast.LENGTH_SHORT).show()
    }
    
    private fun applyBestConfig() {
        // Set 720p @ 30fps (most compatible)
        resolutionSpinner.setSelection(1) // 1280x720
        fpsSpinner.setSelection(2) // 30fps
        rotationSpinner.setSelection(0) // 0°
        flipHorizontalCheck.isChecked = false
        flipVerticalCheck.isChecked = false
        
        // Reset camera controls
        brightnessSeek.progress = brightnessSeek.max / 2
        contrastSeek.progress = contrastSeek.max / 2
        saturationSeek.progress = saturationSeek.max / 2
        
        Toast.makeText(requireContext(), "✓ Applied best configuration", Toast.LENGTH_SHORT).show()
    }
    
    private fun applyTransform() {
        val rotation = rotationSpinner.selectedItemPosition * 90
        val flipH = flipHorizontalCheck.isChecked
        val flipV = flipVerticalCheck.isChecked
        
        // Apply rotation using library's setRotateType
        val rotateType = when (rotation) {
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
        
        statusText.text = "Transform: ${rotation}° ${if(flipH) "H" else ""}${if(flipV) "V" else ""}"
    }
    
    override fun getGravity(): Int = Gravity.CENTER
}
