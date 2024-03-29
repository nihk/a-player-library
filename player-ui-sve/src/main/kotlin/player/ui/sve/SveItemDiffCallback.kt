package player.ui.sve

import androidx.recyclerview.widget.DiffUtil

internal class SveItemDiffCallback : DiffUtil.ItemCallback<SveItem>() {
    override fun areItemsTheSame(oldItem: SveItem, newItem: SveItem): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: SveItem, newItem: SveItem): Boolean {
        return oldItem == newItem
    }
}
