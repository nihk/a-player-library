package player.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import player.test.AwaitAnimation
import player.test.CoroutinesTestRule

class FadingFrameLayoutTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun fadableViewIsGoneAfterDelay() = fadingRobot {
        assertFadableVisibility(isVisible = true)
        awaitDelayAndAnimation()
        assertFadableVisibility(isVisible = false)
    }

    @Test
    fun tappingFadedMakesFadableReappear() = fadingRobot {
        awaitDelayAndAnimation()
        tapOnContainer()
        assertFadableVisibility(isVisible = true)
    }

    @Test
    fun tappingVisibleMakesFadableDisappear() = fadingRobot {
        tapOnContainer()
        assertFadableVisibility(isVisible = false)
    }

    @Test
    fun tappingVisibleTwiceMakesFadableVisible() = fadingRobot {
        tapOnContainer()
        tapOnContainer()
        assertFadableVisibility(isVisible = true)
    }

    @Test
    fun tappingOnDebouncableKeepsViewVisible() = fadingRobot {
        tapOnDebouncable()
        assertFadableVisibility(isVisible = true)
    }

    @Test
    fun tappingOnDebouncableTwiceKeepsViewVisible() = fadingRobot {
        tapOnDebouncable()
        tapOnDebouncable()
        assertFadableVisibility(isVisible = true)
    }

    @Test
    fun tappingOnNonDebouncableFades() = fadingRobot {
        tapOnNonDebouncable()
        assertFadableVisibility(isVisible = false)
    }

    @Test
    fun pausingFadingPreventsFading() = fadingRobot {
        pauseFading()
        awaitDelayAndAnimation()
        assertFadableVisibility(isVisible = true)
    }

    @Test
    fun resumingFadingResultsInAFade() = fadingRobot {
        resumeFading()
        awaitDelayAndAnimation()
        assertFadableVisibility(isVisible = false)
    }

    @Test
    fun pausingThenResumingFadingResultsInAFade() = fadingRobot {
        pauseFading()
        awaitDelayAndAnimation()
        resumeFading()
        awaitDelayAndAnimation()
        assertFadableVisibility(isVisible = false)
    }

    private fun fadingRobot(block: suspend FadingRobot.() -> Unit) = coroutinesTestRule.testDispatcher.runBlockingTest {
        FadingRobot(this)
            .apply { block() }
    }

    private class FadingRobot(
        private val testCoroutineScope: TestCoroutineScope
    ) {
        private val delay: Long = Long.MAX_VALUE - 1L // Intentionally long
        private val context: Context get() = ApplicationProvider.getApplicationContext()
        private val fadable: View
        private val container: FadingFrameLayout
        private val fadableId = View.generateViewId()
        private val debouncerId = View.generateViewId()
        private val nonDebouncerId = View.generateViewId()
        private val containerId = View.generateViewId()

        init {
            val debouncer = FrameLayout(context).apply {
                id = debouncerId
                background = ColorDrawable(Color.YELLOW)
                layoutParams = ViewGroup.LayoutParams(250, 250)
                isClickable = true
            }
            val nonDebouncer = FrameLayout(context).apply {
                id = nonDebouncerId
                background = ColorDrawable(Color.GREEN)
                layoutParams = FrameLayout.LayoutParams(250, 250).apply {
                    gravity = Gravity.END
                }
                isClickable = true
            }

            fadable = FrameLayout(context).apply {
                id = fadableId
                background = ColorDrawable(Color.BLUE)
                addView(debouncer)
                addView(nonDebouncer)
            }

            container = FadingFrameLayout(context).apply {
                id = containerId
                addView(fadable)
                setDelay(delay)
                setFadeDuration(0L) // Instant animation
                addDebouncer(debouncer)
                setFadable(fadable)
                resume() // Kick things off
            }

            launchFragmentInContainer { TestFragment(container) }
        }

        fun assertFadableVisibility(isVisible: Boolean) {
            val visibility = if (isVisible) {
                isDisplayed()
            } else {
                not(isDisplayed())
            }
            onView(withId(fadableId))
                .check(matches(visibility))
        }

        fun awaitDelayAndAnimation() {
            testCoroutineScope.advanceUntilIdle()
            onView(withId(fadableId))
                .perform(AwaitAnimation())
        }

        fun tapOnContainer() {
            onView(withId(containerId))
                .perform(click())
        }

        fun tapOnDebouncable() {
            onView(withId(debouncerId))
                .perform(click())
        }

        fun tapOnNonDebouncable() {
            onView(withId(nonDebouncerId))
                .perform(click())
        }

        fun pauseFading() {
            container.pause()
        }

        fun resumeFading() {
            container.resume()
        }
    }
}

internal class TestFragment(private val v: View) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = v
}
