package player.ui.controller

import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import player.common.requireNotNull

fun View.requireViewTreeLifecycleOwner() = findViewTreeLifecycleOwner().requireNotNull()
fun View.requireViewTreeViewModelStoreOwner() = findViewTreeViewModelStoreOwner().requireNotNull()
fun View.requireViewTreeSavedStateRegistryOwner() = findViewTreeSavedStateRegistryOwner().requireNotNull()
