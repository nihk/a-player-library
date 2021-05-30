package player.mediaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import player.common.AppPlayer
import player.common.PlayerEvent
import player.common.PlayerViewWrapper
import player.mediaplayer.databinding.LibraryMediaPlayerBinding

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

    override fun attach(appPlayer: AppPlayer) {
        val wrapper = appPlayer as? MediaPlayerWrapper
            ?: error("appPlayer $appPlayer was not a ${MediaPlayerWrapper::class.java}")
        val mediaPlayer = wrapper.mediaPlayer

        // We missed the one-time creation event; SurfaceView is ready, so attach.
        if (binding.surfaceView.holder.surface.isValid) {
            mediaPlayer.setDisplay(binding.surfaceView.holder)
        }
        callback = MediaPlayerSurfaceCallback(mediaPlayer)
        binding.surfaceView.holder.addCallback(callback)
    }

    override fun detachPlayer() {
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

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val i = 0
    }
}