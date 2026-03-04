package com.smartvoicemail.util

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {
    private const val PREFS_NAME = "smart_voicemail_prefs"
    private const val KEY_ANSWER_DELAY = "answer_delay"
    private const val KEY_MAX_RECORDING_TIME = "max_recording_time"
    private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
    private const val KEY_GREETING_SET = "greeting_set"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAnswerDelay(context: Context): Int =
        getPrefs(context).getInt(KEY_ANSWER_DELAY, 15)

    fun setAnswerDelay(context: Context, seconds: Int) =
        getPrefs(context).edit().putInt(KEY_ANSWER_DELAY, seconds).apply()

    fun getMaxRecordingTime(context: Context): Int =
        getPrefs(context).getInt(KEY_MAX_RECORDING_TIME, 60)

    fun setMaxRecordingTime(context: Context, seconds: Int) =
        getPrefs(context).edit().putInt(KEY_MAX_RECORDING_TIME, seconds).apply()

    fun getAutoDeleteDays(context: Context): Int =
        getPrefs(context).getInt(KEY_AUTO_DELETE_DAYS, 0)

    fun setAutoDeleteDays(context: Context, days: Int) =
        getPrefs(context).edit().putInt(KEY_AUTO_DELETE_DAYS, days).apply()

    fun isGreetingSet(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_GREETING_SET, false)

    fun setGreetingSet(context: Context, isSet: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_GREETING_SET, isSet).apply()
}
