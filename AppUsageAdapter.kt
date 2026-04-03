package com.screentimetracker.ui.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screentimetracker.data.model.AppSummary
import com.screentimetracker.databinding.ItemAppUsageBinding
import com.screentimetracker.util.DateUtils

/**
 * RecyclerView adapter for displaying a list of [AppSummary] items.
 * Uses ListAdapter + DiffUtil for efficient updates.
 */
class AppUsageAdapter(
    private val onItemClick: ((AppSummary) -> Unit)? = null
) : ListAdapter<AppSummary, AppUsageAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemAppUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppSummary, rank: Int) {
            binding.tvRank.text = "#$rank"
            binding.tvAppName.text = item.appName
            binding.tvPackageName.text = item.packageName
            binding.tvUsageTime.text = DateUtils.formatDuration(item.totalUsageMs)
            binding.tvLastUsed.text = "Last: ${DateUtils.timestampToDisplay(item.lastUsedTimestamp)}"

            // Visual indicator for uninstalled apps
            if (item.isUninstalled) {
                binding.tvUninstalledBadge.visibility = android.view.View.VISIBLE
                binding.root.alpha = 0.7f
            } else {
                binding.tvUninstalledBadge.visibility = android.view.View.GONE
                binding.root.alpha = 1.0f
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppSummary>() {
        override fun areItemsTheSame(oldItem: AppSummary, newItem: AppSummary): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppSummary, newItem: AppSummary): Boolean =
            oldItem == newItem
    }
}
