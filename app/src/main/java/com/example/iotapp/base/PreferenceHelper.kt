package com.example.iotapp.base

import android.content.Context

object PreferenceHelper {
    private const val PREF_FILE = "iot_prefs"
    private const val KEY_LANGUAGE = "pref_language"
    private const val KEY_TEMP_UNIT = "pref_temp_unit"

    fun saveLanguage(context: Context, code: String) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, code)
            .apply()
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun saveTempUnit(context: Context, unit: String) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEMP_UNIT, unit)
            .apply()
    }

    fun getTempUnit(context: Context): String {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_TEMP_UNIT, "C") ?: "C"
    }
}

