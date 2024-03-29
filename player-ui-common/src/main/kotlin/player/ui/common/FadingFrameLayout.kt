package player.ui.common

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.content.res.use
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import player.common.requireNotNull
import player.ui.controller.contains
import kotlin.math.abs

/**
 * A layout that fades a child View after a delay.
 *
 * Clicks on specified children can debounce that fade.
 *
 * Clicks on this layout itself and unspecific children will toggle the visibility without a
 * delay (outside the animation).
 */
class FadingFrameLayout : FrameLayout, View.OnClickListener {
    constructor(context: Context) : super(context) {
        initialize(context, null, 0)
    }
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initialize(context, attributeSet, 0)
    }
    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attributeSet, defStyleAttr) {
        initialize(context, attributeSet, defStyleAttr)
    }

    private var touchSlop: Int = -1
    private var down: PointF? = null
    // A CoroutineScope is used to manage delays rather than ViewPropertyAnimator.setDelay
    // because it makes it easier to have fast tests, where delays can be flattened.
    private var scope: CoroutineScope? = null
    private var hide: Job? = null

    /** Attributes **/
    private var delay: Long? = null
    private var fadeDuration: Long? = null
    // Clicks on debouncers will debounce the fade delay.
    private val debouncers = mutableListOf<View>()
    private var debouncerIds: List<Int>? = null
    // The View to fade in and out.
    private var fadable: View? = null
    private var fadableId: Int? = null
    /** State **/
    private var isPaused: Boolean = true

    private fun initialize(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        setOnClickListener(this)
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.FadingFrameLayout,
            defStyleAttr,
            0
        ).use { typedArray ->
            debouncerIds = typedArray.getString(R.styleable.FadingFrameLayout_debouncers)
                ?.split(",")
                ?.map { name -> resources.getIdentifier(name.trim(), "id", context.packageName) }

            fadableId = typedArray.getResourceId(R.styleable.FadingFrameLayout_fadable, 0)
            delay = typedArray.getInteger(R.styleable.FadingFrameLayout_delay, 2_000).toLong()
            fadeDuration = typedArray.getInteger(R.styleable.FadingFrameLayout_fade_duration, 300).toLong()
        }

        doOnAttach {
            debouncers += debouncerIds?.map { id -> findViewById<View>(id).requireNotNull() }.orEmpty()
        }
    }

    fun setDelay(delay: Long) {
        this.delay = delay
    }

    fun setFadable(fadable: View) {
        this.fadable = fadable
    }

    fun setFadeDuration(fadeDuration: Long) {
        this.fadeDuration = fadeDuration
    }

    fun addDebouncer(debouncer: View) {
        debouncers += debouncer
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
        cancelWork()
    }

    fun pause() {
        isPaused = true

        val isMidFadingAnimation = requireFadable().isVisible && requireFadable().alpha < 1f
        if (isMidFadingAnimation) {
            // Animate back to 100% alpha.
            show()
        } else {
            // Stop any hide animation queued up.
            cancelWork()
        }
    }

    fun resume() {
        isPaused = false

        // No point in starting a hide animation if the View is already not visible.
        if (requireFadable().isVisible) {
            hide(withDelay = true)
        }
    }

    private fun hide(withDelay: Boolean) {
        cancelWork()

        fun hideAnimation() {
            requireFadable()
                .animate()
                .setDuration(fadeDuration.requireNotNull())
                .alpha(0f)
                .withEndAction { requireFadable().isVisible = false }
        }

        if (withDelay) {
            hide = requireScope().launch {
                delay(delay.requireNotNull())
                hideAnimation()
            }
        } else {
            hideAnimation()
        }
    }

    private fun show() {
        cancelWork()

        requireFadable()
            .animate()
            .setDuration(fadeDuration.requireNotNull())
            .withStartAction { requireFadable().isVisible = true }
            .alpha(1f)
            .withEndAction {
                if (!isPaused) {
                    hide(withDelay = true)
                }
            }
    }

    private fun cancelWork() {
        hide?.cancel()
        fadable?.animate()?.cancel()
    }

    override fun onClick(v: View?) {
        if (requireFadable().isVisible) {
            hide(withDelay = false)
        } else {
            show()
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> down = PointF(event.rawX, event.rawY)
            MotionEvent.ACTION_UP -> {
                val down = down.requireNotNull()
                val tapped = abs(event.rawX - down.x) <= touchSlop
                    || abs(event.rawY - down.y) <= touchSlop
                if (tapped) {
                    if (!requireFadable().isVisible
                        || debouncers.any { debouncer -> down in debouncer }) {
                        show()
                    } else {
                        // Intercepted a child View tap, but wasn't a debouncer, so treat as
                        // if it were a tap on this View, i.e. hide immediately.
                        hide(withDelay = false)
                    }
                }
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    // Can't rely on View.doOn* callbacks to initialize this -- child Views invoke this before
    // Views are attached/laid out.
    private fun requireFadable(): View {
        if (fadable == null) {
            fadable = findViewById<View>(fadableId.requireNotNull()).also(::setFadable)
        }

        return fadable.requireNotNull()
    }

    private fun requireScope(): CoroutineScope {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
        return scope.requireNotNull()
    }
}
