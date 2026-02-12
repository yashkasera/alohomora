package io.github.yashkasera.alohomora.androidApp.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferencesDataSource(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun readAutoRefresh(): Boolean = prefs.getBoolean(KEY_AUTO_REFRESH, false)

    fun readLastRefreshEpochMillis(): Long =
        prefs.getLong(KEY_LAST_REFRESH_EPOCH_MILLIS, 0L)

    fun writeUsername(value: String) {
        prefs.edit().putString(KEY_USERNAME, value).apply()
    }

    fun writeAutoRefresh(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, value).apply()
    }

    fun writeLastRefreshEpochMillis(value: Long) {
        prefs.edit().putLong(KEY_LAST_REFRESH_EPOCH_MILLIS, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "android_sample_prefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_AUTO_REFRESH = "auto_refresh"
        private const val KEY_LAST_REFRESH_EPOCH_MILLIS = "last_refresh_epoch_millis"
    }
}
