package player.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory

internal class LibraryFragmentFactory(
    private val map: Map<Class<out Fragment>, () -> Fragment>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return map[loadFragmentClass(classLoader, className)]?.invoke()
            ?: super.instantiate(classLoader, className)
    }
}