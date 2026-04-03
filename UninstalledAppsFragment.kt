package com.screentimetracker.ui.uninstalled

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.screentimetracker.databinding.FragmentUninstalledAppsBinding
import com.screentimetracker.ui.apps.AppUsageAdapter
import com.screentimetracker.viewmodel.AppsViewModel

/**
 * Displays apps that were previously tracked but have since been uninstalled.
 * All historical usage data is preserved in Room and displayed here.
 */
class UninstalledAppsFragment : Fragment() {

    private var _binding: FragmentUninstalledAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppsViewModel by viewModels()
    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUninstalledAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppUsageAdapter()
        binding.rvUninstalledApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UninstalledAppsFragment.adapter
        }

        viewModel.uninstalledApps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
            binding.tvEmpty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
