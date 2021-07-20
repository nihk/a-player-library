package nick.sample.ui.list

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import nick.sample.databinding.PlayerItemBinding
import nick.sample.ui.LibraryConfigurationFactory
import player.core.LibraryView
import player.ui.common.CloseDelegate
import player.ui.common.PlayerArguments
import player.ui.controller.detachFromParent
import player.ui.inline.InlinePlaybackUi
import player.ui.inline.OnFullscreenChangedCallback

class PlayerItemViewHolder(
    private val binding: PlayerItemBinding,
    private val playingPositions: MutableList<Int>,
    private val fullscreenPositions: MutableSet<Int>,
    private val onPlay: (Int /* position */) -> Unit,
    private val fullscreenContainer: ViewGroup
) : RecyclerView.ViewHolder(binding.root) {
    private val libraryView: LibraryView = binding.libraryView
    private var item: PlayerItem? = null

    init {
        binding.container.setOnClickListener {
            play()
        }
    }

    private fun handleFullscreen(isFullscreen: Boolean) {
        libraryView.detachFromParent()
        if (isFullscreen) {
            fullscreenContainer.addView(libraryView)
            fullscreenPositions += bindingAdapterPosition
        } else {
            binding.container.addView(libraryView)
            fullscreenPositions.clear()
        }
    }

    fun bind(item: PlayerItem) {
        stop()
        this.item = item
        binding.container.background = ColorDrawable(item.color)

        // State restoration
        if (bindingAdapterPosition in playingPositions) {
            playInternal()
        }
        if (bindingAdapterPosition in fullscreenPositions) {
            handleFullscreen(true)
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
        initialize()
        val item = requireNotNull(item)
        binding.libraryView.play(item.toPlayerArguments())
        onPlay(bindingAdapterPosition)
    }

    private fun initialize() {
        val configuration = LibraryConfigurationFactory()
            .create(
                activity = binding.root.context as ComponentActivity,
                onFullscreenChangedCallback = object : OnFullscreenChangedCallback {
                    override fun onFullscreenChanged(
                        isFullscreen: Boolean,
                        activity: FragmentActivity
                    ) {
                        handleFullscreen(isFullscreen)
                    }
                },
                closeDelegate = object : CloseDelegate {
                    override fun onClose(activity: FragmentActivity) {
                        activity.onBackPressed()
                    }
                },
                isFullscreenInitially = bindingAdapterPosition in fullscreenPositions
            )
        binding.libraryView.initialize(configuration)
    }

    private fun PlayerItem.toPlayerArguments(): PlayerArguments {
        return PlayerArguments(
            id = id,
            uri = uri,
            playbackUiFactory = InlinePlaybackUi.Factory::class.java
        )
    }
}
