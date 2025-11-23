package com.example.usbcameraviewer

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogViewerActivity : AppCompatActivity() {

    private lateinit var logsTextView: TextView
    private lateinit var logCountText: TextView
    private lateinit var logsScrollView: ScrollView
    private lateinit var clearLogsButton: Button
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        logsTextView = findViewById(R.id.logsTextView)
        logCountText = findViewById(R.id.logCountText)
        logsScrollView = findViewById(R.id.logsScrollView)
        clearLogsButton = findViewById(R.id.clearLogsButton)
        closeButton = findViewById(R.id.closeButton)

        clearLogsButton.setOnClickListener {
            AppLogger.clear()
            updateLogs()
        }

        closeButton.setOnClickListener {
            finish()
        }

        updateLogs()
    }

    override fun onResume() {
        super.onResume()
        updateLogs()
    }

    private fun updateLogs() {
        val logs = AppLogger.getLogs()
        logCountText.text = "${logs.size} logs"

        if (logs.isEmpty()) {
            logsTextView.text = "No logs yet..."
        } else {
            val formattedLogs = logs.joinToString("\n\n") { entry ->
                val color = when (entry.level) {
                    "ERROR" -> "🔴"
                    "WARN" -> "🟡"
                    "INFO" -> "🔵"
                    else -> "⚪"
                }
                "$color [${entry.timestamp}] ${entry.level}\n${entry.tag}: ${entry.message}"
            }
            logsTextView.text = formattedLogs

            // Scroll to bottom
            logsScrollView.post {
                logsScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}
