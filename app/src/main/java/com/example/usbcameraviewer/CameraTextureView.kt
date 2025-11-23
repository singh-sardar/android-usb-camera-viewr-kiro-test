package com.example.usbcameraviewer

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView

class CameraTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    var surfaceAvailableCallback: ((Surface) -> Unit)? = null
    var currentSurface: Surface? = null
        private set
    private var currentRotation = 0
    private var currentFlipH = false
    private var currentFlipV = false

    init {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        currentSurface = Surface(surface)
        surfaceAvailableCallback?.invoke(currentSurface!!)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateTransform(currentRotation, currentFlipH, currentFlipV)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        currentSurface?.release()
        currentSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Called when the surface texture is updated
    }

    fun updateTransform(rotation: Int, flipHorizontal: Boolean, flipVertical: Boolean) {
        currentRotation = rotation
        currentFlipH = flipHorizontal
        currentFlipV = flipVertical
        
        val matrix = Matrix()
        val centerX = width / 2f
        val centerY = height / 2f

        // Apply rotation
        matrix.postRotate(rotation.toFloat(), centerX, centerY)

        // Apply flips
        val scaleX = if (flipHorizontal) -1f else 1f
        val scaleY = if (flipVertical) -1f else 1f
        matrix.postScale(scaleX, scaleY, centerX, centerY)

        setTransform(matrix)
    }
}
