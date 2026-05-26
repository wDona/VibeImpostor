package org.example.project.sound

actual object SoundPlayer {
    actual fun play(sound: AppSound, enabled: Boolean) {
        if (!enabled) return
        when (sound) {
            AppSound.YOUR_TURN -> playWebTone(880.0, 0.0, 1100.0, 0.18)
            AppSound.VOTE_STARTED -> {
                playWebTone(440.0, 0.0, 440.0, 0.12)
                playWebTone(660.0, 0.15, 660.0, 0.12)
            }
            AppSound.REMATCH -> playWebTone(523.0, 0.0, 784.0, 0.25)
        }
    }
}

@JsFun("""(f1, d, f2, dur) => {
    try {
        var AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return;
        var ctx = new AudioCtx();
        var osc = ctx.createOscillator();
        var gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.frequency.setValueAtTime(f1, ctx.currentTime + d);
        osc.frequency.linearRampToValueAtTime(f2, ctx.currentTime + d + dur);
        gain.gain.setValueAtTime(0.28, ctx.currentTime + d);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + d + dur);
        osc.start(ctx.currentTime + d);
        osc.stop(ctx.currentTime + d + dur + 0.05);
    } catch(e) {}
}""")
private external fun playWebTone(startFreq: Double, delayS: Double, endFreq: Double, durS: Double)
