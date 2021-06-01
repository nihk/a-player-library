package player.ui.playbackui.shortvideoexperience

import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import player.common.PlayerArguments
import player.common.TimeFormatter
import player.ui.Navigator
import player.ui.databinding.SveItemBinding

class SveViewHolder(
    private val binding: SveItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        sveItem: SveItem,
        imageLoader: ImageLoader,
        navigator: Navigator,
        playerArguments: PlayerArguments,
        timeFormatter: TimeFormatter
    ) {
        binding.image.load(sveItem.imageUri, imageLoader)
        binding.duration.text = timeFormatter.playerTime(sveItem.duration)
        binding.root.setOnClickListener {
            navigator.toPlayer(playerArguments.copy(uri = sveItem.uri))
        }
    }
}
