package com.screentimetracker.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.screentimetracker.databinding.FragmentInstalledAppsBinding
import com.screentimetracker.util.ExportUtils
import com.screentimetracker.viewmodel.AppsViewModel
import kotlinx.coroutines.launch

/**
 * Screen showing all installed apps sorted by usage time.
 * Provides export (JSON / CSV) via the top-right menu.
 */
class InstalledAppsFragment : Fragment() {

    private var _binding: FragmentInstalledAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppsViewModel by viewModels()
    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstalledAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppUsageAdapter()
        binding.rvInstalledApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InstalledAppsFragment.adapter
        }

        viewModel.installedApps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
            binding.tvEmpty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Export buttons
        binding.btnExportJson.setOnClickListener { exportJson() }
        binding.btnExportCsv.setOnClickListener { exportCsv() }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadInstalledApps()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun exportJson() {
        lifecycleScope.launch {
            val records = viewModel.getAllRecordsForExport()
            ExportUtils.exportAsJson(requireContext(), records)
        }
    }

    private fun exportCsv() {
        lifecycleScope.launch {
            val records = viewModel.getAllRecordsForExport()
            ExportUtils.exportAsCsv(requireContext(), records)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
