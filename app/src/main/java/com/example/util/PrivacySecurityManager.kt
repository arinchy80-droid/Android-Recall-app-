package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

enum class AudioRetentionPolicy(
    val displayName: String,
    val subtitle: String,
    val maxAgeDays: Int
) {
    DELETE_AFTER_TRANSCRIPTION(
        displayName = "Delete After Transcription",
        subtitle = "Immediately delete raw .m4a audio once unpacked into text memories",
        maxAgeDays = 0
    ),
    KEEP_7_DAYS(
        displayName = "Keep 7 Days",
        subtitle = "Automatically purge raw .m4a voice files older than 7 days",
        maxAgeDays = 7
    ),
    KEEP_30_DAYS(
        displayName = "Keep 30 Days",
        subtitle = "Automatically purge raw .m4a voice files older than 30 days",
        maxAgeDays = 30
    ),
    KEEP_INDEFINITELY(
        displayName = "Keep Indefinitely",
        subtitle = "Retain raw .m4a voice files on device until manually deleted",
        maxAgeDays = -1
    );

    companion object {
        fun fromName(name: String?): AudioRetentionPolicy {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: KEEP_30_DAYS
        }
    }
}

data class PrivacyAuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val details: String
)

data class AudioStorageStats(
    val fileCount: Int,
    val totalBytes: Long
) {
    fun formattedSize(): String {
        val kb = totalBytes / 1024.0
        val mb = kb / 1024.0
        return when {
            totalBytes <= 0L -> "0 KB"
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            else -> String.format(Locale.US, "%.1f KB", kb.coerceAtLeast(0.1))
        }
    }
}

/**
 * Phase 7: User Privacy, Persistent App Lock / PIN Gate, Audio Retention Controls,
 * Recording Consent Workflows & Local Audit Log Manager.
 */
object PrivacySecurityManager {

    private const val PREFS_NAME = "recall_privacy_security_prefs"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_PIN_SHA256 = "pin_sha256"
    private const val KEY_AUDIO_RETENTION = "audio_retention_policy"
    private const val KEY_RECORDING_CONSENT_PROMPT = "require_recording_consent_prompt"
    private const val KEY_AUDIT_LOGS = "privacy_audit_logs_v1"

    private val _isAppLockEnabled = MutableStateFlow(false)
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isSessionUnlocked = MutableStateFlow(true)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private val _audioRetentionPolicy = MutableStateFlow(AudioRetentionPolicy.KEEP_30_DAYS)
    val audioRetentionPolicy: StateFlow<AudioRetentionPolicy> = _audioRetentionPolicy.asStateFlow()

    private val _requireRecordingConsentPrompt = MutableStateFlow(false)
    val requireRecordingConsentPrompt: StateFlow<Boolean> = _requireRecordingConsentPrompt.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<PrivacyAuditEntry>>(emptyList())
    val auditLogs: StateFlow<List<PrivacyAuditEntry>> = _auditLogs.asStateFlow()

    private var currentPinHash: String = hashPin("1234")
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        initialized = true
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lockEnabled = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
            _isAppLockEnabled.value = lockEnabled
            _isSessionUnlocked.value = !lockEnabled
            currentPinHash = prefs.getString(KEY_PIN_SHA256, null) ?: hashPin("1234")
            _audioRetentionPolicy.value = AudioRetentionPolicy.fromName(
                prefs.getString(KEY_AUDIO_RETENTION, AudioRetentionPolicy.KEEP_30_DAYS.name)
            )
            _requireRecordingConsentPrompt.value = prefs.getBoolean(KEY_RECORDING_CONSENT_PROMPT, false)
            _auditLogs.value = decodeAuditLogs(prefs.getString(KEY_AUDIT_LOGS, null))

