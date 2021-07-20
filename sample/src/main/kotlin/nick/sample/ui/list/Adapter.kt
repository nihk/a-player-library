package nick.sample.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import nick.sample.R
import nick.sample.databinding.PlayerItemBinding

class Adapter(
    val playingPositions: MutableList<Int>
) : ListAdapter<PlayerItem, PlayerItemViewHolder>(PlayerItemDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerItemViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.player_item, parent, false)
            .let { view -> PlayerItemBinding.bind(view) }
            .let { binding -> PlayerItemViewHolder(binding, playingPositions) }
    }

    override fun onBindViewHolder(holder: PlayerItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: PlayerItemViewHolder) {
        playingPositions -= holder.bindingAdapterPosition
        holder.stop()
    }
}
