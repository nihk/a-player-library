package player.ui.trackspicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import player.common.TrackInfo
import player.common.requireNotNull
import player.ui.controller.PlayerViewModel
import player.ui.trackspicker.databinding.TracksFragmentBinding

// todo: query tracks from VM directly and listen for changes
class TracksPickerFragment : BottomSheetDialogFragment() {
    private val trackInfos: List<TrackInfo>
        get() = requireArguments().getParcelableArrayList<TrackInfo>(KEY_ARG_TRACK_INFOS)?.toList().requireNotNull()
    private val viewModel: PlayerViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = TracksFragmentBinding.inflate(inflater, container, false)
        val trackOptions = trackInfos.map { trackInfo -> TrackOption.SingleTrack(trackInfo) }
        val hasManuallySetOption = trackInfos.any(TrackInfo::isManuallySet)
        val auto = TrackOption.Auto(
            name = "Auto",
            isSelected = !hasManuallySetOption,
            rendererIndex = trackInfos.first().indices.rendererIndex // They should all be the same
        )

        val adapter = TracksAdapter(listOf(auto) + trackOptions) { trackOption ->
            val action = when (trackOption) {
                is TrackOption.Auto -> TrackInfo.Action.Clear(trackOption.rendererIndex)
                is TrackOption.SingleTrack -> TrackInfo.Action.Set(listOf(trackOption.trackInfo))
            }
            viewModel.handleTrackInfoAction(action)
            dismiss()
        }
        binding.recyclerView.adapter = adapter

        return binding.root
    }

    companion object {
        private const val KEY_ARG_TRACK_INFOS = "track_infos"

        fun args(trackInfos: List<TrackInfo>): Bundle {
            return bundleOf(KEY_ARG_TRACK_INFOS to trackInfos)
        }
    }
}
