package com.pixelkeys.ime.haptics

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pixelkeys.ime.data.prefs.HapticStyle

class HapticController(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun performKeyPress(durationMs: Int, style: HapticStyle, soundEnabled: Boolean) {
        if (soundEnabled) {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f)
        }
        if (durationMs <= 0 || vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = when (style) {
                HapticStyle.CRISP -> 200
                HapticStyle.THOCKY -> 255
                HapticStyle.SOFT -> 110
            }
            val effect = VibrationEffect.createOneShot(durationMs.toLong(), amplitude)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs.toLong())
        }
    }

    fun performTrackpadPulse() {
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(24L, 220))
        }
    }
}
