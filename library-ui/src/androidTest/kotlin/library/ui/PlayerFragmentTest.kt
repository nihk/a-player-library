package library.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import library.common.PictureInPictureConfig
import library.common.PlayerArguments
import library.common.PlayerViewWrapper
import library.common.ShareDelegate
import library.common.bundle
import library.test.NoOpPlayerViewWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFragmentTest {
    @Test
    fun appPlayerWasTornDownWhenFragmentIsDestroyed() = playerFragment {
        assertPlayerCreated(times = 1)
        assertPlayerNotReleased()
        destroy()
        assertPlayerReleased()
    }

    @Test
    fun appPlayerWasNotReleasedAcrossFragmentRecreation() = playerFragment {
        assertPlayerCreated(times = 1)
        recreate()
        assertPlayerCreated(times = 1)
        assertViewDetached()
        assertPlayerNotReleased()
    }
}

class FakePlayerViewWrapper(context: Context) : NoOpPlayerViewWrapper() {
    override val view: View = FrameLayout(context)

    override fun detach() {
        super.detach()
        // Support reusing the same test View across Fragment recreation.
        (view.parent as? ViewGroup)?.removeView(view)
    }
}

class FakePlayerViewWrapperFactory(val playerViewWrapper: PlayerViewWrapper) : PlayerViewWrapper.Factory {
    override fun create(context: Context) = playerViewWrapper
}

fun playerFragment(block: PlayerFragmentRobot.() -> Unit) {
    PlayerFragmentRobot().block()
}

class PlayerFragmentRobot {
    private val appPlayer = FakeAppPlayer()
    private val appPlayerFactory = FakeAppPlayerFactory(appPlayer)
    private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
    private val playerEventStream = FakePlayerEventStream()
    private val telemetry = FakePlayerTelemetry()
    private val shareDelegate: ShareDelegate? = null
    private val pipConfig = PictureInPictureConfig(false, false)
    private val scenario: FragmentScenario<PlayerFragment>

    init {
        val vmFactory = PlayerViewModel.Factory(
            appPlayerFactory,
            playerEventStream,
            telemetry
        )

        val playerViewWrapperFactory = FakePlayerViewWrapperFactory(
            playerViewWrapper ?: FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        )

        val args = PlayerArguments(
            uri = "",
            pipConfig = pipConfig
        )
        scenario = launchFragmentInContainer(fragmentArgs = args.bundle()) {
            PlayerFragment(vmFactory, playerViewWrapperFactory, shareDelegate)
        }
    }

    fun destroy() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }

    fun recreate() {
        scenario.recreate()
    }

    fun assertPlayerCreated(times: Int) {
        assertEquals(times, appPlayerFactory.createCount)
    }

    fun assertViewDetached() {
        assertTrue(playerViewWrapper.didDetach)
    }

    fun assertPlayerReleased() {
        assertTrue(appPlayer.didRelease)
    }

    fun assertPlayerNotReleased() {
        assertFalse(appPlayer.didRelease)
    }
}