package com.screentimetracker.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.screentimetracker.databinding.FragmentSettingsBinding
import com.screentimetracker.util.PermissionUtils
import com.screentimetracker.viewmodel.SettingsViewModel

/**
 * Settings screen:
 * - Permission status with a re-grant button
 * - Daily screen time limit toggle + slider
 * - Dark mode toggle
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updatePermissionStatus()

        // Permission button
        binding.btnGrantPermission.setOnClickListener {
            PermissionUtils.openUsageAccessSettings(requireContext())
        }

        // Daily limit toggle
        viewModel.limitEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchLimit.isChecked = enabled
            binding.limitControls.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        binding.switchLimit.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLimitEnabled(isChecked)
        }

        // Limit slider (1–12 hours)
        viewModel.dailyLimitMs.observe(viewLifecycleOwner) { ms ->
            val hours = (ms / (60 * 60 * 1000)).toInt().coerceIn(1, 12)
            binding.seekBarLimit.progress = hours - 1
            binding.tvLimitValue.text = "${hours}h daily limit"
        }

        binding.seekBarLimit.max = 11 // 0..11 → 1h..12h
        binding.seekBarLimit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val hours = progress + 1
                binding.tvLimitValue.text = "${hours}h daily limit"
                if (fromUser) viewModel.setDailyLimitHours(hours)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Dark mode toggle
        val isDark = AppCompatDelegate.getDefaultNightMode() ==
                AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.isChecked = isDark

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val hasPermission = PermissionUtils.hasUsageAccessPermission(requireContext())
        binding.tvPermissionStatus.text = if (hasPermission) {
            "Usage Access: Granted"
        } else {
            "Usage Access: Not Granted"
        }
        binding.btnGrantPermission.visibility = if (hasPermission) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
