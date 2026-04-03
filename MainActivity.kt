package com.screentimetracker

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.screentimetracker.databinding.ActivityMainBinding
import com.screentimetracker.ui.dashboard.DashboardFragment
import com.screentimetracker.ui.apps.InstalledAppsFragment
import com.screentimetracker.ui.uninstalled.UninstalledAppsFragment
import com.screentimetracker.ui.settings.SettingsFragment
import com.screentimetracker.util.PermissionUtils
import com.screentimetracker.worker.UsageSyncWorker

/**
 * Main activity. Hosts bottom navigation between:
 *  - Dashboard (today summary + charts)
 *  - Installed Apps
 *  - Uninstalled Apps
 *  - Settings
 *
 * Permission check: if Usage Access is not granted, DashboardFragment
 * will show the permission prompt instead of the dashboard content.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Schedule periodic background sync
        UsageSyncWorker.schedulePeriodicSync(this)

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission each time app resumes (user might have just granted it)
        val hasPermission = PermissionUtils.hasUsageAccessPermission(this)
        // Fragments observe this via the Activity's hasUsagePermission() helper
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_installed -> loadFragment(InstalledAppsFragment())
                R.id.nav_uninstalled -> loadFragment(UninstalledAppsFragment())
                R.id.nav_settings -> loadFragment(SettingsFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /** Convenience for fragments to check permission state */
    fun hasUsagePermission(): Boolean =
        PermissionUtils.hasUsageAccessPermission(this)
}
