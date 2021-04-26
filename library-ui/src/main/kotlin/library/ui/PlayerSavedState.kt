package library.ui

import androidx.lifecycle.SavedStateHandle
import library.common.PlayerState
import kotlin.reflect.KProperty

class PlayerSavedState(private val handle: SavedStateHandle) {
    operator fun getValue(thisRef: PlayerViewModel, property: KProperty<*>): PlayerState? {
        return handle[KEY_PLAYER_STATE]
    }

    operator fun setValue(thisRef: PlayerViewModel, property: KProperty<*>, value: PlayerState?) {
        handle[KEY_PLAYER_STATE] = value
    }

    companion object {
        private const val KEY_PLAYER_STATE = "player_state"
    }
}
