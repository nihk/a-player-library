package player.ui.test

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import player.common.AppPlayer
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.common.SeekData
import player.ui.common.PlaybackUi

class FakePlaybackUi : PlaybackUi {
    var attachCount: Int = 0
    var detachCount: Int = 0
    override val view: View get() = FrameLayout(ApplicationProvider.getApplicationContext())

    override fun onPlayerEvent(playerEvent: PlayerEvent) = Unit
    override fun onUiState(uiState: player.ui.common.UiState) = Unit
    override fun onSeekData(seekData: SeekData) = Unit
    override fun onTracksState(tracksState: player.ui.common.TracksState) = Unit
    override fun onPlaybackInfos(playbackInfos: List<PlaybackInfo>) = Unit
    override fun saveState(): Bundle = Bundle()
    override fun attach(appPlayer: AppPlayer) {
        ++attachCount
    }
    override fun detachPlayer() {
        ++detachCount
    }
}
