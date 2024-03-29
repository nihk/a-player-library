package nick.sample.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.abs

class DraggableFrameLayout : FrameLayout {
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

    private var touchSlop: Int = -1
    private var down: Down? = null
    private var delta: Down? = null
    private var parentBounds: RectF? = null

    private fun initialize(context: Context) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = Down(ev.rawX, ev.rawY)
                delta = Down(x - ev.rawX, y - ev.rawY)
                false
            }
            MotionEvent.ACTION_MOVE -> {
                val down = requireNotNull(down)
                return abs(ev.rawX - down.x) >= touchSlop || abs(ev.rawY - down.y) >= touchSlop
            }
            else -> false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (parentBounds == null) {
            val container = parent as ViewGroup
            parentBounds = RectF(
                0f,
                0f,
                container.x + container.width - width,
                container.y + container.height - height
            )
        }

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val delta = requireNotNull(delta)
                val bounds = requireNotNull(parentBounds)
                x = (event.rawX + delta.x).coerceIn(bounds.left, bounds.right)
                y = (event.rawY + delta.y).coerceIn(bounds.top, bounds.bottom)
            }
        }

        return true
    }

    data class Down(
        val x: Float,
        val y: Float
    )
}
