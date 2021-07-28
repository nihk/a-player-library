package player.ui.def

import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotSelected
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.test.CoroutinesTestRule
import player.test.FakePlayerViewWrapper
import player.test.AwaitAnimation
import player.ui.common.PlayerArguments
import player.ui.common.UiState
import player.ui.test.FakeCloseDelegate
import player.ui.test.FakePlayerController
import player.ui.test.FakeSeekBarListener
import player.ui.test.FakeShareDelegate
import player.ui.test.FakeTimeFormatter
import player.ui.test.TestActivity

class DefaultPlaybackUiTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun shareButtonIsVisibleWhenShareDelegateIsPresent() = defaultPlaybackUi(hasShareDelegate = true) {
        assertShareButton(isVisible = true)

        clickShareButton()
        assertShareDelegateCalled()
    }

    @Test
    fun shareButtonIsNotVisibleWhenShareDelegateIsNotPresent() = defaultPlaybackUi(hasShareDelegate = false) {
        assertShareButton(isVisible = false)
    }

    @Test
    fun titleIsSetWhenPlaybackInfoIsReceived() = defaultPlaybackUi {
        val mediaTitle = PlaybackInfo.MediaTitle(
            title = "this is a title",
            mediaUriRef = "" // Not relevant
        )
        setPlaybackInfo(mediaTitle)

        assertTitle("this is a title")
    }

    @Test
    fun pauseButtonIsVisibleWhenPlaying_afterEvent() = defaultPlaybackUi {
        val event = PlayerEvent.OnIsPlayingChanged(isPlaying = true)
        sendPlayerEvent(event)

        assertPlayPauseButton(isShowingPause = true)
    }

    @Test
    fun playButtonIsVisibleWhenPaused_afterEvent() = defaultPlaybackUi {
        val event = PlayerEvent.OnIsPlayingChanged(isPlaying = false)
        sendPlayerEvent(event)

        assertPlayPauseButton(isShowingPause = false)
    }

    @Test
    fun controllerFadesAfterDelay_whenPlaying() = defaultPlaybackUi(isPlaying = true) {
        assertControllerVisibility(isVisible = true)
        awaitDelayAndAnimation()
        assertControllerVisibility(isVisible = false)
    }

    @Test
    fun controllerDoesNotFadeAfterDelay_whenPaused() = defaultPlaybackUi(isPlaying = false) {
        assertControllerVisibility(isVisible = true)
        awaitDelayAndAnimation()
        assertControllerVisibility(isVisible = true)
    }

    fun defaultPlaybackUi(
        hasShareDelegate: Boolean = true,
        initialUiState: UiState = UiState(
            isControllerUsable = true,
            showLoading = false,
            isInPip = false
        ),
        isPlaying: Boolean = false,
        block: suspend DefaultPlaybackUiRobot.() -> Unit
    ) = coroutinesTestRule.testDispatcher.runBlockingTest {
        DefaultPlaybackUiRobot(hasShareDelegate, initialUiState, isPlaying).block()
    }

    inner class DefaultPlaybackUiRobot(
        hasShareDelegate: Boolean,
        initialUiState: UiState,
        isPlaying: Boolean
    ) {
        private val shareDelegate = if (hasShareDelegate) FakeShareDelegate() else null
        private val closeDelegate = FakeCloseDelegate()
        private val seekBarListener = FakeSeekBarListener()
        private val seekBarListenerFactory = FakeSeekBarListener.Factory(seekBarListener)
        private val timeFormatter = FakeTimeFormatter()
        private val navigator = FakeNavigator()
        private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        private val playerViewWrapperFactory = FakePlayerViewWrapper.Factory(playerViewWrapper)
        private val playerController = FakePlayerController(isPlaying)
        private val tracksPickerConfigFactory = FakeTracksPickerConfigFactory()
        private val playerArguments = PlayerArguments(
            id = "id",
            uri = "",
            playbackUiFactory = DefaultPlaybackUi.Factory::class.java
        )
        private lateinit var defaultPlaybackUi: DefaultPlaybackUi

        init {
            val scenario = launchActivity<TestActivity>()
            scenario.onActivity { testActivity ->
                defaultPlaybackUi = DefaultPlaybackUi(
                    activity = testActivity,
                    navigator = navigator,
                    seekBarListenerFactory = seekBarListenerFactory,
                    playerViewWrapperFactory = playerViewWrapperFactory,
                    playerController = playerController,
                    playerArguments = playerArguments,
                    closeDelegate = closeDelegate,
                    shareDelegate = shareDelegate,
                    timeFormatter = timeFormatter,
                    tracksPickerConfigFactory = tracksPickerConfigFactory
                )

                testActivity.attach(defaultPlaybackUi.view)

                setUiState(initialUiState)
            }
        }

        fun setUiState(uiState: UiState) {
            defaultPlaybackUi.onUiState(uiState)
        }

        fun assertShareButton(isVisible: Boolean) {
            onView(withId(R.id.share))
                .check(matches(isVisible.toVisibilityMatcher()))
        }

        fun clickShareButton() {
            onView(withId(R.id.share))
                .perform(click())
        }

        fun assertShareDelegateCalled() {
            assertTrue(shareDelegate?.didShare == true)
        }

        fun setPlaybackInfo(playbackInfo: PlaybackInfo) {
            defaultPlaybackUi.onPlaybackInfos(listOf(playbackInfo))
        }

        fun sendPlayerEvent(playerEvent: PlayerEvent) {
            defaultPlaybackUi.onPlayerEvent(playerEvent)
        }

        fun assertTitle(title: String) {
            onView(withId(R.id.title))
                .check(matches(withText(title)))
        }

        fun assertPlayPauseButton(isShowingPause: Boolean) {
            onView(withId(R.id.play_pause))
                .check(matches(isShowingPause.toSelectedMatcher()))
        }

        fun assertControllerVisibility(isVisible: Boolean) {
            onView(withId(R.id.player_controller))
                .check(matches(isVisible.toVisibilityMatcher()))
        }

        fun awaitDelayAndAnimation() {
            coroutinesTestRule.testDispatcher.advanceUntilIdle()
            onView(withId(R.id.player_controller))
                .perform(AwaitAnimation())
        }

        fun setIsPlaying(isPlaying: Boolean) {
            playerController.setIsPlaying(isPlaying)
        }

        private fun Boolean.toVisibilityMatcher(): Matcher<View> {
            return if (this) {
                isDisplayed()
            } else {
                not(isDisplayed())
            }
        }

        private fun Boolean.toSelectedMatcher(): Matcher<View> {
            return if (this) {
                isSelected()
            } else {
                isNotSelected()
            }
        }
    }
}
