package com.example.usbcameraviewer

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UsbAudioManager(private val context: Context) {
    
    private val TAG = "UsbAudioManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _audioState = MutableStateFlow<AudioState>(AudioState.Stopped)
    val audioState: StateFlow<AudioState> = _audioState
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    
    private val sampleRate = 48000 // Common USB audio sample rate
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    sealed class AudioState {
        object Stopped : AudioState()
        object Recording : AudioState()
        data class Error(val message: String) : AudioState()
    }
    
    fun hasAudioInterface(device: UsbDevice): Boolean {
        // USB Audio Class devices have class 1 (0x01)
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == 1) { // USB_CLASS_AUDIO
                return true
            }
        }
        return false
    }
    
    fun startAudioCapture() {
        if (isRecording) {
            Log.d(TAG, "Audio already recording")
            return
        }
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat
            )
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                _audioState.value = AudioState.Error("Invalid buffer size")
                return
            }
            
            // Create AudioRecord for capturing
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            // Create AudioTrack for playback
            val playbackChannelConfig = AudioFormat.CHANNEL_OUT_STEREO
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(playbackChannelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            audioRecord?.startRecording()
            audioTrack?.play()
            
            isRecording = true
            _audioState.value = AudioState.Recording
            
            // Start audio processing loop
            recordingJob = scope.launch {
                processAudio(bufferSize)
            }
            
            Log.d(TAG, "Audio capture started")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Audio permission denied", e)
            _audioState.value = AudioState.Error("Audio permission denied")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio capture", e)
            _audioState.value = AudioState.Error(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun processAudio(bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        
        while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                
                if (readSize > 0) {
                    // Write to audio track for playback
                    audioTrack?.write(buffer, 0, readSize)
                }
                
                // Small delay to prevent tight loop
                delay(1)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio", e)
                break
            }
        }
    }
    
    fun stopAudioCapture() {
        isRecording = false
        recordingJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            _audioState.value = AudioState.Stopped
            Log.d(TAG, "Audio capture stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }
    
    fun setVolume(volume: Float) {
        // Volume range 0.0 to 1.0
        val clampedVolume = volume.coerceIn(0f, 1f)
        audioTrack?.setVolume(clampedVolume)
    }
    
    fun release() {
        stopAudioCapture()
        scope.cancel()
    }
}
