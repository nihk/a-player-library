package nick.sample.ui.list

import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.RecyclerView
import nick.sample.databinding.PlayerItemBinding
import player.ui.common.PlayerArguments
import player.ui.inline.InlinePlaybackUi

class PlayerItemViewHolder(
    private val binding: PlayerItemBinding,
    private val playingPositions: MutableList<Int>
) : RecyclerView.ViewHolder(binding.root) {
    private var item: PlayerItem? = null

    init {
        binding.container.setOnClickListener {
            play()
        }
    }

    fun bind(item: PlayerItem) {
        stop()
        this.item = item
        binding.container.background = ColorDrawable(item.color)
        if (bindingAdapterPosition in playingPositions) {
            // Restore state
            playInternal()
        }
    }

    fun stop() {
        binding.libraryView.stop()
    }

    private fun play() {
        playingPositions += bindingAdapterPosition
        playInternal()
    }

    private fun playInternal() {
        val item = requireNotNull(item)
        binding.libraryView.play(item.toPlayerArguments())
    }

    private fun PlayerItem.toPlayerArguments(): PlayerArguments {
        return PlayerArguments(
            id = id,
            uri = uri,
            playbackUiFactory = InlinePlaybackUi.Factory::class.java
        )
    }
}
