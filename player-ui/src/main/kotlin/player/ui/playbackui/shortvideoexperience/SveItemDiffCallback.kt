package player.ui.playbackui.shortvideoexperience

import androidx.recyclerview.widget.DiffUtil

class SveItemDiffCallback : DiffUtil.ItemCallback<SveItem>() {
    override fun areItemsTheSame(oldItem: SveItem, newItem: SveItem): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: SveItem, newItem: SveItem): Boolean {
        return oldItem == newItem
    }
}
