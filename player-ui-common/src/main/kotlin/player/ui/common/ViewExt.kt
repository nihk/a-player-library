package player.ui.controller

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
