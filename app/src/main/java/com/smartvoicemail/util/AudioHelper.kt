package com.smartvoicemail.util

import android.media.MediaMetadataRetriever

object AudioHelper {

    fun getAudioDuration(filePath: String): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            (durationStr?.toLongOrNull() ?: 0L).toInt() / 1000
        } catch (e: Exception) {
            0
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs} sec"
    }
}
