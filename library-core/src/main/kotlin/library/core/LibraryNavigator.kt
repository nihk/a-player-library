package library.core

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import library.ui.Navigator

internal class LibraryNavigator(
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int
) : Navigator {
    override fun toDialog(clazz: Class<out Fragment>, bundle: Bundle?) {
        fragmentManager.beginTransaction()
            .add(clazz, bundle, null)
            .commit()
    }

    override fun replace(clazz: Class<out Fragment>, bundle: Bundle?) {
        fragmentManager.beginTransaction()
            .replace(containerId, clazz, bundle)
            .commit()
    }
}
