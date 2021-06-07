package player.ui.sve

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
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
}