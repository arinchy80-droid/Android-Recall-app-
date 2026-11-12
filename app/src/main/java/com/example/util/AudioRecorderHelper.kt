package com.example.util

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null
    private val accumulatedSpeech = StringBuilder()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Boolean> = MutableStateFlow(false) // placeholder
    private val _normalizedAmplitude = MutableStateFlow(0.1f)
    val normalizedAmplitude: StateFlow<Float> = _normalizedAmplitude.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _lastRecordedFile = MutableStateFlow<File?>(null)
    val lastRecordedFile: StateFlow<File?> = _lastRecordedFile.asStateFlow()

    fun startRecording(coroutineScope: CoroutineScope) {
        if (_isRecording.value) return

        _durationSeconds.value = 0
        _liveTranscript.value = ""
        accumulatedSpeech.clear()
        _isRecording.value = true

        val audioDir = File(context.filesDir, "audio_memories").apply {
            if (!exists()) mkdirs()
        }
        outputFile = File(audioDir, "recall_audio_${System.currentTimeMillis()}.mp4")

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile?.absolutePath)
                    prepare()
                    start()
                }
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile?.absolutePath)
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            // MediaRecorder initialization fallback if permission/device constraints
        }

        // Initialize SpeechRecognizer for continuous live speech-to-text
        initSpeechRecognizer()

        // Amplitude and timer ticker loop - runs continuously until user taps stop
        recordingJob = coroutineScope.launch(Dispatchers.Default) {
            var secondsCounter = 0
            while (isActive && _isRecording.value) {
                delay(100)
                secondsCounter++
                if (secondsCounter % 10 == 0) {
                    _durationSeconds.value += 1
                }

                // Sample decibels from raw audio hardware
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 100
                    val normalized = (maxAmp / 32768f).coerceIn(0.05f, 1.0f)
                    _normalizedAmplitude.value = normalized
                } catch (e: Exception) {
                    _normalizedAmplitude.value = 0.2f
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {
                            if (mediaRecorder == null) {
                                val norm = ((rmsdB + 2) / 12f).coerceIn(0.1f, 1.0f)
                                _normalizedAmplitude.value = norm
                            }
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            // When user pauses, restart listening if still in active recording session
                            if (_isRecording.value) {
                                restartListeningSafely()
                            }
                        }

                        override fun onError(error: Int) {
                            // On silence timeout or temporary pause, restart listening if still recording
                            if (_isRecording.value) {
                                restartListeningSafely()
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val text = matches[0].trim()
                                if (text.isNotBlank()) {
                                    if (accumulatedSpeech.isNotEmpty() && !accumulatedSpeech.endsWith(" ")) {
                                        accumulatedSpeech.append(" ")
                                    }
                                    accumulatedSpeech.append(text)
                                    _liveTranscript.value = accumulatedSpeech.toString()
                                }
                            }
                            if (_isRecording.value) {
                                restartListeningSafely()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val partialText = matches[0].trim()
                                if (partialText.isNotBlank()) {
                                    val prefix = if (accumulatedSpeech.isNotEmpty()) "$accumulatedSpeech " else ""
                                    _liveTranscript.value = "$prefix$partialText"
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                startListeningIntent()
            } catch (e: Exception) {
                // Speech recognizer fallback
            }
        }
    }

    private fun startListeningIntent() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun restartListeningSafely() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                delay(150)
                if (_isRecording.value && speechRecognizer != null) {
                    startListeningIntent()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun stopRecording(): Pair<File?, String> {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignored on stop
        } finally {
            mediaRecorder = null
        }

        try {
            speechRecognizer?.apply {
                stopListening()
                destroy()
            }
        } catch (e: Exception) {
            // Ignored
        } finally {
            speechRecognizer = null
        }

        _normalizedAmplitude.value = 0.05f
        if (outputFile?.exists() == true && (outputFile?.length() ?: 0L) > 100L) {
            _lastRecordedFile.value = outputFile
        }
        val finalTranscript = if (accumulatedSpeech.isNotEmpty()) {
            accumulatedSpeech.toString().trim()
        } else {
            _liveTranscript.value.trim()
        }
        return Pair(outputFile, finalTranscript)
    }

    fun setManualTranscript(text: String) {
        accumulatedSpeech.clear()
        accumulatedSpeech.append(text)
        _liveTranscript.value = text
    }

    fun clearLastRecordedFile() {
        _lastRecordedFile.value = null
    }
}
