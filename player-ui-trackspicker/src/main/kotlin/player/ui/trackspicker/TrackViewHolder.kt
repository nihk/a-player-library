package player.ui.trackspicker

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import player.ui.trackspicker.databinding.TrackItemBinding

internal class TrackViewHolder(private val binding: TrackItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(trackOption: TrackOption, callback: (TrackOption.Action) -> Unit) {
        binding.name.text = trackOption.name
        binding.check.isVisible = trackOption.isSelected
        binding.container.setOnClickListener {
            callback(trackOption.action)
        }
    }
}
