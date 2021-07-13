package player.ui.controller

import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import player.common.requireNotNull

internal fun View.requireViewTreeLifecycleOwner() = findViewTreeLifecycleOwner().requireNotNull()
internal fun View.requireViewTreeViewModelStoreOwner() = findViewTreeViewModelStoreOwner().requireNotNull()
internal fun View.requireViewTreeSaveStateRegistryOwner() = findViewTreeSavedStateRegistryOwner().requireNotNull()
