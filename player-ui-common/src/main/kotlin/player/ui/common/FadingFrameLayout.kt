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
    private var delay: Long = 3_000L
    private var fadable: View? = null
    private var touchSlop: Int = -1
    private var down: PointF? = null
    private val debouncers = mutableListOf<View>()
    private var debouncerIds: List<Int>? = null
    private var fadableId: Int? = null

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
                .orEmpty()
                .map { name -> resources.getIdentifier(name, "id", context.packageName) }

            fadableId = typedArray.getResourceId(R.styleable.FadingFrameLayout_fadable, 0)
        }

        doOnAttach {
            debouncers += debouncerIds?.map { id -> findViewById<View>(id).requireNotNull() }.orEmpty()
            fadable = fadableId?.let { findViewById(it) }
        }
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
                delay(delay)
                hideAnimation()
            }
        } else {
            hideAnimation()
        }
    }

    fun show(hideAtEnd: Boolean = true) {
        cancelJob()

        requireFadable()
            .animate()
            .withStartAction { requireFadable().isVisible = true }
            .alpha(1f)
            .withEndAction {
                if (hideAtEnd) {
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

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> down = PointF(ev.rawX, ev.rawY)
            MotionEvent.ACTION_MOVE -> cancelJob()
            MotionEvent.ACTION_UP -> {
                val down = down.requireNotNull()
                val tapped = abs(ev.rawX - down.x) <= touchSlop
                    || abs(ev.rawY - down.y) <= touchSlop
                if (tapped) {
                    if (!requireFadable().isVisible
                        || debouncers.any { debouncer -> down in debouncer }) {
                        show()
                    } else {
                        hide(withDelay = false)
                    }
                } else {
                    // Moved
                    show()
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun requireFadable() = fadable.requireNotNull()
    private fun requireScope(): CoroutineScope {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
        return scope.requireNotNull()
    }
}
