package org.example.project.sound

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.sin

actual object SoundPlayer {
    actual fun play(sound: AppSound, enabled: Boolean) {
        if (!enabled) return
        Thread {
            try {
                when (sound) {
                    AppSound.YOUR_TURN -> {
                        playTone(880.0, 150)
                        Thread.sleep(20)
                        playTone(1100.0, 150)
                    }
                    AppSound.VOTE_STARTED -> {
                        playTone(440.0, 120)
                        Thread.sleep(20)
                        playTone(660.0, 150)
                    }
                    AppSound.REMATCH -> {
                        playTone(523.0, 100)
                        Thread.sleep(15)
                        playTone(659.0, 100)
                        Thread.sleep(15)
                        playTone(784.0, 180)
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun playTone(frequency: Double, durationMs: Int) {
        val sampleRate = 44100
        val samples = sampleRate * durationMs / 1000
        val buffer = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                i < samples * 0.05 -> i / (samples * 0.05)
                i > samples * 0.7 -> 1.0 - (i - samples * 0.7) / (samples * 0.3)
                else -> 1.0
            }
            val value = (sin(2.0 * PI * frequency * t) * envelope * Short.MAX_VALUE * 0.4).toInt()
            buffer[i * 2] = (value and 0xFF).toByte()
            buffer[i * 2 + 1] = (value shr 8).toByte()
        }
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val info = DataLine.Info(SourceDataLine::class.java, format)
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(format)
        line.start()
        line.write(buffer, 0, buffer.size)
        line.drain()
        line.close()
    }
}
