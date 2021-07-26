package player.ui.common

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
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
    private var scope: CoroutineScope? = null
    private var hide: Job? = null

    /** Attributes **/
    private var delay: Long? = null
    private var duration: Long? = null
    // Clicks on debouncers will debounce the fade delay.
    private val debouncers = mutableListOf<View>()
    private var debouncerIds: List<Int>? = null
    // The View to fade in and out.
    private var fadable: View? = null
    private var fadableId: Int? = null

    /** State **/
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
            delay = typedArray.getInteger(R.styleable.FadingFrameLayout_delay, 2_000).toLong()
            duration = typedArray.getInteger(R.styleable.FadingFrameLayout_fade_duration, 300).toLong()
        }

        doOnAttach {
            debouncers += debouncerIds?.map { id -> findViewById<View>(id).requireNotNull() }.orEmpty()
            val fadableId = fadableId
            if (fadableId != null && fadableId != 0) {
                fadable = findViewById(fadableId)
                setFadable(requireFadable())
            }
        }
    }

    fun setDelay(delay: Long) {
        this.delay = delay
    }

    fun setFadable(fadable: View) {
        this.fadable = fadable
        hide(withDelay = true) // Kick things off
    }

    fun setFadeDuration(duration: Long) {
        this.duration = duration
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

    fun setFadingEnabled(isEnabled: Boolean) {
        if (isFadingEnabled == isEnabled) return

        isFadingEnabled = isEnabled
        if (isEnabled) {
            if (requireFadable().isVisible) { // No point in hiding something already hidden.
                hide(withDelay = true)
            }
        } else {
            show()
        }
    }

    fun hide(withDelay: Boolean) {
        cancelWork()

        fun hideAnimation() {
            requireFadable()
                .animate()
                .setDuration(duration.requireNotNull())
                .alpha(0f)
                .withEndAction {
                    requireFadable().isVisible = false
                }
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
        cancelWork()

        requireFadable()
            .animate()
            .setDuration(duration.requireNotNull())
            .withStartAction { requireFadable().isVisible = true }
            .alpha(1f)
            .withEndAction {
                if (isFadingEnabled) {
                    hide(withDelay = true)
                }
            }
    }

    private fun cancelWork() {
        hide?.cancel()
        fadable?.animate()?.cancel()
    }

    override fun onClick(v: View?) {
        Log.d("qwer", "clicked")
        if (requireFadable().isVisible) {
            hide(withDelay = false)
        } else {
            show()
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Log.d("qwer", "event: ${event.actionMasked}")
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
