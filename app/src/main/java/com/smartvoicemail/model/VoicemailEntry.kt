package com.smartvoicemail.model

import java.io.Serializable

data class VoicemailEntry(
    val id: String,
    val callerNumber: String,
    val callerName: String?,
    val timestamp: Long,
    val duration: Int,
    val filePath: String,
    val isRead: Boolean = false
) : Serializable
