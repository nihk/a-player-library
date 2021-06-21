package player.ui.sve

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import player.common.PlayerViewWrapper
import player.ui.sve.databinding.SveItemBinding

// fixme: PlayerView is briefly showing stale Player content when recycled, notably when coming back from PiP
//  need to hide/show PlayerView with the first frame of the video showing in the interim
internal class SveAdapter(
    private val playerViewWrapper: PlayerViewWrapper,
    private val imageLoader: ImageLoader
) : ListAdapter<SveItem, SveViewHolder>(SveItemDiffCallback()) {
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SveViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> SveItemBinding.inflate(inflater, parent, false) }
            .let { binding -> SveViewHolder(binding, imageLoader) }
    }

    override fun onBindViewHolder(holder: SveViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        // Change animations for PlaybackInfo updates are jarring.
        recyclerView.itemAnimator = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    fun onPageSettled(position: Int) {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
            as? SveViewHolder
        viewHolder?.attach(playerViewWrapper)
    }
}
