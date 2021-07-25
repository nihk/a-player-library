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

// todo: save/restore visibility state
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

    private var scope: CoroutineScope? = null
    private var hide: Job? = null
    private var delay: Long? = null
    private var touchSlop: Int = -1
    private var down: PointF? = null
    // Clicks on debouncers will restart the fade delay.
    private val debouncers = mutableListOf<View>()
    private var debouncerIds: List<Int>? = null
    // The View to fade in and out.
    private var fadable: View? = null
    private var fadableId: Int? = null
    private var isFadingEnabled: Boolean = true

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
            delay = typedArray.getInteger(R.styleable.FadingFrameLayout_delay, 3_000).toLong()
        }

        doOnAttach {
            debouncers += debouncerIds?.map { id -> findViewById<View>(id).requireNotNull() }.orEmpty()
            fadable = fadableId?.let { findViewById(it) }
            hide() // Kick things off
        }
    }

    fun setDelay(delay: Long) {
        this.delay = delay
    }

    fun setFadable(fadable: View) {
        this.fadable = fadable
    }

    fun addDebouncer(debouncer: View) {
        debouncers += debouncer
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requireScope()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }

    fun setFadingEnabled(isEnabled: Boolean) {
        if (isFadingEnabled == isEnabled) return

        isFadingEnabled = isEnabled
        if (isEnabled) {
            if (requireFadable().isVisible) {
                // Restart
                hide()
            }
        } else {
            cancelJob()
        }
    }

    fun hide(withDelay: Boolean = true) {
        cancelJob()

        fun hideAnimation() {
            requireFadable()
                .animate()
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

    fun show() {
        cancelJob()

        requireFadable()
            .animate()
            .withStartAction { requireFadable().isVisible = true }
            .alpha(1f)
            .withEndAction {
                if (isFadingEnabled) {
                    hide()
                }
            }
    }

    private fun cancelJob() {
        hide?.cancel()
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

    private fun requireFadable() = fadable.requireNotNull()

    private fun requireScope(): CoroutineScope {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
        return scope.requireNotNull()
    }
}
