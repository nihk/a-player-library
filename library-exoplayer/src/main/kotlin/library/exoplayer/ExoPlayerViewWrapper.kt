package library.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ui.PlayerView
import library.common.AppPlayer
import library.common.PlayerViewWrapper
import library.common.TrackInfo
import library.exoplayer.databinding.LibraryExoPlayerControllerBinding

internal class ExoPlayerViewWrapper(context: Context) : PlayerViewWrapper {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(context)
        .inflate(R.layout.library_exo_player, null)

    private val playerView: PlayerView get() = view.findViewById(R.id.player_view)

    private val controllerBinding = view.findViewById<View>(R.id.controller)
        .let(LibraryExoPlayerControllerBinding::bind)

    private val loading: ProgressBar get() = view.findViewById(R.id.loading)

    override fun bindTracks(type: TrackInfo.Type, onClick: (View) -> Unit) {
        when (type) {
            TrackInfo.Type.VIDEO -> controllerBinding.video.run {
                isVisible = true
                setOnClickListener(onClick)
            }
            TrackInfo.Type.AUDIO -> controllerBinding.audio.run {
                isVisible = true
                setOnClickListener(onClick)
            }
            TrackInfo.Type.TEXT -> controllerBinding.captions.run {
                isVisible = true
                setOnClickListener(onClick)
            }
        }
    }

    override fun bindPlay(play: (View) -> Unit) {
        // ExoPlayer already handles this via shared PlayerViewController IDs
    }

    override fun bindPause(pause: (View) -> Unit) {
        // ExoPlayer already handles this via shared PlayerViewController IDs
    }

    override fun bindShare(onClick: (View) -> Unit) {
        controllerBinding.share.run {
            isVisible = true
            setOnClickListener(onClick)
        }
    }

    override fun attachTo(appPlayer: AppPlayer) {
        appPlayer as? ExoPlayerWrapper
            ?: error("appPlayer $appPlayer was not a ${ExoPlayerWrapper::class.java}")
        playerView.player = appPlayer.player
    }

    override fun detach() {
        // Player is being detached, so don't keep these click listeners wired up.
        listOf(
            controllerBinding.captions,
            controllerBinding.video,
            controllerBinding.audio
        ).forEach { view ->
            view.isVisible = false
            view.setOnClickListener(null)
        }

        // Player holds a strong reference to PlayerView via PlayerView.ComponentListener, so nulling
        // this out is necessary when the Player lives longer than the PlayerView.
        playerView.player = null
    }

    override fun setControllerUsability(isUsable: Boolean) {
        playerView.useController = isUsable
    }

    override fun setLoading(isLoading: Boolean) {
        loading.isVisible = isLoading
    }

    class Factory : PlayerViewWrapper.Factory {
        override fun create(context: Context): PlayerViewWrapper {
            return ExoPlayerViewWrapper(context)
        }
    }
}