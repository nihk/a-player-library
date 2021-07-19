package player.ui.controller

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import player.ui.common.PlayerArguments

class PlayerViewModel(
    private val handle: SavedStateHandle,
    private val playerNonConfigFactory: PlayerNonConfig.Factory
) : ViewModel() {
    private val playerNonConfigs: LinkedHashMap<String, PlayerNonConfig> = linkedMapOf()

    fun get(playerArguments: PlayerArguments): PlayerNonConfig {
        return playerNonConfigs.getOrPut(playerArguments.id) {
            playerNonConfigFactory.create(playerArguments, handle)
        }
    }

    // fixme: hack to avoid passing parameters
    fun getLatest(): PlayerNonConfig? {
        return playerNonConfigs.values.lastOrNull()
    }

    fun remove(id: String) {
        val playerNonConfig = playerNonConfigs[id]
        playerNonConfigs.remove(id)
        playerNonConfig?.close()
    }

    override fun onCleared() {
        playerNonConfigs.values.forEach { playerNonConfig ->
            playerNonConfig.close()
        }
        playerNonConfigs.clear()
    }

    class Factory(private val playerNonConfigFactory: PlayerNonConfig.Factory) {
        fun create(owner: SavedStateRegistryOwner): AbstractSavedStateViewModelFactory {
            return object : AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return PlayerViewModel(handle, playerNonConfigFactory) as T
                }
            }
        }
    }
}
