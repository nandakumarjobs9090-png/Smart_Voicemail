package com.smartvoicemail

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.smartvoicemail.util.StorageHelper

class SmartVoicemailApp : Application() {

    companion object {
        const val CHANNEL_VOICEMAIL = "voicemail_channel"
        const val CHANNEL_CALL = "call_channel"
    }

    override fun onCreate() {
        super.onCreate()
        StorageHelper.initDirectories(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val voicemailChannel = NotificationChannel(
            CHANNEL_VOICEMAIL,
            "Voicemail Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new voicemails"
        }

        val callChannel = NotificationChannel(
            CHANNEL_CALL,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call notifications"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(voicemailChannel)
        manager.createNotificationChannel(callChannel)
    }
}
