package org.example.project.sound

import android.media.AudioManager
import android.media.ToneGenerator

actual object SoundPlayer {
    actual fun play(sound: AppSound, enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                val (tone, ms) = when (sound) {
                    AppSound.YOUR_TURN -> ToneGenerator.TONE_PROP_BEEP to 250
                    AppSound.VOTE_STARTED -> ToneGenerator.TONE_PROP_BEEP2 to 400
                    AppSound.REMATCH -> ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE to 300
                }
                tg.startTone(tone, ms)
                Thread.sleep(ms.toLong() + 150)
                tg.release()
            } catch (_: Exception) {}
        }.start()
    }
}
