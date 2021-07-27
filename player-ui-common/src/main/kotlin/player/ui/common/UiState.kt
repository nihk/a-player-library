package player.ui.common

data class UiState(
    val isControllerUsable: Boolean,
    val showLoading: Boolean,
    val isInPip: Boolean
) {
    companion object {
        val INITIAL = UiState(
            isControllerUsable = false,
            showLoading = true,
            isInPip = false
        )
    }
}
