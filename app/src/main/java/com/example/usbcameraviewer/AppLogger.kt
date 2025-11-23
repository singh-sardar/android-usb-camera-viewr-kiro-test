package com.example.usbcameraviewer

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Application-wide logging system
 * Logs to both Logcat and in-memory buffer for in-app log viewer
 */
object AppLogger {
    private val logs = mutableListOf<LogEntry>()
    private val maxLogs = 500
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String
    )
    
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("DEBUG", tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("INFO", tag, message)
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("WARN", tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.message}\n${throwable.stackTraceToString().take(500)}"
        } else {
            message
        }
        addLog("ERROR", tag, fullMessage)
    }
    
    private fun addLog(level: String, tag: String, message: String) {
        synchronized(logs) {
            logs.add(LogEntry(
                timestamp = dateFormat.format(Date()),
                level = level,
                tag = tag,
                message = message
            ))
            
            // Keep only last maxLogs entries
            if (logs.size > maxLogs) {
                logs.removeAt(0)
            }
        }
    }
    
    fun getLogs(): List<LogEntry> {
        synchronized(logs) {
            return logs.toList()
        }
    }
    
    fun getLogsAsString(): String {
        synchronized(logs) {
            return logs.joinToString("\n") { entry ->
                "[${entry.timestamp}] ${entry.level} ${entry.tag}: ${entry.message}"
            }
        }
    }
    
    fun clear() {
        synchronized(logs) {
            logs.clear()
        }
    }
}
