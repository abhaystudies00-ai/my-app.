package com.screentimetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.screentimetracker.data.model.AppSummary
import com.screentimetracker.data.repository.UsageRepository
import com.screentimetracker.util.DateUtils
import kotlinx.coroutines.launch

/**
 * Manages data for the "Installed Apps" and "Uninstalled Apps" screens.
 */
class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)

    // ─── Installed apps (weekly by default) ─────────────────────────────────
    private val _installedApps = MutableLiveData<List<AppSummary>>(emptyList())
    val installedApps: LiveData<List<AppSummary>> = _installedApps

    // ─── Uninstalled apps ────────────────────────────────────────────────────
    val uninstalledApps: LiveData<List<AppSummary>> =
        repository.getUninstalledApps().asLiveData()

    // ─── Export / loading state ───────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _exportStatus = MutableLiveData<String?>(null)
    val exportStatus: LiveData<String?> = _exportStatus

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps(
        startDate: String = DateUtils.weekRange().first,
        endDate: String = DateUtils.today()
    ) {
        viewModelScope.launch {
            repository.getTopInstalledApps(startDate, endDate).collect {
                _installedApps.value = it
            }
        }
    }

    /** Fetch all records for JSON/CSV export */
    suspend fun getAllRecordsForExport() = repository.getAllRecordsForExport()
}
