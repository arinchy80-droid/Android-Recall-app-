package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ReminderToneManager {

    private val previewScope = CoroutineScope(Dispatchers.Default)
    private var activePlaybackJob: Job? = null

    /**
     * Plays the physical audible tone and triggers tactile haptic vibrations asynchronously.
     */
    fun playPhysicalToneAndVibrate(
        context: Context,
        tone: ReminderTone,
        vibratePattern: ReminderVibratePattern
    ) {
        activePlaybackJob?.cancel()
        activePlaybackJob = previewScope.launch {
            playPhysicalToneAndVibrateSuspend(context, tone, vibratePattern)
        }
    }

    /**
     * Suspend version for BroadcastReceivers (with WakeLock + goAsync) so the OS cannot
     * freeze or kill the background process before the physical tone and vibration complete.
     */
    suspend fun playPhysicalToneAndVibrateSuspend(
        context: Context,
        tone: ReminderTone,
        vibratePattern: ReminderVibratePattern
    ) {
        // 1. Trigger physical tactile vibration with ALARM usage so it fires over lock screen
        triggerTactileVibration(context, vibratePattern)

        // 2. Play acoustic message tone on ALARM stream (with NOTIFICATION fallback)
        playAcousticTone(context, tone)
    }

    /**
     * Preview tone & vibration when testing in UI.
     */
    fun previewPhysicalTone(
        context: Context,
        tone: ReminderTone,
        vibratePattern: ReminderVibratePattern
    ) {
        playPhysicalToneAndVibrate(context, tone, vibratePattern)
    }

    private fun triggerTactileVibration(
        context: Context,
        pattern: ReminderVibratePattern
    ) {
        if (pattern == ReminderVibratePattern.NONE) return

        try {
            val vibrator: Vibrator = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }) ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect: VibrationEffect? = when (pattern) {
                    ReminderVibratePattern.STRONG_PULSE -> {
                        val timings = longArrayOf(0, 220, 100, 220, 100, 320)
                        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                    ReminderVibratePattern.DOUBLE_TAP -> {
                        val timings = longArrayOf(0, 120, 90, 180)
                        val amplitudes = intArrayOf(0, 240, 0, 255)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                    ReminderVibratePattern.GENTLE_BUZZ -> {
                        VibrationEffect.createOneShot(220, 160)
                    }
                    ReminderVibratePattern.NONE -> null
                }

                if (effect != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val vibAttrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
                        vibrator.vibrate(effect, vibAttrs)
                    } else {
                        val audioAttrs = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(effect, audioAttrs)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (pattern) {
                    ReminderVibratePattern.STRONG_PULSE -> {
                        vibrator.vibrate(longArrayOf(0, 220, 100, 220, 100, 320), -1)
                    }
                    ReminderVibratePattern.DOUBLE_TAP -> {
                        vibrator.vibrate(longArrayOf(0, 120, 90, 180), -1)
                    }
                    ReminderVibratePattern.GENTLE_BUZZ -> {
                        vibrator.vibrate(220)
                    }
                    ReminderVibratePattern.NONE -> Unit
                }
            }
        } catch (_: Throwable) {
            // Safe fallback if vibrator is unavailable
        }
    }

    private fun createAlarmOrNotificationToneGenerator(volume: Int): ToneGenerator? {
        return try {
            ToneGenerator(AudioManager.STREAM_ALARM, volume)
        } catch (_: Throwable) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
            } catch (_: Throwable) {
                null
            }
        }
    }

    private suspend fun playAcousticTone(
        context: Context,
        tone: ReminderTone
    ) {
        try {
            when (tone) {
                ReminderTone.DIGITAL_CRYSTAL -> {
                    val tg = createAlarmOrNotificationToneGenerator(95)
                    if (tg != null) {
                        try {
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 240)
                            delay(260)
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 220)
                            delay(250)
                        } finally {
                            tg.release()
                        }
                    }
                }

                ReminderTone.COSMIC_CHIME -> {
                    val tones = listOf(
                        ToneGenerator.TONE_DTMF_3 to 150,
                        ToneGenerator.TONE_DTMF_6 to 150,
                        ToneGenerator.TONE_DTMF_9 to 280
                    )
                    tones.forEach { (toneId, durationMs) ->
                        val tg = createAlarmOrNotificationToneGenerator(92)
                        if (tg != null) {
                            try {
                                tg.startTone(toneId, durationMs)
                                delay(durationMs.toLong() + 40)
                            } finally {
                                tg.release()
                            }
                        }
                    }
                }

                ReminderTone.GENTLE_HARP -> {
                    val tg = createAlarmOrNotificationToneGenerator(88)
                    if (tg != null) {
                        try {
                            tg.startTone(ToneGenerator.TONE_PROP_ACK, 340)
                            delay(360)
                        } finally {
                            tg.release()
                        }
                    }
                }

                ReminderTone.URGENT_PULSE -> {
                    val tg = createAlarmOrNotificationToneGenerator(100)
                    if (tg != null) {
                        try {
                            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 300)
                            delay(320)
                            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 300)
                            delay(320)
                        } finally {
                            tg.release()
                        }
                    }
                }

                ReminderTone.SYSTEM_NOTIFICATION -> {
                    playSystemRingtoneFallback(context)
                }
            }
        } catch (_: Throwable) {
            playSystemRingtoneFallback(context)
        }
    }

    private suspend fun playSystemRingtoneFallback(context: Context) {
        try {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, defaultUri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
                delay(1200)
            }
        } catch (_: Throwable) {
        }
    }
}
