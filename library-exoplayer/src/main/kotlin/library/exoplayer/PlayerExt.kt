package library.exoplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TrackNameProvider
import library.common.TrackInfo

internal val Player.defaultTrackSelector: DefaultTrackSelector
    get() = trackSelector as DefaultTrackSelector

internal fun Player.setTrackInfo(trackInfo: TrackInfo) {
    val trackGroupArray = defaultTrackSelector.currentMappedTrackInfo
        ?.getTrackGroups(trackInfo.rendererIndex)
        ?: error("Can't find track group array for renderer index ${trackInfo.rendererIndex}")

    defaultTrackSelector.parameters = defaultTrackSelector.parameters.buildUpon()
        .clearSelectionOverrides(trackInfo.rendererIndex)
        .setRendererDisabled(trackInfo.rendererIndex, !trackInfo.isSelected)
        .setSelectionOverride(
            trackInfo.rendererIndex,
            trackGroupArray,
            DefaultTrackSelector.SelectionOverride(trackInfo.groupIndex, trackInfo.index)
        )
        .build()
}

internal fun Player.clearTrackOverrides(rendererIndex: Int) {
    defaultTrackSelector.parameters = defaultTrackSelector.parameters.buildUpon()
        .clearSelectionOverrides(rendererIndex)
        .build()
}

// MappedTrackInfo[rendererIndex] > TrackGroupArray[groupIndex] > TrackGroup[trackIndex] > Format
internal fun Player.getTrackInfos(trackTypes: List<Int>, trackNameProvider: TrackNameProvider): List<TrackInfo> {
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
                    if (mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex) == RendererCapabilities.FORMAT_HANDLED) {
                        val name = trackNameProvider.getTrackName(format)
                        val isTrackSelected = trackSelection?.indexOf(format) != C.INDEX_UNSET
                        val isOverridden = selectionOverride?.containsTrack(trackIndex) == true
                                && selectionOverride.groupIndex == groupIndex
                        val isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0
                        val isAutoSelected = format.selectionFlags and C.SELECTION_FLAG_AUTOSELECT != 0
                        trackInfos.add(
                            TrackInfo(
                                name = name,
                                type = trackType.toTrackInfoType(),
                                index = trackIndex,
                                groupIndex = groupIndex,
                                rendererIndex = rendererIndex,
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
