package player.ui.trackspicker

import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.PlayerEvent
import player.common.TrackInfo
import player.common.requireNotNull
import player.ui.common.PlayerController
import player.ui.trackspicker.databinding.TracksPickerDialogBinding

class TracksPickerDialog(
    context: Context,
    private val playerController: PlayerController,
    private val config: TracksPickerConfig
) : BottomSheetDialog(context), LifecycleOwner {
    private var binding: TracksPickerDialogBinding? = null
    private val registry = LifecycleRegistry(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registry.currentState = Lifecycle.State.CREATED
        binding = TracksPickerDialogBinding.inflate(layoutInflater)
        setContentView(requireBinding().root)

        // On landscape orientation, show as much of the dialog as possible on the screen.
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        requireBinding().title.setText(config.title)

        val adapter = TracksAdapter { action ->
            when (action) {
                is TrackOption.Action.Clear -> playerController.clearTrackInfos(action.rendererIndex)
                is TrackOption.Action.Set -> {
                    val selected = action.trackInfo.copy(isSelected = true)
                    playerController.setTrackInfos(listOf(selected))
                }
            }
            dismiss()
        }
        requireBinding().recyclerView.adapter = adapter

        submitTrackOptions(adapter)

        playerController.playerEvents()
            .onEach { playerEvent ->
                when (playerEvent) {
                    is PlayerEvent.OnTracksChanged -> {
                        submitTrackOptions(adapter)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun queryTrackInfos(): List<TrackInfo> {
        return playerController.tracks()
            .filter { it.type == config.type }
            .let { config.mapper(it) }
    }

    private fun List<TrackInfo>.toTrackOptions(): List<TrackOption> {
        return map { trackInfo ->
            TrackOption(
                id = trackInfo.indices.groupIndex, // fixme: this might not be consistent for other player impl's, e.g. MediaPlayer
                name = trackInfo.name ?: "Unknown",
                isSelected = trackInfo.isManuallySet || (!config.withAuto && trackInfo.isSelected),
                action = TrackOption.Action.Set(trackInfo)
            )
        }
    }

    private fun submitTrackOptions(adapter: TracksAdapter) {
        val trackInfos = queryTrackInfos()
        requireBinding().loading.isVisible = trackInfos.isEmpty()
        if (trackInfos.isNotEmpty()) {
            val trackOptions = if (config.withAuto) {
                val hasManuallySetOption = trackInfos.any(TrackInfo::isManuallySet)
                val auto = TrackOption(
                    id = -1,
                    name = context.getString(R.string.auto),
                    isSelected = !hasManuallySetOption,
                    action = TrackOption.Action.Clear(trackInfos.first().indices.rendererIndex) // These will all be the same
                )
                listOf(auto) + trackInfos.toTrackOptions()
            } else {
                trackInfos.toTrackOptions()
            }
            adapter.submitList(trackOptions)
        }
    }

    override fun onDetachedFromWindow() {
        registry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle = registry

    private fun requireBinding() = binding.requireNotNull()
}