            // Enforce scheduled retention policy on startup for KEEP_7_DAYS / KEEP_30_DAYS
            if (_audioRetentionPolicy.value.maxAgeDays > 0) {
                enforceAudioRetentionPolicy(context)
            }
        } catch (_: Throwable) {
        }
    }

    fun hashPin(pin: String): String {
        val clean = pin.trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(("recall_salt_v1:$clean").toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun setAppLockEnabled(context: Context?, enabled: Boolean, customPin: String? = null) {
        if (!customPin.isNullOrBlank() && customPin.trim().length >= 4) {
            currentPinHash = hashPin(customPin.trim())
        }
        _isAppLockEnabled.value = enabled
        _isSessionUnlocked.value = true
        try {
            context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean(KEY_APP_LOCK_ENABLED, enabled)
                ?.putString(KEY_PIN_SHA256, currentPinHash)
                ?.apply()
        } catch (_: Throwable) {
        }
        recordAuditEvent(
            context = context,
            action = if (enabled) "App Lock Enabled" else "App Lock Disabled",
            details = if (enabled) "4-digit PIN vault lock active on app launch" else "PIN vault lock turned off"
        )
    }

    fun updatePin(context: Context?, newPin: String): Boolean {
        val clean = newPin.trim()
        if (clean.length < 4 || !clean.all { it.isDigit() }) return false
        currentPinHash = hashPin(clean)
        try {
            context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_PIN_SHA256, currentPinHash)
                ?.apply()
        } catch (_: Throwable) {
        }
        recordAuditEvent(context, "Security PIN Updated", "Updated 4-digit SHA-256 security PIN")
        return true
    }

    fun verifyPin(pin: String): Boolean {
        return hashPin(pin.trim()) == currentPinHash
    }

    fun unlockSession(context: Context?, pin: String): Boolean {
        if (verifyPin(pin)) {
            _isSessionUnlocked.value = true
            recordAuditEvent(context, "Vault Unlocked", "Authenticated via security PIN")
            return true
        }
        return false
    }

    fun lockSessionNow() {
        if (_isAppLockEnabled.value) {
            _isSessionUnlocked.value = false
        }
    }

    fun setAudioRetentionPolicy(context: Context?, policy: AudioRetentionPolicy) {
        _audioRetentionPolicy.value = policy
        try {
            context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_AUDIO_RETENTION, policy.name)
                ?.apply()
        } catch (_: Throwable) {
        }
        recordAuditEvent(
            context = context,
            action = "Audio Retention Policy Updated",
            details = "Set to ${policy.displayName}"
        )
        if (context != null && policy.maxAgeDays > 0) {
            enforceAudioRetentionPolicy(context)
        }
    }

    fun setRequireRecordingConsentPrompt(context: Context?, required: Boolean) {
        _requireRecordingConsentPrompt.value = required
        try {
            context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean(KEY_RECORDING_CONSENT_PROMPT, required)
                ?.apply()
        } catch (_: Throwable) {
        }
        recordAuditEvent(
            context = context,
            action = if (required) "Recording Consent Prompt Enabled" else "Recording Consent Prompt Disabled",
            details = "Explicit pre-recording consent confirmation updated"
        )
    }

    fun scanRawAudioFiles(directories: List<File>): List<File> {
        val found = mutableListOf<File>()
        for (dir in directories) {
            if (!dir.exists() || !dir.isDirectory) continue
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (f.isFile && (f.name.endsWith(".m4a", ignoreCase = true) ||
                        f.name.endsWith(".aac", ignoreCase = true) ||
                        f.name.startsWith("debrief_", ignoreCase = true) ||
                        f.name.startsWith("voice_", ignoreCase = true))
                ) {
                    found.add(f)
                }
            }
        }
        return found.distinctBy { it.absolutePath }
    }

    fun getAudioStorageStats(context: Context): AudioStorageStats {
        val files = scanRawAudioFiles(listOfNotNull(context.cacheDir, context.filesDir))
        val totalBytes = files.sumOf { it.length() }
        return AudioStorageStats(fileCount = files.size, totalBytes = totalBytes)
    }

    /**
     * Enforces the given AudioRetentionPolicy on the target directories.
     * Returns the number of raw audio files deleted.
     */
    fun enforceAudioRetentionPolicy(
        directories: List<File>,
        policy: AudioRetentionPolicy = _audioRetentionPolicy.value,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (policy == AudioRetentionPolicy.KEEP_INDEFINITELY) return 0
        val files = scanRawAudioFiles(directories)
        var deletedCount = 0
        val cutoffMillis = if (policy == AudioRetentionPolicy.DELETE_AFTER_TRANSCRIPTION) {
            nowMillis + 1_000L // delete all raw audio files immediately
        } else {
            nowMillis - (policy.maxAgeDays.toLong() * 24L * 3600L * 1000L)
        }
        for (file in files) {
            if (file.lastModified() <= cutoffMillis) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    fun enforceAudioRetentionPolicy(context: Context): Int {
        val deleted = enforceAudioRetentionPolicy(
            directories = listOfNotNull(context.cacheDir, context.filesDir),
            policy = _audioRetentionPolicy.value
        )
        if (deleted > 0) {
            recordAuditEvent(
                context = context,
                action = "Auto-Purged Raw Audio",
                details = "Deleted $deleted raw audio file(s) per ${_audioRetentionPolicy.value.displayName} policy"
            )
        }
        return deleted
    }

    fun purgeAllRawAudioFiles(directories: List<File>): Int {
        val files = scanRawAudioFiles(directories)
        var deletedCount = 0
        for (file in files) {
            if (file.delete()) {
                deletedCount++
            }
        }
        return deletedCount
    }

    fun purgeAllRawAudioFiles(context: Context): Int {
        val deleted = purgeAllRawAudioFiles(listOfNotNull(context.cacheDir, context.filesDir))
        recordAuditEvent(
            context = context,
            action = "Manual Audio Purge",
            details = "Deleted $deleted raw audio recording(s) while preserving text transcripts"
        )
        return deleted
    }

    fun recordAuditEvent(context: Context?, action: String, details: String) {
        val entry = PrivacyAuditEntry(
            action = action,
            details = details
        )
        val updated = (listOf(entry) + _auditLogs.value).take(25)
        _auditLogs.value = updated
        try {
            context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_AUDIT_LOGS, encodeAuditLogs(updated))
                ?.apply()
        } catch (_: Throwable) {
        }
    }

    private fun encodeAuditLogs(logs: List<PrivacyAuditEntry>): String {
        return logs.joinToString("\n") { e ->
            "${e.id}|${e.timestamp}|${e.action.replace("|", "/")}|${e.details.replace("|", "/")}"
        }
    }

    private fun decodeAuditLogs(raw: String?): List<PrivacyAuditEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 4) {
                PrivacyAuditEntry(
                    id = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis(),
                    action = parts[2],
                    details = parts[3]
                )
            } else null
        }
    }
}
