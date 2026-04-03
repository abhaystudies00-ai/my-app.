package com.screentimetracker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.screentimetracker.MainActivity
import com.screentimetracker.R
import com.screentimetracker.databinding.FragmentDashboardBinding
import com.screentimetracker.ui.apps.AppUsageAdapter
import com.screentimetracker.util.DateUtils
import com.screentimetracker.util.PermissionUtils
import com.screentimetracker.viewmodel.DashboardViewModel

/**
 * Dashboard screen:
 * - Shows permission prompt if Usage Access is not granted
 * - Today's total screen time
 * - Period selector (Daily / Weekly / Monthly)
 * - Bar chart of daily totals
 * - Top apps list
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var appsAdapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check permission before doing anything else
        if (!(activity as? MainActivity)?.hasUsagePermission()!!) {
            showPermissionUI()
            return
        }

        showDashboard()
        setupObservers()
        setupChart()
        setupPeriodTabs()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission in case user just granted it
        val hasPermission = (activity as? MainActivity)?.hasUsagePermission() ?: false
        if (hasPermission) {
            binding.permissionLayout.visibility = View.GONE
            binding.dashboardContent.visibility = View.VISIBLE
            viewModel.syncAndRefresh()
        }
    }

    private fun showPermissionUI() {
        binding.permissionLayout.visibility = View.VISIBLE
        binding.dashboardContent.visibility = View.GONE

        binding.btnGrantPermission.setOnClickListener {
            PermissionUtils.openUsageAccessSettings(requireContext())
        }
    }

    private fun showDashboard() {
        binding.permissionLayout.visibility = View.GONE
        binding.dashboardContent.visibility = View.VISIBLE

        // Setup RecyclerView for top apps
        appsAdapter = AppUsageAdapter()
        binding.rvTopApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        // Today's total
        viewModel.todayTotal.observe(viewLifecycleOwner) { totalMs ->
            val ms = totalMs ?: 0L
            binding.tvTodayTotal.text = DateUtils.formatDuration(ms)
        }

        // Top apps
        viewModel.topApps.observe(viewLifecycleOwner) { apps ->
            appsAdapter.submitList(apps.take(10))
            binding.tvNoApps.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        }

        // Chart data
        viewModel.dailyTotals.observe(viewLifecycleOwner) { totals ->
            updateChart(totals.map { it.date to it.totalMs })
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Error
        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = msg
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }

    private fun setupChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
        }
    }

    private fun updateChart(data: List<Pair<String, Long>>) {
        if (data.isEmpty()) {
            binding.barChart.clear()
            return
        }

        val entries = data.mapIndexed { index, (_, ms) ->
            BarEntry(index.toFloat(), ms.toFloat())
        }

        val labels = data.map { (date, _) -> DateUtils.toDisplayShort(date) }

        val dataSet = BarDataSet(entries, "Screen Time").apply {
            color = requireContext().getColor(R.color.colorPrimary)
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    DateUtils.formatDuration(value.toLong())
            }
        }

        binding.barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx in labels.indices) labels[idx] else ""
            }
        }

        binding.barChart.data = BarData(dataSet)
        binding.barChart.invalidate()
    }

    private fun setupPeriodTabs() {
        binding.tabPeriod.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val period = when (tab?.position) {
                    0 -> DashboardViewModel.Period.DAILY
                    1 -> DashboardViewModel.Period.WEEKLY
                    else -> DashboardViewModel.Period.MONTHLY
                }
                viewModel.setPeriod(period)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
