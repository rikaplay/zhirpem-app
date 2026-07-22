package com.RIKAPLAY.zhirpem_app.ui

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var currentSpeed = 1.0f

    fun play(audioUrlOrPath: String, onProgressUpdate: (Float, Long) -> Unit, onCompletion: () -> Unit) {
        stop()
        
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioUrlOrPath)
                prepareAsync()
                setOnPreparedListener {
                    applySpeed(currentSpeed)
                    start()
                    startProgressUpdates(onProgressUpdate)
                }
                setOnCompletionListener {
                    stopProgressUpdates()
                    onCompletion()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Playback failed", e)
            }
        }
    }

    private fun startProgressUpdates(onProgressUpdate: (Float, Long) -> Unit) {
        playbackJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val duration = mediaPlayer?.duration ?: 1
                onProgressUpdate(current.toFloat() / duration, current.toLong())
                delay(100)
            }
        }
    }

    private fun stopProgressUpdates() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun togglePlayPause(onProgressUpdate: (Float, Long) -> Unit): Boolean {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                stopProgressUpdates()
                return false
            } else {
                it.start()
                startProgressUpdates(onProgressUpdate)
                return true
            }
        }
        return false
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let {
            val duration = it.duration
            val newPos = (progress * duration).toInt()
            it.seekTo(newPos)
        }
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        mediaPlayer?.let {
            try {
                it.playbackParams = it.playbackParams.setSpeed(speed)
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error setting speed", e)
            }
        }
    }

    fun stop() {
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
