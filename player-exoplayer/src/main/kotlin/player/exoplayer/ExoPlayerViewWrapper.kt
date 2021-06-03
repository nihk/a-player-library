package player.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.exoplayer2.ui.PlayerView
import player.common.AppPlayer
import player.common.ui.PlayerViewWrapper

internal class ExoPlayerViewWrapper(context: Context) : PlayerViewWrapper {
    @SuppressLint("InflateParams")
    override val view: View = LayoutInflater.from(context)
        .inflate(R.layout.library_exo_player, null)

    private val playerView: PlayerView get() = view.findViewById(R.id.player_view)

    override fun attach(appPlayer: AppPlayer) {
        appPlayer as? ExoPlayerWrapper
            ?: error("appPlayer $appPlayer was not a ${ExoPlayerWrapper::class.java}")
        playerView.player = appPlayer.player
    }

    override fun detachPlayer() {
        // Player holds a strong reference to PlayerView via PlayerView.ComponentListener, so nulling
        // this out is necessary when the Player lives longer than the PlayerView.
        playerView.player = null
    }

    class Factory : PlayerViewWrapper.Factory {
        override fun create(context: Context): PlayerViewWrapper {
            return ExoPlayerViewWrapper(context)
        }
    }
}