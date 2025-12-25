package com.example.usbcameraviewer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Dedicated launcher activity for better compatibility with Android TV boxes and launchers
 * This activity immediately launches the main camera activity
 */
class LauncherActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immediately launch the main camera activity
        val intent = Intent(this, UsbCameraActivity::class.java)
        
        // Forward any intent data (like USB device attached)
        if (getIntent().extras != null) {
            intent.putExtras(getIntent().extras!!)
        }
        if (getIntent().action != null) {
            intent.action = getIntent().action
        }
        if (getIntent().data != null) {
            intent.data = getIntent().data
        }
        
        startActivity(intent)
        finish() // Close this launcher activity
    }
}