package nick.sample.ui.list

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import nick.sample.databinding.PlayerItemBinding
import nick.sample.ui.SampleLibraryConfigurationFactory
import player.common.DefaultPlaybackInfoResolver
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
    private val fullscreenContainer: ViewGroup,
) : RecyclerView.ViewHolder(binding.root) {
    private val libraryView: LibraryView = binding.libraryView
    private var item: PlayerItem? = null

    init {
        binding.container.setOnClickListener {
            play()
        }
    }

    private fun handleFullscreen(isFullscreen: Boolean) {
        val x = fullscreenContainer.x
        val y = fullscreenContainer.y
        val height = fullscreenContainer.height
        val width = fullscreenContainer.width
        val widthDelta = width - binding.container.width
        val heightDelta = height - binding.container.height

        val animation: ViewPropertyAnimator = if (isFullscreen) {
            fullscreenContainer.updateLayoutParams {
                this.width = binding.container.width
                this.height = binding.container.height
            }
            fullscreenContainer.x = binding.container.x
            fullscreenContainer.y = binding.container.y

            fullscreenContainer.animate()
                .withStartAction {
                    handleFullscreenReparenting(isFullscreen)
                }
                .setUpdateListener { valueAnimator ->
                    val progress = valueAnimator.animatedValue as Float
                    fullscreenContainer.updateLayoutParams {
                        this.width = binding.container.width + (widthDelta * progress).toInt()
                        this.height = binding.container.height + (heightDelta * progress).toInt()
                    }
                }
                .x(x)
                .y(y)
        } else {
            fullscreenContainer.animate()
                .setUpdateListener { valueAnimator ->
                    val progress = valueAnimator.animatedValue as Float
                    fullscreenContainer.updateLayoutParams {
                        this.width = width - (widthDelta * progress).toInt()
                        this.height = height - (heightDelta * progress).toInt()
                    }
                }
                .x(binding.container.x)
                .y(binding.container.y)
                .withEndAction {
                    handleFullscreenReparenting(isFullscreen)
                    fullscreenContainer.updateLayoutParams {
                        this.width = ViewGroup.LayoutParams.MATCH_PARENT
                        this.height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    fullscreenContainer.x = x
                    fullscreenContainer.y = y
                }
        }

        animation
            .setDuration(250L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun handleFullscreenReparenting(isFullscreen: Boolean) {
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
            // Avoid animating
            handleFullscreenReparenting(true)
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
        val configuration = SampleLibraryConfigurationFactory()
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
                isFullscreenInitially = bindingAdapterPosition in fullscreenPositions,
                playbackInfoResolver = DefaultPlaybackInfoResolver()
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
