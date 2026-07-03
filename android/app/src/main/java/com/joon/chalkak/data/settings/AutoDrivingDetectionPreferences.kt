package com.joon.chalkak.data.settings

import android.content.Context

class AutoDrivingDetectionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun isEnabled(): Boolean =
        preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "auto_driving_detection_preferences"
        const val KEY_ENABLED = "enabled"
    }
}
