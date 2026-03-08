package com.smartvoicemail.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import com.smartvoicemail.ui.incall.IncomingCallActivity
import com.smartvoicemail.util.BeepGenerator
import com.smartvoicemail.util.PrefsManager
import com.smartvoicemail.util.StorageHelper

class SmartCallService : InCallService() {

    companion object {
        private const val TAG = "SmartCallService"
        var currentCall: Call? = null
            private set
        var instance: SmartCallService? = null
            private set
        var onCallStateChanged: ((Int) -> Unit)? = null
        var onTimerTick: ((Long) -> Unit)? = null
        var isVoicemailActive = false
            private set
    }

    private var voicemailTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingFile: java.io.File? = null
    private var userAnswered = false
    private val handler = Handler(Looper.getMainLooper())
    private var maxRecordingRunnable: Runnable? = null
    private var previousAudioMode = AudioManager.MODE_NORMAL

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(TAG, "Call state changed: $state")
            onCallStateChanged?.invoke(state)
            when (state) {
                Call.STATE_DISCONNECTED -> {
                    stopVoicemailProcess()
                    voicemailTimer?.cancel()
                    currentCall = null
                }
                Call.STATE_ACTIVE -> {
                    if (userAnswered) {
                        voicemailTimer?.cancel()
                    }
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added")
        currentCall = call
        instance = this
        userAnswered = false
        isVoicemailActive = false
        call.registerCallback(callCallback)

        if (call.state == Call.STATE_RINGING) {
            showIncomingCallUI()
            startVoicemailTimer(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed")
        call.unregisterCallback(callCallback)
        stopVoicemailProcess()
        voicemailTimer?.cancel()
        currentCall = null
        instance = null
        isVoicemailActive = false
    }

    private fun showIncomingCallUI() {
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun startVoicemailTimer(call: Call) {
        val delayMs = PrefsManager.getAnswerDelay(this) * 1000L
        voicemailTimer = object : CountDownTimer(delayMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTimerTick?.invoke(millisUntilFinished / 1000)
            }
            override fun onFinish() {
                if (call.state == Call.STATE_RINGING && !userAnswered) {
                    autoAnswerAndRecord(call)
                }
            }
        }.start()
    }

    private fun autoAnswerAndRecord(call: Call) {
        Log.d(TAG, "Auto-answering call for voicemail")
        isVoicemailActive = true
        call.answer(VideoProfile.STATE_AUDIO_ONLY)

        // Wait for call to fully connect before setting up audio
        handler.postDelayed({
            // Set audio mode to IN_COMMUNICATION so that audio with
            // USAGE_VOICE_COMMUNICATION is routed into the call stream
            // (injected into the uplink) rather than played through the speaker
            setupCallAudioMode()

            // Small delay for audio routing to take effect
            handler.postDelayed({
                playGreeting {
                    playBeep {
                        startRecording()
                    }
                }
            }, 300)
        }, 1500)
    }

    /**
     * Configures AudioManager for voice communication mode.
     * This ensures that audio played with USAGE_VOICE_COMMUNICATION
     * is injected into the call's uplink (sent to the caller)
     * without playing through the device speaker.
     */
    private fun setupCallAudioMode() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousAudioMode = audioManager.mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            Log.d(TAG, "AudioManager set to MODE_IN_COMMUNICATION for audio injection")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio mode", e)
        }
    }

    /**
     * Restores the AudioManager mode to its previous state.
     */
    private fun restoreAudioMode() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = previousAudioMode
            Log.d(TAG, "AudioManager mode restored")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio mode", e)
        }
    }

    /**
     * Plays the greeting audio.
     * Uses USAGE_VOICE_COMMUNICATION + CONTENT_TYPE_SPEECH so the audio
     * is injected into the telephony uplink and heard by the CALLER only.
     * The device user does NOT hear the greeting through their speaker.
     */
    private fun playGreeting(onComplete: () -> Unit) {
        val greetingFile = StorageHelper.getGreetingFile(this)
        if (!greetingFile.exists()) {
            Log.d(TAG, "No greeting file found, skipping to beep")
            onComplete()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                // USAGE_VOICE_COMMUNICATION routes the audio into the call stream
                // The caller hears this; the device user does not
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(greetingFile.absolutePath)
                prepare()
                setVolume(1.0f, 1.0f)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    onComplete()
                }
                start()
            }
            Log.d(TAG, "Greeting playing → injected into call uplink for caller")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing greeting", e)
            mediaPlayer?.release()
            mediaPlayer = null
            onComplete()
        }
    }

    private fun playBeep(onComplete: () -> Unit) {
        // BeepGenerator also uses USAGE_VOICE_COMMUNICATION,
        // so the beep is injected into the call stream for the caller
        BeepGenerator.playBeep(onComplete)
    }

    /**
     * Records the CALLER's voice from the call audio stream.
     *
     * Tries audio sources in order of preference:
     * 1. VOICE_DOWNLINK  → captures only the remote party (caller's voice)
     * 2. VOICE_CALL      → captures both sides of the call
     * 3. VOICE_COMMUNICATION → mic with echo cancellation (fallback)
     *
     * VOICE_DOWNLINK is ideal because it records ONLY what the caller says,
     * without any audio from our side. Some devices restrict this to system
     * apps, so we fall back gracefully.
     */
    private fun startRecording() {
        if (currentCall == null || currentCall?.state == Call.STATE_DISCONNECTED) return

        val file = StorageHelper.createVoicemailFile(this)
        recordingFile = file

        // Audio sources in order: caller-only → both sides → mic fallback
        val audioSources = listOf(
            MediaRecorder.AudioSource.VOICE_DOWNLINK,      // Caller's voice only (ideal)
            MediaRecorder.AudioSource.VOICE_CALL,          // Both sides of the call
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // Mic with echo cancellation
            MediaRecorder.AudioSource.MIC                  // Raw mic (last resort)
        )

        for (source in audioSources) {
            try {
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                val maxTimeMs = PrefsManager.getMaxRecordingTime(this) * 1000

                recorder.apply {
                    setAudioSource(source)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000)
                    setOutputFile(file.absolutePath)
                    setMaxDuration(maxTimeMs)

                    setOnInfoListener { _, what, _ ->
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                            Log.d(TAG, "Max recording duration reached")
                            handler.post {
                                stopVoicemailProcess()
                                currentCall?.disconnect()
                            }
                        }
                    }

                    prepare()
                    start()
                }

                mediaRecorder = recorder
                isRecording = true

                val sourceName = when (source) {
                    MediaRecorder.AudioSource.VOICE_DOWNLINK -> "VOICE_DOWNLINK (caller only)"
                    MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL (both sides)"
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION (mic+AEC)"
                    MediaRecorder.AudioSource.MIC -> "MIC (raw)"
                    else -> "unknown"
                }
                Log.d(TAG, "Recording caller with source: $sourceName → ${file.absolutePath}")

                // Safety timeout to end recording and hang up
                val maxTime = PrefsManager.getMaxRecordingTime(this) * 1000L + 2000L
                maxRecordingRunnable = Runnable {
                    stopVoicemailProcess()
                    currentCall?.disconnect()
                }
                handler.postDelayed(maxRecordingRunnable!!, maxTime)

                return // Success — stop trying other sources

            } catch (e: Exception) {
                Log.w(TAG, "Audio source $source not available on this device, trying next", e)
                continue
            }
        }

        // All audio sources failed
        Log.e(TAG, "Cannot record voicemail — no audio source available")
        isRecording = false
        currentCall?.disconnect()
    }

    private fun stopVoicemailProcess() {
        Log.d(TAG, "Stopping voicemail process")
        maxRecordingRunnable?.let { handler.removeCallbacks(it) }

        mediaPlayer?.release()
        mediaPlayer = null

        if (isRecording) {
            try { mediaRecorder?.stop() } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder", e)
            }
            try { mediaRecorder?.release() } catch (e: Exception) {
                Log.e(TAG, "Error releasing recorder", e)
            }
            mediaRecorder = null
            isRecording = false

            recordingFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    StorageHelper.saveVoicemailEntry(this, file, currentCall)
                    Log.d(TAG, "Voicemail saved: ${file.absolutePath}")
                } else {
                    file.delete()
                }
            }
        }

        // Restore audio mode to what it was before voicemail
        restoreAudioMode()

        isVoicemailActive = false
    }

    fun answerCall() {
        userAnswered = true
        voicemailTimer?.cancel()
        if (isVoicemailActive) stopVoicemailProcess()
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun rejectCall() {
        voicemailTimer?.cancel()
        currentCall?.reject(false, null)
    }

    fun disconnectCall() {
        currentCall?.disconnect()
    }
}
