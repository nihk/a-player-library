package nick.sample.data

import android.view.View
import android.view.ViewGroup

fun View.detachFromParent() {
    val parent = parent as? ViewGroup ?: return
    parent.removeView(this)
}
