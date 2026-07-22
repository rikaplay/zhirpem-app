package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.media.MediaPlayer

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playSplashSound(context: Context) {
        // Если уже играет, не запускаем заново или сбрасываем
        mediaPlayer?.stop()
        mediaPlayer?.release()
        
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.splash_sound)
            mediaPlayer?.setOnCompletionListener {
                release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
