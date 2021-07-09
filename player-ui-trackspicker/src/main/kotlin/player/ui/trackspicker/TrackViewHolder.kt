package player.ui.trackspicker

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import player.ui.trackspicker.databinding.TrackItemBinding

internal class TrackViewHolder(private val binding: TrackItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(trackOption: TrackOption, callback: (TrackOption) -> Unit) {
        binding.name.text = when (trackOption) {
            is TrackOption.Auto -> trackOption.name
            is TrackOption.SingleTrack -> trackOption.trackInfo.name
        }
        binding.check.isVisible = when (trackOption) {
            is TrackOption.Auto -> trackOption.isSelected
            is TrackOption.SingleTrack -> trackOption.trackInfo.isManuallySet
        }
        binding.container.setOnClickListener {
            val selected = when (trackOption) {
                is TrackOption.Auto -> trackOption.copy(isSelected = true)
                is TrackOption.SingleTrack -> trackOption.copy(trackInfo = trackOption.trackInfo.copy(isSelected = true))
            }
            callback(selected)
        }
    }
}
