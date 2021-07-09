package player.ui.trackspicker

import androidx.recyclerview.widget.DiffUtil

internal class TrackOptionDiffCallback : DiffUtil.ItemCallback<TrackOption>() {
    override fun areItemsTheSame(oldItem: TrackOption, newItem: TrackOption): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TrackOption, newItem: TrackOption): Boolean {
        return oldItem == newItem
    }
}
