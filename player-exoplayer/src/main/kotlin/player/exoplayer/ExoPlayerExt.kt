package player.exoplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TrackNameProvider
import player.common.TrackInfo

internal val KNOWN_TRACK_TYPES = listOf(C.TRACK_TYPE_TEXT, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_VIDEO)

internal val ExoPlayer.defaultTrackSelector: DefaultTrackSelector
    get() = trackSelector as DefaultTrackSelector

internal fun ExoPlayer.setTrackInfo(trackInfo: TrackInfo) {
    val indices = trackInfo.indices
    val trackGroupArray = defaultTrackSelector.currentMappedTrackInfo
        ?.getTrackGroups(indices.rendererIndex)
        ?: error("Can't find track group array for renderer index ${indices.rendererIndex}")

    defaultTrackSelector.parameters = defaultTrackSelector.parameters.buildUpon()
        .clearSelectionOverrides(indices.rendererIndex)
        .setRendererDisabled(indices.rendererIndex, !trackInfo.isSelected)
        .setSelectionOverride(
            indices.rendererIndex,
            trackGroupArray,
            DefaultTrackSelector.SelectionOverride(indices.groupIndex, indices.index)
        )
        .build()
}

internal fun ExoPlayer.clearTrackOverrides(rendererIndex: Int) {
    defaultTrackSelector.parameters = defaultTrackSelector.parameters.buildUpon()
        .clearSelectionOverrides(rendererIndex)
        .build()
}

// MappedTrackInfo[rendererIndex] > TrackGroupArray[groupIndex] > TrackGroup[trackIndex] > Format
internal fun ExoPlayer.getTrackInfos(trackTypes: List<Int>, trackNameProvider: TrackNameProvider? = null): List<TrackInfo> {
    val mappedTrackInfo = defaultTrackSelector.currentMappedTrackInfo
        ?: return emptyList()
    val parameters = defaultTrackSelector.parameters
    val trackInfos = mutableListOf<TrackInfo>()

    for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
        val trackType = mappedTrackInfo.getRendererType(rendererIndex)
        if (trackType in trackTypes) {
            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
            val selectionOverride = parameters.getSelectionOverride(rendererIndex, trackGroupArray)
            val trackSelectionArray = currentTrackSelections
            val trackSelection = trackSelectionArray.get(rendererIndex)
            for (groupIndex in 0 until trackGroupArray.length) {
                val trackGroup = trackGroupArray.get(groupIndex)
                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    if (mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex) == C.FORMAT_HANDLED) {
                        val name = trackNameProvider?.getTrackName(format) ?: format.label
                        val isTrackSelected = trackSelection?.indexOf(format) != C.INDEX_UNSET
                        val isOverridden = selectionOverride?.containsTrack(trackIndex) == true
                                && selectionOverride.groupIndex == groupIndex
                        val isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0
                        val isAutoSelected = format.selectionFlags and C.SELECTION_FLAG_AUTOSELECT != 0
                        trackInfos.add(
                            TrackInfo(
                                name = name,
                                type = trackType.toTrackInfoType(),
                                indices = TrackInfo.Indices(
                                    index = trackIndex,
                                    groupIndex = groupIndex,
                                    rendererIndex = rendererIndex
                                ),
                                isDefault = isDefault,
                                isSelected = isTrackSelected,
                                isAutoSelected = isAutoSelected,
                                isManuallySet = isOverridden
                            )
                        )
                    }
                }
            }
        }
    }

    return trackInfos
}

private fun Int.toTrackInfoType(): TrackInfo.Type {
    return when (this) {
        C.TRACK_TYPE_VIDEO -> TrackInfo.Type.VIDEO
        C.TRACK_TYPE_AUDIO -> TrackInfo.Type.AUDIO
        C.TRACK_TYPE_TEXT -> TrackInfo.Type.TEXT
        else -> error("Unknown ExoPlayer track type: $this")
    }
}
