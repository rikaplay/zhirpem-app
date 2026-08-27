package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.media.MediaPlayer

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playSplashSound(context: Context, isPremium: Boolean = false) {
        // Если уже играет, не запускаем заново или сбрасываем
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            val soundRes = if (isPremium) R.raw.splash_sound_premium else R.raw.splash_sound
            mediaPlayer = MediaPlayer.create(context, soundRes)
            mediaPlayer?.setOnCompletionListener {
                release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }
}
