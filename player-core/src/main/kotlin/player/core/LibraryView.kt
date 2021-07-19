package player.core

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import player.common.requireNotNull
import player.ui.common.PlayerArguments

class LibraryView : FrameLayout {
    val isPlaying: Boolean get() = childCount == 1

    private var playerArguments: PlayerArguments? = null
    private var keyPlayerNonConfig: String? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attributeSet, defStyleAttr)

    fun play(playerArguments: PlayerArguments) {
        stop()
        this.playerArguments = playerArguments
        val module = LibraryModule(context as FragmentActivity)
        val playerView = module.playerViewFactory.create(
            context = context,
            playerArguments = playerArguments,
        )
        addView(playerView)
    }

    fun stop() {
        playerArguments = null
        keyPlayerNonConfig = null
        removeAllViews()
    }

    override fun onSaveInstanceState(): Parcelable {
        return bundleOf(
            KEY_SUPER_STATE to super.onSaveInstanceState(),
            KEY_PLAYER_ARGUMENTS to playerArguments,
            KEY_PLAYER_NON_CONFIG to keyPlayerNonConfig
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var viewState = state
        if (viewState is Bundle) {
            playerArguments = viewState.getParcelable(KEY_PLAYER_ARGUMENTS)
            viewState = viewState.getParcelable(KEY_SUPER_STATE)
            if (playerArguments != null) {
                play(playerArguments.requireNotNull())
            }
        }
        super.onRestoreInstanceState(viewState)
    }

    companion object {
        private const val KEY_SUPER_STATE = "library_view:super_state"
        private const val KEY_PLAYER_ARGUMENTS = "library_view:player_arguments"
        private const val KEY_PLAYER_NON_CONFIG = "library_view:player_non_config"
    }
}
