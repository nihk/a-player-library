package player.ui

import android.os.Bundle
import androidx.fragment.app.Fragment

interface Navigator {
    fun toDialog(clazz: Class<out Fragment>, bundle: Bundle? = null)
    fun replace(clazz: Class<out Fragment>, bundle: Bundle? = null)
}
