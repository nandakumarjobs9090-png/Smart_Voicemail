package com.smartvoicemail.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

object BeepGenerator {

    private const val SAMPLE_RATE = 44100
    private const val BEEP_DURATION_MS = 600
    private const val BEEP_FREQUENCY = 1000.0

    fun playBeep(onComplete: () -> Unit) {
        val numSamples = SAMPLE_RATE * BEEP_DURATION_MS / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / BEEP_FREQUENCY)
            samples[i] = (sin(angle) * Short.MAX_VALUE * 0.7).toInt().toShort()
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, numSamples * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.setNotificationMarkerPosition(numSamples)
        audioTrack.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.release()
                    Handler(Looper.getMainLooper()).post { onComplete() }
                }
                override fun onPeriodicNotification(track: AudioTrack?) {}
            }
        )
        audioTrack.play()
    }
}
