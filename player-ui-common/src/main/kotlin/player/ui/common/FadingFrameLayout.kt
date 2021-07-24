package player.ui.common

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import player.common.requireNotNull
import kotlin.math.abs

// todo: save/restore visibility state
class FadingFrameLayout : FrameLayout, View.OnClickListener {
    constructor(context: Context) : super(context) {
        initialize(context)
    }
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initialize(context)
    }
    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attributeSet, defStyleAttr) {
        initialize(context)
    }

    private var scope: CoroutineScope? = null
    private var hide: Job? = null
    private var delay: Long = 3_000L
    private var fadable: View? = null
    private var touchSlop: Int = -1
    private var down: PointF? = null

    private fun initialize(context: Context) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        setOnClickListener(this)
    }

    fun setFadable(view: View) {
        fadable = view
        hide()
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
        hide?.cancel()

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
        hide?.cancel()

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

    override fun onClick(v: View?) {
        if (requireFadable().isVisible) {
            hide(withDelay = false)
        } else {
            show()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = PointF(ev.rawX, ev.rawY)
            }
            MotionEvent.ACTION_UP -> {
                val down = down.requireNotNull()
                val didClickChild = abs(ev.rawX - down.x) <= touchSlop
                    || abs(ev.rawY - down.y) <= touchSlop
                if (didClickChild) {
                    // User clicked a child control, so debounce the fade.
                    show()
                }
            }
        }

        return false
    }

    private fun requireFadable() = fadable.requireNotNull()
    private fun requireScope(): CoroutineScope {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
        return scope.requireNotNull()
    }
}
