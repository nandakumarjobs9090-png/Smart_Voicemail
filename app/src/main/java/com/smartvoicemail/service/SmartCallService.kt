package com.smartvoicemail.service

import android.content.Intent
import android.media.AudioAttributes
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

        // Wait for call to fully connect before playing greeting
        handler.postDelayed({
            playGreeting {
                playBeep {
                    startRecording()
                }
            }
        }, 1500)
    }

    private fun playGreeting(onComplete: () -> Unit) {
        val greetingFile = StorageHelper.getGreetingFile(this)
        if (!greetingFile.exists()) {
            Log.d(TAG, "No greeting file found, skipping to beep")
            onComplete()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(greetingFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    onComplete()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing greeting", e)
            mediaPlayer?.release()
            mediaPlayer = null
            onComplete()
        }
    }

    private fun playBeep(onComplete: () -> Unit) {
        BeepGenerator.playBeep(onComplete)
    }

    private fun startRecording() {
        if (currentCall == null || currentCall?.state == Call.STATE_DISCONNECTED) return

        val file = StorageHelper.createVoicemailFile(this)
        recordingFile = file

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)

                val maxTimeMs = PrefsManager.getMaxRecordingTime(this@SmartCallService) * 1000
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

            isRecording = true
            Log.d(TAG, "Recording started: ${file.absolutePath}")

            // Safety timeout
            val maxTime = PrefsManager.getMaxRecordingTime(this) * 1000L + 2000L
            maxRecordingRunnable = Runnable {
                stopVoicemailProcess()
                currentCall?.disconnect()
            }
            handler.postDelayed(maxRecordingRunnable!!, maxTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            isRecording = false
            currentCall?.disconnect()
        }
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
