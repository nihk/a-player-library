package player.ui.sve

import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import player.common.TimeFormatter
import player.common.ui.Navigator
import player.common.ui.PlayerArguments
import player.ui.sve.databinding.SveItemBinding

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
            val args = playerArguments.copy(
                uri = sveItem.uri,
                playbackUiFactory = sveItem.playbackUiFactory
            )
            navigator.toPlayer(args)
        }
    }
}
