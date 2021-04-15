package library.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import library.common.AppPlayer
import library.common.PictureInPictureConfig
import library.common.PlayerEvent
import library.common.PlayerEventStream
import library.common.PlayerState
import library.common.PlayerTelemetry
import library.common.PlayerViewWrapper
import library.common.ShareDelegate
import library.common.TrackInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFragmentTest {

    @Test
    fun appPlayerWasTornDownWhenFragmentIsDestroyed() {
        val appPlayer = FakeAppPlayer()

        val scenario = launchPlayerFragment(appPlayer)

        assertFalse(appPlayer.didRelease)
        scenario.moveToState(Lifecycle.State.DESTROYED)
        assertTrue(appPlayer.didRelease)
    }

    @Test
    fun appPlayerWasNotReleasedAcrossFragmentRecreation() {
        val appPlayer = FakeAppPlayer()
        val fakePlayerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())

        val scenario = launchPlayerFragment(
            appPlayer = appPlayer,
            playerViewWrapper = fakePlayerViewWrapper
        )
        scenario.recreate()

        assertTrue(fakePlayerViewWrapper.didDetach)
        assertFalse(appPlayer.didRelease)
    }

    private fun launchPlayerFragment(
        appPlayer: AppPlayer = FakeAppPlayer(),
        playerViewWrapper: PlayerViewWrapper? = null,
        playerEventStream: PlayerEventStream = FakePlayerEventStream(),
        url: String = "",
        telemetry: PlayerTelemetry? = null,
        shareDelegate: ShareDelegate? = null,
        pipConfig: PictureInPictureConfig = PictureInPictureConfig(false, false)
    ): FragmentScenario<PlayerFragment> {
        val vmFactory = PlayerViewModel.Factory(
            FakeAppPlayerFactory(appPlayer),
            playerEventStream,
            telemetry
        )

        val playerViewWrapperFactory = FakePlayerViewWrapperFactory(
            playerViewWrapper ?: FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        )

        return launchFragmentInContainer(fragmentArgs = PlayerFragment.args(url, pipConfig)) {
            PlayerFragment(vmFactory, playerViewWrapperFactory, shareDelegate)
        }
    }
}

class FakeAppPlayerFactory(val appPlayer: AppPlayer) : AppPlayer.Factory {
    override fun create(url: String) = appPlayer
}

class FakeAppPlayer : AppPlayer {
    var boundState: PlayerState? = null
    var didRelease: Boolean = false

    override val state: PlayerState get() = PlayerState.INITIAL
    override val textTracks: List<TrackInfo>
        get() = error("unused")
    override val audioTracks: List<TrackInfo>
        get() = error("unused")
    override val videoTracks: List<TrackInfo>
        get() = error("unused")

    override fun bind(playerViewWrapper: PlayerViewWrapper, playerState: PlayerState?) {
        boundState = playerState
    }

    override fun play() {
        error("unused")
    }

    override fun pause() {
        error("unused")
    }

    override fun release() {
        didRelease = true
    }

    override fun handleTrackInfoAction(action: TrackInfo.Action) {
        error("unused")
    }
}

class FakePlayerEventStream(val flow: Flow<PlayerEvent> = emptyFlow()) : PlayerEventStream {
    override fun listen(appPlayer: AppPlayer) = flow
}

class FakePlayerViewWrapperFactory(val playerViewWrapper: PlayerViewWrapper) : PlayerViewWrapper.Factory {
    override fun create(context: Context) = playerViewWrapper
}

class FakePlayerViewWrapper(context: Context) : PlayerViewWrapper {
    var didAttach: Boolean = false
    var didDetach: Boolean = false

    override val view: View = FrameLayout(context)

    override fun bindTextTracksPicker(textTracks: (View) -> Unit) = Unit
    override fun bindAudioTracksPicker(audioTracks: (View) -> Unit) = Unit
    override fun bindVideoTracksPicker(videoTracks: (View) -> Unit) = Unit
    override fun bindPlay(play: (View) -> Unit) = Unit
    override fun bindPause(pause: (View) -> Unit) = Unit
    override fun bindShare(onClick: (View) -> Unit) = Unit
    override fun onEvent(playerEvent: PlayerEvent) = Unit

    override fun attachTo(appPlayer: AppPlayer) {
        didAttach = true
    }

    override fun detach() {
        // Support reusing the same test View across Fragment recreation.
        (view.parent as? ViewGroup)?.removeView(view)
        didDetach = true
    }
}