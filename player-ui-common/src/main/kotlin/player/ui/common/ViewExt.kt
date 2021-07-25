package player.ui.controller

import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import player.common.requireNotNull

fun View.requireViewTreeLifecycleOwner() = findViewTreeLifecycleOwner().requireNotNull()
fun View.requireViewTreeViewModelStoreOwner() = findViewTreeViewModelStoreOwner().requireNotNull()
fun View.requireViewTreeSavedStateRegistryOwner() = findViewTreeSavedStateRegistryOwner().requireNotNull()

fun View.detachFromParent() {
    val parent = parent as? ViewGroup ?: return
    parent.removeView(this)
}

operator fun View.contains(pointF: PointF): Boolean {
    return getRectOnScreen().contains(pointF.x.toInt(), pointF.y.toInt())
}

fun View.getRectOnScreen(): Rect {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return Rect(
        location.first(),
        location.last(),
        location.first() + width,
        location.last() + height
    )
}
