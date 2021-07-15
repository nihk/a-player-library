package player.ui.trackspicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.PlayerEvent
import player.common.TrackInfo
import player.common.requireNotNull
import player.ui.controller.PlayerNonConfig
import player.ui.controller.PlayerViewModel
import player.ui.trackspicker.databinding.TracksFragmentBinding

// fixme: setting a video quality track should also (hidden to user) set every video quality
//  below that one, too (?)
class TracksPickerFragment : BottomSheetDialogFragment() {
    private val trackType: TrackInfo.Type
        get() = requireArguments().getSerializable(KEY_ARG_TRACK_TYPE).requireNotNull() as TrackInfo.Type
    // fixme: need to pass in factory here, for process recreation handling
    private val viewModel: PlayerViewModel by activityViewModels()
    private val playerNonConfig: PlayerNonConfig by lazy {
        viewModel.getLatest().requireNotNull()
    }
    private var binding: TracksFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TracksFragmentBinding.inflate(inflater, container, false)
        return requireBinding().root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = TracksAdapter { action ->
            when (action) {
                is TrackOption.Action.Clear -> playerNonConfig.clearTrackInfos(action.rendererIndex)
                is TrackOption.Action.Set -> {
                    val selected = action.trackInfo.copy(isSelected = true)
                    playerNonConfig.setTrackInfos(listOf(selected))
                }
            }
            dismiss()
        }
        requireBinding().recyclerView.adapter = adapter

        submitTrackOptions(adapter)

        playerNonConfig.playerEvents()
            .onEach { playerEvent ->
                when (playerEvent) {
                    is PlayerEvent.OnTracksChanged -> {
                        submitTrackOptions(adapter)
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun trackInfosBy(type: TrackInfo.Type): List<TrackInfo> {
        return playerNonConfig.tracks().filter { it.type == type }
    }

    private fun List<TrackInfo>.toTrackOptions(): List<TrackOption> {
        return map { trackInfo ->
            TrackOption(
                id = trackInfo.indices.groupIndex, // fixme: this might not be consistent for other player impl's, e.g. MediaPlayer
                name = trackInfo.name ?: "Unknown",
                isSelected = trackInfo.isManuallySet,
                action = TrackOption.Action.Set(trackInfo)
            )
        }
    }

    private fun submitTrackOptions(adapter: TracksAdapter) {
        val trackInfos = trackInfosBy(trackType)
        val trackOptions = if (trackInfos.isEmpty()) {
            // todo: this is a bit jarring when tracks update/change, which puts the player in a
            //  temporary state where there are no tracks, but I also don't want tracks to be
            //  selectable when none exist.
            emptyList()
        } else {
            val hasManuallySetOption = trackInfos.any(TrackInfo::isManuallySet)
            val auto = TrackOption(
                id = -1,
                name = "Auto",
                isSelected = !hasManuallySetOption,
                action = TrackOption.Action.Clear(trackInfos.first().indices.rendererIndex) // These will all be the same
            )
            listOf(auto) + trackInfosBy(trackType).toTrackOptions()
        }
        adapter.submitList(trackOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun requireBinding() = requireNotNull(binding)

    companion object {
        private const val KEY_ARG_TRACK_TYPE = "track_type"

        fun create(type: TrackInfo.Type): TracksPickerFragment {
            return TracksPickerFragment().apply {
                arguments = bundleOf(KEY_ARG_TRACK_TYPE to type)
            }
        }
    }
}
