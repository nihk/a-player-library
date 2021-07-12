package player.ui.common

import android.view.View

fun View.setOnSingleClickListener(block: (View) -> Unit) {
    val listener = object : OnSingleClickListener() {
        override fun onSingleClick(view: View) {
            block(view)
        }
    }
    setOnClickListener(listener)
}

abstract class OnSingleClickListener(
    private val throttle: Long = 500L,
    private val currentTime: () -> Long = { System.currentTimeMillis() }
) : View.OnClickListener {
    private var lastClickTime: Long = 0L

    final override fun onClick(v: View) {
        val current = currentTime()
        val elapsedTime = current - lastClickTime
        lastClickTime = current

        if (elapsedTime <= throttle) {
            return
        }

        onSingleClick(v)
    }

    abstract fun onSingleClick(view: View)
}
