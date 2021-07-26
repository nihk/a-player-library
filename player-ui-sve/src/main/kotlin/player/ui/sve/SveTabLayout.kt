package player.ui.sve

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

/**
 * Draws the [tabSelectedIndicator] on top (i.e. z-index wise) of [TabLayout.Tab]s.
 * Usually the tab indicator is drawn underneath tabs, but for the SVE design, it should be drawn on
 * top.
 *
 * [shouldDraw] exists because drawing [tabSelectedIndicator] in this class happens _before_
 * [TabLayout.SlidingTabIndicator] updates the Drawable's bounds and calls its
 * [TabLayout.SlidingTabIndicator.draw] -- they are out of sync. If [shouldDraw] was not used,
 * the final state is [tabSelectedIndicator] drawn twice with different bounds.
 */
internal class SveTabLayout : TabLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr)

    private var shouldDraw = false
    private var touchStateListener: TouchStateListener? = null

    init {
        addTabLayoutInsets()
    }

    val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            shouldDraw = state != ViewPager2.SCROLL_STATE_IDLE
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (shouldDraw) {
            tabSelectedIndicator.draw(canvas)
        }
    }

    private fun addTabLayoutInsets() {
        // A bit of a hack, but the API isn't flexible.
        val slidingTabIndicator = getChildAt(0)
        val itemWidth = context.resources.getDimension(R.dimen.sve_item_side_length)
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val itemPadding = context.resources.getDimension(R.dimen.tab_padding_horizontal)
        val padding = (screenWidth / 2 - itemWidth / 2 - itemPadding / 2).toInt()
        ViewCompat.setPaddingRelative(slidingTabIndicator, padding, 0, padding, 0)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val touchState = when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> TouchState.Down
            MotionEvent.ACTION_MOVE -> TouchState.Moving
            MotionEvent.ACTION_UP -> TouchState.Up
            else -> TouchState.Unknown
        }
        touchStateListener?.onTouchState(touchState)
        return super.onTouchEvent(ev)
    }

    fun setTouchStateListener(listener: TouchStateListener) {
        this.touchStateListener = listener
    }

    interface TouchStateListener {
        fun onTouchState(touchState: TouchState)
    }

    enum class TouchState {
        Unknown,
        Down,
        Moving,
        Up
    }
}
