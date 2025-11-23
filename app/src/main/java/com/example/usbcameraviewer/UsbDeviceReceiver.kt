package com.example.usbcameraviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

class UsbDeviceReceiver(
    private val onDeviceAttached: (UsbDevice) -> Unit,
    private val onDeviceDetached: (UsbDevice) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "UsbDeviceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "USB device attached: ${it.deviceName}")
                    onDeviceAttached(it)
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "USB device detached: ${it.deviceName}")
                    onDeviceDetached(it)
                }
            }
        }
    }
}
