package player.ui.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import player.common.TrackInfo
import player.common.requireNotNull
import player.ui.databinding.TrackItemBinding
import player.ui.databinding.TracksFragmentBinding

// fixme: if tracks update while this Fragment is open, this Fragment doesn't get updated.
//  do not use an activity scoped PlayerViewModel for this (VM/player instance should always
//  be scoped to PlayerFragment). use [TracksBroadcaster].
class TracksPickerFragment : BottomSheetDialogFragment() {

    private val trackInfos: List<TrackInfo>
        get() = requireArguments().getParcelableArrayList<TrackInfo>(KEY_ARG_TRACK_INFOS)?.toList().requireNotNull()

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
            when (trackOption) {
                is TrackOption.Auto -> setFragmentResult(
                    KEY_PICK_RESULT, bundleOf(
                        KEY_RENDERER_INDEX to trackOption.rendererIndex))
                is TrackOption.SingleTrack -> setFragmentResult(
                    KEY_PICK_RESULT, bundleOf(
                        KEY_TRACK_INFO to trackOption.trackInfo))
            }

            dismiss()
        }
        binding.recyclerView.adapter = adapter

        return binding.root
    }

    companion object {
        const val KEY_PICK_RESULT = "pick_result"
        private const val KEY_TRACK_INFO = "track_info"
        private const val KEY_RENDERER_INDEX = "renderer_index"
        private const val KEY_ARG_TRACK_INFOS = "track_infos"

        fun args(trackInfos: List<TrackInfo>): Bundle {
            return bundleOf(KEY_ARG_TRACK_INFOS to trackInfos)
        }

        fun getTrackInfoAction(bundle: Bundle): TrackInfo.Action {
            val trackInfo: TrackInfo? = bundle.getParcelable(KEY_TRACK_INFO)
            return if (trackInfo != null) {
                TrackInfo.Action.Set(trackInfos = listOf(trackInfo))
            } else {
                val rendererIndex = bundle.getInt(KEY_RENDERER_INDEX)
                TrackInfo.Action.Clear(rendererIndex)
            }
        }
    }
}

class TracksAdapter(
    private val trackOptions: List<TrackOption>,
    private val callback: (TrackOption) -> Unit
) : RecyclerView.Adapter<TrackViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return LayoutInflater.from(parent.context)
            .let { inflater -> TrackItemBinding.inflate(inflater, parent, false) }
            .let { binding -> TrackViewHolder(binding) }
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(trackOptions[position], callback)
    }

    override fun getItemCount(): Int {
        return trackOptions.size
    }
}

class TrackViewHolder(private val binding: TrackItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(trackOption: TrackOption, callback: (TrackOption) -> Unit) {
        binding.name.text = when (trackOption) {
            is TrackOption.Auto -> trackOption.name
            is TrackOption.SingleTrack -> trackOption.trackInfo.name
        }
        binding.check.isVisible = when (trackOption) {
            is TrackOption.Auto -> trackOption.isSelected
            is TrackOption.SingleTrack -> trackOption.trackInfo.isManuallySet
        }
        binding.container.setOnClickListener {
            val selected = when (trackOption) {
                is TrackOption.Auto -> trackOption.copy(isSelected = true)
                is TrackOption.SingleTrack -> trackOption.copy(trackInfo = trackOption.trackInfo.copy(isSelected = true))
            }
            callback(selected)
        }
    }
}

sealed class TrackOption {
    data class Auto(
        val name: String,
        val isSelected: Boolean,
        val rendererIndex: Int
    ) : TrackOption()

    data class SingleTrack(val trackInfo: TrackInfo) : TrackOption()
}