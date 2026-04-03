package com.screentimetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.screentimetracker.data.model.AppSummary
import com.screentimetracker.data.model.DailyTotal
import com.screentimetracker.data.repository.UsageRepository
import com.screentimetracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * Manages UI state for the Dashboard screen.
 *
 * Exposes:
 * - Today's total screen time (live)
 * - Top apps for the selected period
 * - Daily totals for the bar chart
 * - Sync state (loading / error)
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)

    // ─── Period selector: Daily / Weekly / Monthly ──────────────────────────
    enum class Period { DAILY, WEEKLY, MONTHLY }

    private val _period = MutableLiveData(Period.DAILY)
    val period: LiveData<Period> = _period

    // ─── Today's total (always shown in the header) ─────────────────────────
    val todayTotal: LiveData<Long?> = repository.getTodayTotalFlow().asLiveData()

    // ─── Top apps (updated when period changes) ──────────────────────────────
    private val _topApps = MutableLiveData<List<AppSummary>>(emptyList())
    val topApps: LiveData<List<AppSummary>> = _topApps

    // ─── Daily totals for chart ──────────────────────────────────────────────
    private val _dailyTotals = MutableLiveData<List<DailyTotal>>(emptyList())
    val dailyTotals: LiveData<List<DailyTotal>> = _dailyTotals

    // ─── Sync state ──────────────────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // Load immediately
        syncAndRefresh()
    }

    /** Called when the user selects a different time period tab */
    fun setPeriod(period: Period) {
        _period.value = period
        refreshData()
    }

    /** Trigger a sync with the system UsageStatsManager */
    fun syncAndRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncTodayUsage()
                // Also backfill the current period
                val (start, end) = currentPeriodRange()
                repository.syncUsageRange(start, end)
            } catch (e: Exception) {
                _errorMessage.value = "Sync failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
            refreshData()
        }
    }

    /** Refresh displayed data from DB for the current period (no network / system call) */
    private fun refreshData() {
        val (start, end) = currentPeriodRange()

        viewModelScope.launch {
            repository.getTopInstalledApps(start, end).collect { apps ->
                _topApps.value = apps
            }
        }

        viewModelScope.launch {
            repository.getDailyTotals(start, end).collect { totals ->
                _dailyTotals.value = totals
            }
        }
    }

    private fun currentPeriodRange(): Pair<String, String> {
        return when (_period.value ?: Period.DAILY) {
            Period.DAILY -> Pair(DateUtils.today(), DateUtils.today())
            Period.WEEKLY -> DateUtils.weekRange()
            Period.MONTHLY -> DateUtils.monthRange()
        }
    }
}
