package com.example.util

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyDebriefReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val isSystemReschedule = action == Intent.ACTION_BOOT_COMPLETED ||
                action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED

        if (isSystemReschedule) {
            NotificationHelper.rescheduleSavedDailyDebriefAlarm(context)
            return
        }

        val wakeLock = NotificationHelper.acquireAlarmWakeLock(context, "Recall:DailyDebriefWakeLock")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Immediately schedule tomorrow's Daily Debrief exact alarm
                NotificationHelper.rescheduleSavedDailyDebriefAlarm(context, forceNextDay = true)

                // Show lock-screen & heads-up notification
                NotificationHelper.showDebriefNotification(context, playToneAsync = false)

                // Play physical chime and vibration while WakeLock & goAsync keep process alive
                ReminderToneManager.playPhysicalToneAndVibrateSuspend(
                    context = context,
                    tone = ReminderTone.COSMIC_CHIME,
                    vibratePattern = ReminderVibratePattern.DOUBLE_TAP
                )
            } catch (_: Throwable) {
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Throwable) {
                }
                try {
                    pendingResult.finish()
                } catch (_: Throwable) {
                }
            }
        }
    }
}

object NotificationHelper {
    const val CHANNEL_ID = "recall_daily_debrief_v2"
    const val ACTION_DAILY_DEBRIEF_ALARM = "com.example.ACTION_DAILY_DEBRIEF_ALARM"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "recall_app_prefs"
    private const val KEY_REMINDER_HOUR = "pref_debrief_reminder_hour"
    private const val KEY_REMINDER_MINUTE = "pref_debrief_reminder_minute"

    fun acquireAlarmWakeLock(context: Context, tag: String, timeoutMs: Long = 10_000L): PowerManager.WakeLock? {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                tag
            )
            wl.setReferenceCounted(false)
            wl.acquire(timeoutMs)
            wl
        } catch (_: Throwable) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
                wl?.setReferenceCounted(false)
                wl?.acquire(timeoutMs)
                wl
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Debrief Alarms"
            val descriptionText = "Evening prompts and lock-screen alerts to unpack your day into personal memory."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 160, 90, 220)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showDebriefNotification(context: Context, playToneAsync: Boolean = true) {
        createNotificationChannel(context)

        if (playToneAsync) {
            ReminderToneManager.playPhysicalToneAndVibrate(
                context = context,
                tone = ReminderTone.COSMIC_CHIME,
                vibratePattern = ReminderVibratePattern.DOUBLE_TAP
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "debrief")
            putExtra("from_alarm_or_reminder", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("🎙️ Unpack Your Day • Recall Debrief")
            .setContentText("Tap to record your thoughts, living notes, and actions from today.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Ready for your Daily Debrief? Tap to speak or type what happened today and automatically update your notes & people."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Phase 5: Posts a high-priority Proactive Assistant Anticipatory Briefing notification
     * summarizing open commitments, upcoming dates, and overdue catch-ups.
     */
    fun showProactiveBriefingNotification(
        context: Context,
        title: String,
        summaryText: String,
        playToneAsync: Boolean = true
    ) {
        createNotificationChannel(context)

        if (playToneAsync) {
            ReminderToneManager.playPhysicalToneAndVibrate(
                context = context,
                tone = ReminderTone.DIGITAL_CRYSTAL,
                vibratePattern = ReminderVibratePattern.DOUBLE_TAP
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "stream")
            putExtra("from_alarm_or_reminder", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            105,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID + 5, builder.build())
    }

    fun rescheduleSavedDailyDebriefAlarm(context: Context, forceNextDay: Boolean = false) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedHour = prefs.getInt(KEY_REMINDER_HOUR, 20)
            val savedMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
            if (savedHour < 0) {
                cancelDailyDebriefAlarm(context)
            } else {
                scheduleDailyDebriefAlarm(
                    context = context,
                    hourOfDay = savedHour,
                    minute = savedMinute,
                    forceNextDay = forceNextDay
                )
            }
        } catch (_: Throwable) {
            scheduleDailyDebriefAlarm(context, hourOfDay = 20, minute = 0, forceNextDay = forceNextDay)
        }
    }

    fun cancelDailyDebriefAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyDebriefReceiver::class.java).apply {
            action = ACTION_DAILY_DEBRIEF_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleDailyDebriefAlarm(
        context: Context,
        hourOfDay: Int = 20,
        minute: Int = 0,
        forceNextDay: Boolean = false
    ) {
        createNotificationChannel(context)

        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_REMINDER_HOUR, hourOfDay)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply()
        } catch (_: Throwable) {
        }

        if (hourOfDay < 0) {
            cancelDailyDebriefAlarm(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyDebriefReceiver::class.java).apply {
            action = ACTION_DAILY_DEBRIEF_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (forceNextDay || timeInMillis <= now + 1000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "debrief")
            putExtra("from_alarm_or_reminder", true)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            101,
            showIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            // setAlarmClock pierces Doze mode, works when app is closed, and wakes locked screens
            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (_: Throwable) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }
}
