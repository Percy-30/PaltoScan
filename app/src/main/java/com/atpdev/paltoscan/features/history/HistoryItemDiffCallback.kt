package com.atpdev.paltoscan.features.history

import androidx.recyclerview.widget.DiffUtil
import com.atpdev.paltoscan.features.history.adapter.HistoryItem

class HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
    /*override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return oldItem.diseaseName == newItem.diseaseName && oldItem.section == newItem.section
    }*/

    override fun areItemsTheSame(
        oldItem: HistoryItem,
        newItem: HistoryItem,
    ): Boolean {
        // Comparar por ID en lugar de nombre y sección
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: HistoryItem,
        newItem: HistoryItem,
    ): Boolean {
        return oldItem == newItem
    }
}
