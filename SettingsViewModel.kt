package com.screentimetracker.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Manages user settings: daily screen time limit and export operations.
 * Settings are persisted in SharedPreferences.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "screen_time_prefs"
        const val KEY_DAILY_LIMIT_MS = "daily_limit_ms"
        const val KEY_LIMIT_ENABLED = "limit_enabled"

        /** Default daily limit: 4 hours */
        const val DEFAULT_LIMIT_MS = 4L * 60L * 60L * 1000L
    }

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _limitEnabled = MutableLiveData(prefs.getBoolean(KEY_LIMIT_ENABLED, false))
    val limitEnabled: LiveData<Boolean> = _limitEnabled

    private val _dailyLimitMs = MutableLiveData(prefs.getLong(KEY_DAILY_LIMIT_MS, DEFAULT_LIMIT_MS))
    val dailyLimitMs: LiveData<Long> = _dailyLimitMs

    fun setLimitEnabled(enabled: Boolean) {
        _limitEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LIMIT_ENABLED, enabled).apply()
    }

    fun setDailyLimitMs(limitMs: Long) {
        _dailyLimitMs.value = limitMs
        prefs.edit().putLong(KEY_DAILY_LIMIT_MS, limitMs).apply()
    }

    /** Returns current limit in hours (for display in the SeekBar) */
    fun getDailyLimitHours(): Int =
        ((_dailyLimitMs.value ?: DEFAULT_LIMIT_MS) / (60 * 60 * 1000)).toInt().coerceIn(1, 12)

    /** Called when user drags the hour slider */
    fun setDailyLimitHours(hours: Int) {
        setDailyLimitMs(hours.toLong() * 60L * 60L * 1000L)
    }

    /** Check if today's usage should trigger a warning notification */
    fun shouldWarn(todayUsageMs: Long): Boolean {
        if (_limitEnabled.value != true) return false
        val limit = _dailyLimitMs.value ?: DEFAULT_LIMIT_MS
        return todayUsageMs >= limit
    }
}
