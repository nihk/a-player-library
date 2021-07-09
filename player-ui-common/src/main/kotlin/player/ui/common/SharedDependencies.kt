package player.ui.common

data class SharedDependencies(
    val seekBarListenerFactory: SeekBarListener.Factory,
    val navigator: Navigator,
)
