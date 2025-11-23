package com.example.usbcameraviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class CameraControlsFragment : DialogFragment() {
    
    private var onControlsChanged: ((CameraControls) -> Unit)? = null
    private lateinit var controls: CameraControls
    
    companion object {
        fun newInstance(controls: CameraControls, callback: (CameraControls) -> Unit): CameraControlsFragment {
            return CameraControlsFragment().apply {
                this.controls = controls
                this.onControlsChanged = callback
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createControlsView()
    }
    
    private fun createControlsView(): View {
        val context = requireContext()
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        layout.addView(TextView(context).apply {
            text = "Camera Controls"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        })
        
        // Brightness
        addSliderControl(layout, "Brightness", controls.brightness, 0, 100) { value ->
            controls.brightness = value
            onControlsChanged?.invoke(controls)
        }
        
        // Contrast
        addSliderControl(layout, "Contrast", controls.contrast, 0, 100) { value ->
            controls.contrast = value
            onControlsChanged?.invoke(controls)
        }
        
        // Saturation
        addSliderControl(layout, "Saturation", controls.saturation, 0, 100) { value ->
            controls.saturation = value
            onControlsChanged?.invoke(controls)
        }
        
        // Auto Focus
        layout.addView(Switch(context).apply {
            text = "Auto Focus"
            isChecked = controls.autoFocus
            setOnCheckedChangeListener { _, isChecked ->
                controls.autoFocus = isChecked
                onControlsChanged?.invoke(controls)
            }
        })
        
        // Auto White Balance
        layout.addView(Switch(context).apply {
            text = "Auto White Balance"
            isChecked = controls.autoWhiteBalance
            setOnCheckedChangeListener { _, isChecked ->
                controls.autoWhiteBalance = isChecked
                onControlsChanged?.invoke(controls)
            }
        })
        
        return layout
    }
    
    private fun addSliderControl(
        parent: ViewGroup,
        label: String,
        initialValue: Int,
        min: Int,
        max: Int,
        onChange: (Int) -> Unit
    ) {
        val context = requireContext()
        
        val labelView = TextView(context).apply {
            text = "$label: $initialValue"
            setPadding(0, 16, 0, 8)
        }
        parent.addView(labelView)
        
        parent.addView(SeekBar(context).apply {
            this.max = max - min
            progress = initialValue - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + min
                    labelView.text = "$label: $value"
                    onChange(value)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
    }
}

data class CameraControls(
    var brightness: Int = 50,
    var contrast: Int = 50,
    var saturation: Int = 50,
    var autoFocus: Boolean = true,
    var autoWhiteBalance: Boolean = true
)
