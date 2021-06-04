package player.ui.common

import player.common.SeekData

data class UiState(
    val isControllerUsable: Boolean,
    val showLoading: Boolean,
    val seekData: SeekData
) {
    companion object {
        val INITIAL = UiState(
            isControllerUsable = false,
            showLoading = true,
            seekData = SeekData.INITIAL
        )
    }
}
