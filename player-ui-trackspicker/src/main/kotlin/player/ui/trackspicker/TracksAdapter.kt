package player.ui.trackspicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import player.ui.trackspicker.databinding.TrackItemBinding

internal class TracksAdapter(
    private val callback: (TrackOption.Action) -> Unit
) : ListAdapter<TrackOption, TrackViewHolder>(TrackOptionDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> TrackItemBinding.inflate(inflater, parent, false) }
            .let { binding -> TrackViewHolder(binding) }
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position), callback)
    }
}
