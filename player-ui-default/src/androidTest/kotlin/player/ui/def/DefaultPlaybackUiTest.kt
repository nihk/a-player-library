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
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Assert.assertTrue
import org.junit.Test
import player.common.PlaybackInfo
import player.common.PlayerEvent
import player.test.FakePlayerViewWrapper
import player.ui.common.PlayerArguments
import player.ui.common.UiState
import player.ui.test.FakeCloseDelegate
import player.ui.test.FakePipController
import player.ui.test.FakePlayerController
import player.ui.test.FakeSeekBarListener
import player.ui.test.FakeShareDelegate
import player.ui.test.FakeTimeFormatter
import player.ui.test.TestActivity

class DefaultPlaybackUiTest {
    @Test
    fun shareButtonIsVisibleWhenShareDelegateIsPresent() {
        DefaultPlaybackUiRobot(hasShareDelegate = true).run {
            assertShareButton(isVisible = true)

            clickShareButton()
            assertShareDelegateCalled()
        }
    }

    @Test
    fun shareButtonIsNotVisibleWhenShareDelegateIsNotPresent() {
        DefaultPlaybackUiRobot(hasShareDelegate = false).run {
            assertShareButton(isVisible = false)
        }
    }

    @Test
    fun titleIsSetWhenPlaybackInfoIsReceived() {
        DefaultPlaybackUiRobot().run {
            val mediaTitle = PlaybackInfo.MediaTitle(
                title = "this is a title",
                mediaUriRef = "" // Not relevant
            )
            setPlaybackInfo(mediaTitle)

            assertTitle("this is a title")
        }
    }

    @Test
    fun pauseButtonIsVisibleWhenPlaying_afterEvent() {
        DefaultPlaybackUiRobot().run {
            val event = PlayerEvent.OnIsPlayingChanged(isPlaying = true)
            sendPlayerEvent(event)

            assertPlayPauseButton(isShowingPause = true)
        }
    }

    @Test
    fun playButtonIsVisibleWhenPaused_afterEvent() {
        DefaultPlaybackUiRobot().run {
            val event = PlayerEvent.OnIsPlayingChanged(isPlaying = false)
            sendPlayerEvent(event)

            assertPlayPauseButton(isShowingPause = false)
        }
    }

    class DefaultPlaybackUiRobot(
        hasShareDelegate: Boolean = true,
        initialUiState: UiState = UiState(
            isControllerUsable = true,
            showLoading = false
        )
    ) {
        private val shareDelegate = if (hasShareDelegate) FakeShareDelegate() else null
        private val closeDelegate = FakeCloseDelegate()
        private val seekBarListener = FakeSeekBarListener()
        private val seekBarListenerFactory = FakeSeekBarListener.Factory(seekBarListener)
        private val timeFormatter = FakeTimeFormatter()
        private val navigator = FakeNavigator()
        private val playerViewWrapper = FakePlayerViewWrapper(ApplicationProvider.getApplicationContext())
        private val playerViewWrapperFactory = FakePlayerViewWrapper.Factory(playerViewWrapper)
        private val pipController = FakePipController()
        private val playerController = FakePlayerController()
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
                    pipController = pipController,
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
