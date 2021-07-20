package nick.sample.ui.list

import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import nick.sample.databinding.PlayerItemBinding
import nick.sample.ui.LibraryConfigurationFactory
import player.ui.common.PlayerArguments
import player.ui.inline.InlinePlaybackUi
import player.ui.inline.OnFullscreenChangedCallback

class PlayerItemViewHolder(
    private val binding: PlayerItemBinding,
    private val playingPositions: MutableList<Int>,
    private val onPlay: (Int /* position */) -> Unit,
    private val onFullscreen: (View) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private var item: PlayerItem? = null

    init {
        val configuration = LibraryConfigurationFactory()
            .create(
                activity = binding.root.context as ComponentActivity,
                onFullscreenChangedCallback = object : OnFullscreenChangedCallback {
                    override fun onFullscreenChanged(
                        isFullscreen: Boolean,
                        activity: FragmentActivity
                    ) {
                        if (isFullscreen) {
                            onFullscreen(binding.libraryView)
                        }
                    }
                }
            )
        binding.libraryView.initialize(configuration)
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
        onPlay(bindingAdapterPosition)
    }

    private fun PlayerItem.toPlayerArguments(): PlayerArguments {
        return PlayerArguments(
            id = id,
            uri = uri,
            playbackUiFactory = InlinePlaybackUi.Factory::class.java
        )
    }
}
