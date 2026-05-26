package org.example.project.sound

enum class AppSound {
    YOUR_TURN,
    VOTE_STARTED,
    REMATCH
}

expect object SoundPlayer {
    fun play(sound: AppSound, enabled: Boolean)
}
