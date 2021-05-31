package player.ui

import player.common.PlayerEvent

interface PlayerEmissionsHandler {
    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onUiState(uiState: UiState)
}
