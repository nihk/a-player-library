package player.core

import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import player.common.requireNotNull
import player.ui.common.PlayerArguments
import java.util.*

class LibraryView : FrameLayout {
    private var playerArguments: PlayerArguments? = null
    private var uuid: UUID? = null
    val isPlaying: Boolean get() = childCount == 1

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attributeSet, defStyleAttr)

    fun play(playerArguments: PlayerArguments) {
        play(playerArguments, uuid ?: UUID.randomUUID())
    }

    private fun play(playerArguments: PlayerArguments, uuid: UUID) {
        removeAllViews()
        // todo: how handle fragment manager? wrap FragmentFactory? what about state restoration?
        this.playerArguments = playerArguments
        this.uuid = uuid
        val module = LibraryModule(context as FragmentActivity)
        val playerView = module.playerViewFactory.create(
            context = context,
            playerArguments = playerArguments,
            uuid = uuid
        )
        addView(playerView)
    }

    fun stop() {
        playerArguments = null
        uuid = null
        removeAllViews()
    }

    override fun onSaveInstanceState(): Parcelable {
        return bundleOf(
            KEY_SUPER_STATE to super.onSaveInstanceState(),
            KEY_PLAYER_ARGUMENTS to playerArguments,
            KEY_UUID to uuid?.let { ParcelUuid(uuid) }
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var viewState = state
        if (viewState is Bundle) {
            playerArguments = viewState.getParcelable(KEY_PLAYER_ARGUMENTS)
            uuid = viewState.getParcelable<ParcelUuid>(KEY_UUID)?.uuid
            viewState = viewState.getParcelable(KEY_SUPER_STATE)
            if (playerArguments != null && uuid != null) {
                play(playerArguments.requireNotNull(), uuid.requireNotNull())
            }
        }
        super.onRestoreInstanceState(viewState)
    }

    companion object {
        private const val KEY_SUPER_STATE = "library_view:super_state"
        private const val KEY_PLAYER_ARGUMENTS = "library_view:player_arguments"
        private const val KEY_UUID = "library_view:uuid"
    }
}
