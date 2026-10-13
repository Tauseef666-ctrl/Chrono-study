package com.chronostudy.app.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.chronostudy.app.R
import com.chronostudy.app.domain.model.AlertMode

data class SoundOption(val key: String, val label: String, val resId: Int)

object SoundCatalog {
    val options = listOf(
        SoundOption("chime", "Chime", R.raw.chime),
        SoundOption("bell", "Bell", R.raw.bell),
        SoundOption("marimba", "Marimba", R.raw.marimba),
        SoundOption("soft", "Soft", R.raw.soft),
        SoundOption("digital", "Digital", R.raw.digital),
        SoundOption("success", "Success", R.raw.success),
    )

    fun resIdFor(key: String): Int = options.firstOrNull { it.key == key }?.resId ?: R.raw.chime
}

data class VibrationOption(val key: String, val label: String, val pattern: LongArray)

object VibrationCatalog {
    val options = listOf(
        VibrationOption("default", "Standard", longArrayOf(0, 250, 150, 250)),
        VibrationOption("short", "Short", longArrayOf(0, 120)),
        VibrationOption("long", "Long", longArrayOf(0, 600)),
        VibrationOption("double", "Double", longArrayOf(0, 150, 100, 150, 100, 150)),
        VibrationOption("heartbeat", "Heartbeat", longArrayOf(0, 80, 60, 80, 200, 80, 60, 80)),
    )

    fun patternFor(key: String): LongArray = options.firstOrNull { it.key == key }?.pattern
        ?: options.first().pattern
}

/**
 * Plays completion alerts. Whether a sound or vibration is actually delivered still depends on
 * device state (mute, Do Not Disturb, disabled channels); this class cannot override that and
 * does not claim to.
 */
class AlertPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun play(
        mode: AlertMode,
        soundKey: String,
        vibrationKey: String,
        repeat: Boolean = false,
    ) {
        when (mode) {
            AlertMode.SOUND_AND_VIBRATION -> {
                playSound(soundKey)
                vibrate(vibrationKey, repeat)
            }
            AlertMode.SOUND_ONLY -> playSound(soundKey)
            AlertMode.VIBRATION_ONLY -> vibrate(vibrationKey, repeat)
            AlertMode.SILENT -> Unit
        }
    }

    fun playSound(key: String) {
        stopSound()
        runCatching {
            mediaPlayer = MediaPlayer.create(context, SoundCatalog.resIdFor(key))?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer === it) mediaPlayer = null
                }
                start()
            }
        }
    }

    fun vibrate(key: String, repeat: Boolean = false) {
        val vibrator: Vibrator = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }) ?: return

        if (!vibrator.hasVibrator()) return
        val pattern = VibrationCatalog.patternFor(key)
        val effect = VibrationEffect.createWaveform(pattern, if (repeat) 0 else -1)
        runCatching { vibrator.vibrate(effect) }
    }

    fun stopSound() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    fun stop() {
        stopSound()
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
        runCatching { vibrator?.cancel() }
    }
}
