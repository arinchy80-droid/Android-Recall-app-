package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.RecallDatabase
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderTone
import com.example.data.local.entity.ReminderVibratePattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val wakeLock = NotificationHelper.acquireAlarmWakeLock(context, "Recall:ReminderReceiverWakeLock")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderNotificationHelper.ACTION_REMINDER_ALARM -> {
                        val reminderId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_ID) ?: return@launch
                        val dbReminder = try {
                            RecallDatabase.getInstance(context).reminderDao().getReminderById(reminderId)
                        } catch (_: Throwable) {
                            null
                        }

                        if (dbReminder?.isCompleted == true) {
                            return@launch
                        }

                        val title = intent.getStringExtra(ReminderNotificationHelper.EXTRA_TITLE)
                            ?: dbReminder?.title
                            ?: "Recall Reminder"
                        val details = intent.getStringExtra(ReminderNotificationHelper.EXTRA_DETAILS)
                            ?: dbReminder?.details
                            ?: ""
                        val noteId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_NOTE_ID)
                            ?: dbReminder?.noteId
                        val toneType = intent.getStringExtra(ReminderNotificationHelper.EXTRA_TONE)
                            ?: dbReminder?.toneType
                            ?: ReminderTone.DIGITAL_CRYSTAL.name
                        val vibratePattern = intent.getStringExtra(ReminderNotificationHelper.EXTRA_VIBRATE)
                            ?: dbReminder?.vibratePattern
                            ?: ReminderVibratePattern.STRONG_PULSE.name

                        // 1. Show high-priority lock-screen & heads-up notification immediately
                        ReminderNotificationHelper.showReminderNotification(
                            context = context,
                            reminderId = reminderId,
                            title = title,
                            details = details,
                            noteId = noteId,
                            toneType = toneType,
                            vibratePattern = vibratePattern,
                            playToneAsync = false
                        )

                        // 2. Play physical tone and vibration synchronously inside goAsync + WakeLock
                        val resolvedTone = try {
                            ReminderTone.valueOf(toneType)
                        } catch (_: Exception) {
                            ReminderTone.DIGITAL_CRYSTAL
                        }
                        val resolvedVibrate = try {
                            ReminderVibratePattern.valueOf(vibratePattern)
                        } catch (_: Exception) {
                            ReminderVibratePattern.STRONG_PULSE
                        }
                        ReminderToneManager.playPhysicalToneAndVibrateSuspend(
                            context = context,
                            tone = resolvedTone,
                            vibratePattern = resolvedVibrate
                        )
                    }

                    ReminderNotificationHelper.ACTION_REMINDER_DONE -> {
                        val reminderId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_ID) ?: return@launch
                        val notificationId = reminderId.hashCode()
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        notificationManager?.cancel(notificationId)

                        val db = RecallDatabase.getInstance(context)
                        db.reminderDao().setCompleted(reminderId, true)
                        ReminderNotificationHelper.cancelReminderAlarm(context, reminderId)
                    }

                    ReminderNotificationHelper.ACTION_REMINDER_SNOOZE -> {
                        val reminderId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_ID) ?: return@launch
                        val notificationId = reminderId.hashCode()
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        notificationManager?.cancel(notificationId)

                        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes snooze
                        val db = RecallDatabase.getInstance(context)
                        db.reminderDao().snoozeReminder(reminderId, snoozeTime)
                        val updated = db.reminderDao().getReminderById(reminderId)
                        if (updated != null) {
                            ReminderNotificationHelper.scheduleReminderAlarm(context, updated)
                        }
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    "android.intent.action.LOCKED_BOOT_COMPLETED",
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        ReminderNotificationHelper.rescheduleAllActiveReminders(context)
                        NotificationHelper.rescheduleSavedDailyDebriefAlarm(context)
                    }
                }
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

object ReminderNotificationHelper {
    const val CHANNEL_ID = "recall_physical_reminders_v2"
    const val ACTION_REMINDER_ALARM = "com.example.ACTION_REMINDER_ALARM"
    const val ACTION_REMINDER_DONE = "com.example.ACTION_REMINDER_DONE"
    const val ACTION_REMINDER_SNOOZE = "com.example.ACTION_REMINDER_SNOOZE"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_DETAILS = "extra_reminder_details"
    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_TONE = "extra_tone_type"
    const val EXTRA_VIBRATE = "extra_vibrate_pattern"

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notificationsEnabled = try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Throwable) {
            true
        }
        return runtimeGranted && notificationsEnabled
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        } catch (_: Throwable) {
        }
        openNotificationSettings(context)
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
        }
    }

    fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Physical Tone Reminders & Alarms"
            val descriptionText = "High-priority lock-screen & closed-app audible chimes and vibration alerts for memory reminders."
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
                vibrationPattern = longArrayOf(0, 220, 100, 220, 100, 320)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    suspend fun rescheduleAllActiveReminders(context: Context) {
        try {
            val db = RecallDatabase.getInstance(context)
            // Include reminders slightly in the past (up to 1 minute) or in the future
            val upcoming = db.reminderDao().getUpcomingActiveReminders(System.currentTimeMillis() - 60_000L)
            upcoming.forEach { reminder ->
                scheduleReminderAlarm(context, reminder)
            }
        } catch (_: Throwable) {
        }
    }

    fun scheduleReminderAlarm(context: Context, reminder: ReminderEntity) {
        createReminderChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_DETAILS, reminder.details)
            putExtra(EXTRA_NOTE_ID, reminder.noteId)
            putExtra(EXTRA_TONE, reminder.toneType)
            putExtra(EXTRA_VIBRATE, reminder.vibratePattern)
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerAtMillis = maxOf(reminder.reminderTimeMillis, System.currentTimeMillis() + 1000L)

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", if (reminder.noteId.isNullOrBlank()) "reminders" else "stream")
            putExtra("from_alarm_or_reminder", true)
            if (!reminder.noteId.isNullOrBlank()) {
                putExtra("open_note_id", reminder.noteId)
            }
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 3,
            showIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            // setAlarmClock pierces Doze mode, works when the app is shut/killed, and wakes locked screens
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (_: Throwable) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun cancelReminderAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
        }
        val requestCode = reminderId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        details: String,
        noteId: String?,
        toneType: String,
        vibratePattern: String,
        playToneAsync: Boolean = true
    ) {
        createReminderChannel(context)

        val resolvedTone = try {
            ReminderTone.valueOf(toneType)
        } catch (_: Exception) {
            ReminderTone.DIGITAL_CRYSTAL
        }
        val resolvedVibrate = try {
            ReminderVibratePattern.valueOf(vibratePattern)
        } catch (_: Exception) {
            ReminderVibratePattern.STRONG_PULSE
        }

        if (playToneAsync) {
            ReminderToneManager.playPhysicalToneAndVibrate(context, resolvedTone, resolvedVibrate)
        }

        // Open app intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", if (noteId.isNullOrBlank()) "reminders" else "stream")
            putExtra("from_alarm_or_reminder", true)
            if (!noteId.isNullOrBlank()) {
                putExtra("open_note_id", noteId)
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // "Mark as Done" action intent
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_DONE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            doneIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // "Snooze 10m" action intent
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = reminderId.hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("🔔 $title")
            .setContentText(details.ifBlank { "Scheduled Recall Reminder • Tap to review memory" })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    details.ifBlank { "Reminder: $title\nTone: ${resolvedTone.displayName}" }
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, true)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification_small, "Done", donePendingIntent)
            .addAction(R.drawable.ic_notification_small, "Snooze 10m", snoozePendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, builder.build())
    }
}
