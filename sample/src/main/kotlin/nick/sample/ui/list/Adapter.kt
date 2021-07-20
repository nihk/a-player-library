package nick.sample.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nick.sample.R
import nick.sample.databinding.PlayerItemBinding

class Adapter(
    val playingPositions: MutableList<Int>,
    private val fullscreenContainer: ViewGroup
) : ListAdapter<PlayerItem, PlayerItemViewHolder>(PlayerItemDiffCallback) {
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerItemViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.player_item, parent, false)
            .let { view -> PlayerItemBinding.bind(view) }
            .let { binding -> PlayerItemViewHolder(binding, playingPositions, ::onPlay, ::onFullscreen) }
    }

    override fun onBindViewHolder(holder: PlayerItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onViewDetachedFromWindow(holder: PlayerItemViewHolder) {
        holder.stop()
        playingPositions -= holder.bindingAdapterPosition
    }

    private fun onPlay(positionToPlay: Int) {
        val previousPosition = playingPositions.firstOrNull { position -> position != positionToPlay }
            ?: return
        val holder = recyclerView?.findViewHolderForAdapterPosition(previousPosition)
            as? PlayerItemViewHolder
        // Only allow 1 video playing in the list at a time.
        holder?.stop()
        playingPositions -= previousPosition
    }

    private fun onFullscreen(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        fullscreenContainer.addView(view)
    }
}
