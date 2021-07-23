package player.ui.controller

import android.view.View
import com.google.android.material.snackbar.Snackbar

interface ErrorRenderer {
    fun render(view: View, message: String, action: Action? = null)

    data class Action(
        val name: String,
        val callback: (View) -> Unit
    )
}

class SnackbarErrorRenderer : ErrorRenderer {
    override fun render(view: View, message: String, action: ErrorRenderer.Action?) {
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
            .apply {
                if (action != null) {
                    setAction(action.name, action.callback)
                }
            }
            .show()
    }
}
