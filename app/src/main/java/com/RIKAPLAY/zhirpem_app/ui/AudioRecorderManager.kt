package com.RIKAPLAY.zhirpem_app.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var currentFile: File? = null

    fun startRecording(onAmplitudeUpdate: (Float) -> Unit) {
        val file = File(context.cacheDir, "temp_voice_message_${System.currentTimeMillis()}.m4a")
        currentFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed", e)
            }
        }

        recordingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val maxAmplitude = mediaRecorder?.maxAmplitude ?: 0
                // Normalize amplitude (max is usually 32767 for 16-bit PCM)
                val normalized = (maxAmplitude.toFloat() / 32767f).coerceIn(0f, 1f)
                onAmplitudeUpdate(normalized)
                delay(80)
            }
        }
    }

    fun stopRecording(): File? {
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop failed", e)
        }
        mediaRecorder = null
        return currentFile
    }
}
