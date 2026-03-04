package com.smartvoicemail.util

import android.content.Context
import android.telecom.Call
import com.smartvoicemail.model.VoicemailEntry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object StorageHelper {

    private const val VOICEMAILS_DIR = "voicemails"
    private const val GREETING_DIR = "greeting"
    private const val VOICEMAILS_DB = "voicemails.json"

    fun initDirectories(context: Context) {
        getVoicemailsDir(context).mkdirs()
        getGreetingDir(context).mkdirs()
    }

    fun getVoicemailsDir(context: Context): File =
        File(context.filesDir, VOICEMAILS_DIR)

    fun getGreetingDir(context: Context): File =
        File(context.filesDir, GREETING_DIR)

    fun getGreetingFile(context: Context): File =
        File(getGreetingDir(context), "greeting.mp3")

    fun createVoicemailFile(context: Context): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val fileName = "vm_${dateFormat.format(Date())}.mp3"
        return File(getVoicemailsDir(context), fileName)
    }

    fun saveVoicemailEntry(context: Context, file: File, call: Call?) {
        val callerNumber = call?.details?.handle?.schemeSpecificPart ?: "Unknown"
        val callerName = ContactHelper.getContactName(context, callerNumber)
        val duration = AudioHelper.getAudioDuration(file.absolutePath)

        val entry = VoicemailEntry(
            id = UUID.randomUUID().toString(),
            callerNumber = callerNumber,
            callerName = callerName,
            timestamp = System.currentTimeMillis(),
            duration = duration,
            filePath = file.absolutePath
        )

        val entries = loadVoicemailEntries(context).toMutableList()
        entries.add(0, entry)
        saveVoicemailEntries(context, entries)
    }

    fun loadVoicemailEntries(context: Context): List<VoicemailEntry> {
        val dbFile = File(context.filesDir, VOICEMAILS_DB)
        if (!dbFile.exists()) return emptyList()

        return try {
            val json = dbFile.readText()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                VoicemailEntry(
                    id = obj.getString("id"),
                    callerNumber = obj.getString("callerNumber"),
                    callerName = obj.optString("callerName", null),
                    timestamp = obj.getLong("timestamp"),
                    duration = obj.getInt("duration"),
                    filePath = obj.getString("filePath"),
                    isRead = obj.optBoolean("isRead", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveVoicemailEntries(context: Context, entries: List<VoicemailEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("callerNumber", entry.callerNumber)
                put("callerName", entry.callerName ?: JSONObject.NULL)
                put("timestamp", entry.timestamp)
                put("duration", entry.duration)
                put("filePath", entry.filePath)
                put("isRead", entry.isRead)
            })
        }
        File(context.filesDir, VOICEMAILS_DB).writeText(array.toString(2))
    }

    fun deleteVoicemail(context: Context, entry: VoicemailEntry) {
        File(entry.filePath).delete()
        val entries = loadVoicemailEntries(context).toMutableList()
        entries.removeAll { it.id == entry.id }
        saveVoicemailEntries(context, entries)
    }

    fun markAsRead(context: Context, entry: VoicemailEntry) {
        val entries = loadVoicemailEntries(context).toMutableList()
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index != -1) {
            entries[index] = entries[index].copy(isRead = true)
            saveVoicemailEntries(context, entries)
        }
    }
}
