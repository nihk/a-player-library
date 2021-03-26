package library.mediaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import library.common.AppPlayer
import library.common.PlayerEvent
import library.common.PlayerViewWrapper
import library.mediaplayer.databinding.LibraryMediaPlayerBinding

internal class MediaPlayerViewWrapper(context: Context) : PlayerViewWrapper {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(context)
        .inflate(R.layout.library_media_player, null)

    private val binding = LibraryMediaPlayerBinding.bind(view)

    private var callback: MediaPlayerSurfaceCallback? = null

    override fun onEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.OnVideoSizeChanged -> setSurfaceSize(
                width = playerEvent.width,
                height = playerEvent.height
            )
        }
    }

    private fun setSurfaceSize(width: Int, height: Int) {
        ConstraintSet().apply {
            clone(binding.container)
            setDimensionRatio(binding.surfaceView.id, "$width:$height")
            applyTo(binding.container)
        }
    }

    override fun bindTextTracksPicker(textTracks: (View) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun bindAudioTracksPicker(audioTracks: (View) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun bindVideoTracksPicker(videoTracks: (View) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun bindShare(onClick: (View) -> Unit) {
        binding.share.run {
            isVisible = true
            setOnClickListener(onClick)
        }
    }

    override fun bindPlay(play: (View) -> Unit) {
        binding.play.setOnClickListener(play)
    }

    override fun bindPause(pause: (View) -> Unit) {
        binding.pause.setOnClickListener(pause)
    }

    override fun attachTo(appPlayer: AppPlayer) {
        val mediaPlayer = appPlayer as? MediaPlayerWrapper
            ?: error("appPlayer $appPlayer was not a ${MediaPlayerWrapper::class.java}")
        callback = MediaPlayerSurfaceCallback(mediaPlayer.mediaPlayer)
        binding.surfaceView.holder.addCallback(callback)
    }

    override fun detach() {
        binding.surfaceView.holder.removeCallback(callback)
        callback = null
    }

    class Factory : PlayerViewWrapper.Factory {
        override fun create(context: Context): PlayerViewWrapper {
            return MediaPlayerViewWrapper(context)
        }
    }
}

private class MediaPlayerSurfaceCallback(
    private val mediaPlayer: MediaPlayer
) : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        mediaPlayer.setDisplay(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mediaPlayer.setDisplay(null)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
}