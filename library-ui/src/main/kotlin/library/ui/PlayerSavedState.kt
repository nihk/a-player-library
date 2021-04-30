package library.ui

import androidx.lifecycle.SavedStateHandle
import library.common.PlayerState

class PlayerSavedState(private val handle: SavedStateHandle) {
    var value: PlayerState?
        get() = handle[KEY_PLAYER_STATE]
        set(value) { handle[KEY_PLAYER_STATE] = value }

    companion object {
        private const val KEY_PLAYER_STATE = "player_state"
    }
}
