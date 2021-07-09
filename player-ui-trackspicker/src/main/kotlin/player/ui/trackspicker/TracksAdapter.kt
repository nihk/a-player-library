package player.ui.trackspicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import player.ui.trackspicker.databinding.TrackItemBinding

internal class TracksAdapter(
    private val trackOptions: List<TrackOption>,
    private val callback: (TrackOption) -> Unit
) : RecyclerView.Adapter<TrackViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> TrackItemBinding.inflate(inflater, parent, false) }
            .let { binding -> TrackViewHolder(binding) }
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(trackOptions[position], callback)
    }

    override fun getItemCount(): Int {
        return trackOptions.size
    }
}
