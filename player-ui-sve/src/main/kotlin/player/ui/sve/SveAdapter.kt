package player.ui.sve

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import player.common.TimeFormatter
import player.common.ui.Navigator
import player.common.ui.PlayerArguments
import player.ui.sve.databinding.SveItemBinding

class SveAdapter(
    private val imageLoader: ImageLoader,
    private val navigator: Navigator,
    private val playerArguments: PlayerArguments,
    private val timeFormatter: TimeFormatter,
    diffCallback: SveItemDiffCallback = SveItemDiffCallback()
) : ListAdapter<SveItem, SveViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SveViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> SveItemBinding.inflate(inflater, parent, false) }
            .let { binding -> SveViewHolder(binding) }
    }

    override fun onBindViewHolder(holder: SveViewHolder, position: Int) {
        holder.bind(getItem(position), imageLoader, navigator, playerArguments, timeFormatter)
    }
}
