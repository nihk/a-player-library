package player.ui.controller

import android.view.View
import com.google.android.material.snackbar.Snackbar

interface ErrorRenderer {
    fun render(view: View, message: String)
}

class SnackbarErrorRenderer : ErrorRenderer {
    override fun render(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}
