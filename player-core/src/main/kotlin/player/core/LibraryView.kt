package player.core

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import player.ui.common.PlayerArguments

class LibraryView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attributeSet, defStyleAttr)

    fun initialize(playerArguments: PlayerArguments) {
        // todo: handle fragment manager? wrap FragmentFactory?
        val module = LibraryModule(context as FragmentActivity)
        val playerView = module.playerViewFactory.create(context, playerArguments)
        addView(playerView)
    }
}
