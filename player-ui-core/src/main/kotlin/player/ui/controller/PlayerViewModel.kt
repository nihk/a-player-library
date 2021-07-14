package player.ui.controller

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import java.util.*
import kotlin.collections.LinkedHashMap

class PlayerViewModel(
    private val handle: SavedStateHandle,
    private val playerNonConfigFactory: PlayerNonConfig.Factory
) : ViewModel() {
    private val playerNonConfigs: LinkedHashMap<UUID, PlayerNonConfig> = linkedMapOf()

    fun get(uuid: UUID, uri: String): PlayerNonConfig {
        return playerNonConfigs[uuid] ?: run {
            playerNonConfigFactory.create(uuid, handle, uri).also {
                playerNonConfigs[uuid] = it
            }
        }
    }

    // fixme: hack to avoid passing parameters
    fun getLatest(): PlayerNonConfig? {
        return playerNonConfigs.values.lastOrNull()
    }

    fun remove(uuid: UUID) {
        val playerNonConfig = playerNonConfigs[uuid]
        playerNonConfigs.remove(uuid)
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
