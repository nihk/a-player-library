package player.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import player.test.CoroutinesTestRule
import kotlin.coroutines.resume

class FadingFrameLayoutTest {
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun fadableViewIsGoneAfterDelay() = fadingRobot {
        assertFadableVisibility(isVisible = true)
        awaitDelay()
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
        private val container: ViewGroup
        private val fadableId = View.generateViewId()
        private val debouncerId = View.generateViewId()
        private val containerId = View.generateViewId()

        init {
            val debouncer = FrameLayout(context).apply {
                id = debouncerId
                background = ColorDrawable(Color.YELLOW)
                layoutParams = ViewGroup.LayoutParams(250, 250)
            }

            fadable = FrameLayout(context).apply {
                id = fadableId
                background = ColorDrawable(Color.BLUE)
                addView(debouncer)
            }

            container = FadingFrameLayout(context).apply {
                id = containerId
                addView(fadable)
                setDelay(delay)
                setFadeDuration(0L) // Instant animation
                addDebouncer(debouncer)
                setFadable(fadable)
            }

            launchFragmentInContainer { TestFragment(container) }
        }

        suspend fun assertFadableVisibility(isVisible: Boolean) {
            val visibility = if (isVisible) {
                isDisplayed()
            } else {
                not(isDisplayed())
            }
            withTimeout(5_000L) {
                while (fadable.isVisible != isVisible) {
                }
            }
            onView(withId(fadableId))
                .check(matches(visibility))
        }

        suspend fun awaitDelay() {
            testCoroutineScope.advanceUntilIdle()
            fadable.blah()
        }

        suspend fun View.blah() = suspendCancellableCoroutine<Unit> {
            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    it.resume(Unit)
                }
            }

            it.invokeOnCancellation {
                viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }

            viewTreeObserver.addOnGlobalLayoutListener(listener)
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
