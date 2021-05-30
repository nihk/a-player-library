package player.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.OnUserLeaveHintViewModel
import player.common.PlaybackInfo
import player.common.PlayerArguments
import player.common.PlayerEvent
import player.common.PlayerViewWrapper
import player.common.SeekData
import player.common.ShareDelegate
import player.common.TimeFormatter
import player.common.TrackInfo
import player.common.toPlayerArguments
import player.ui.databinding.PlayerFragmentBinding
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// todo: show controller before player is loaded?
// todo: this is currently only using the default playback UI. needs to be more like a composable factory pattern.
//  --> Factory will own the layout ID
// todo: can UI be extracted to its own class? this fragment is getting bloated
// fixme: any way to not have 3 calls to setPlayPause?
class PlayerFragment(
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val shareDelegate: ShareDelegate?,
    private val pipController: PipController,
    private val errorRenderer: ErrorRenderer,
    private val navigator: Navigator,
    private val timeFormatter: TimeFormatter
) : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels { vmFactory.create(this, playerArguments.uri) }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by activityViewModels()
    private var playerViewWrapper: PlayerViewWrapper? = null
    private val playerArguments: PlayerArguments get() = requireArguments().toPlayerArguments()
    private val seekBarListener = SeekBarListener { playerViewModel.seekTo(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(TracksPickerFragment.KEY_PICK_RESULT) { _, bundle ->
            val action = TracksPickerFragment.getTrackInfoAction(bundle)
            playerViewModel.handleTrackInfoAction(action)
        }

        setUpBackPressHandling()
    }

    private fun setUpBackPressHandling() {
        val pipConfig = playerArguments.pipConfig
        val pipOnBackPress = pipConfig?.enabled == true && pipConfig.onBackPresses
        if (!pipOnBackPress) return

        val onBackPressed = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val result = enterPip()
                if (result == EnterPipResult.DidNotEnterPip) {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressed)
    }

    private fun enterPip(): EnterPipResult {
        return pipController.enterPip(playerViewModel.isPlaying())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PlayerFragmentBinding.bind(view)
        playerViewWrapper = playerViewWrapperFactory.create(view.context)
        binding.playerContainer.addView(requirePlayerViewWrapper().view)

        bindControls(binding)
        listenToPlayer(binding)
    }

    private fun bindControls(binding: PlayerFragmentBinding) {
        shareDelegate?.run {
            binding.share.apply {
                isVisible = true
                setOnClickListener {
                    share(requireActivity(), playerArguments.uri)
                }
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(seekBarListener)
        binding.playPause.apply {
            setPlayPause(
                imageView = this,
                isPlaying = playerViewModel.isPlaying()
            )
            setOnClickListener {
                if (playerViewModel.isPlaying()) {
                    playerViewModel.pause()
                } else {
                    playerViewModel.play()
                }
            }
        }
        binding.seekBackward.setOnClickListener {
            val amount = -playerArguments.seekConfiguration.backwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerViewModel.seekRelative(amount)
        }
        binding.seekForward.setOnClickListener {
            val amount = playerArguments.seekConfiguration.forwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerViewModel.seekRelative(amount)
        }
    }

    private fun listenToPlayer(binding: PlayerFragmentBinding) {
        playerViewModel.playerEvents()
            .onEach { playerEvent ->
                requirePlayerViewWrapper().onEvent(playerEvent)
                pipController.onEvent(playerEvent)
                when (playerEvent) {
                    is PlayerEvent.Initial -> {
                        setPlayPause(
                            imageView = binding.playPause,
                            isPlaying = playerViewModel.isPlaying()
                        )
                    }
                    is PlayerEvent.OnIsPlayingChanged -> {
                        setPlayPause(
                            imageView = binding.playPause,
                            isPlaying = playerEvent.isPlaying
                        )
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.uiStates()
            .onEach { uiState ->
                binding.playerController.isVisible = uiState.showController && !pipController.isInPip()
                binding.progressBar.isVisible = uiState.showLoading
                if (!seekBarListener.isSeekBarBeingTouched) {
                    updateSeekData(binding, uiState.seekData)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.tracksStates()
            .onEach { tracksState ->
                if (tracksState is TracksState.Available) {
                    bindTracksToPicker(binding, tracksState)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.errors()
            .onEach { message -> errorRenderer.render(requireView(), message) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        if (playerArguments.pipConfig?.enabled == true) {
            pipController.events()
                .onEach { pipAction ->
                    when (pipAction) {
                        PipEvent.Pause -> playerViewModel.pause()
                        PipEvent.Play -> playerViewModel.play()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

            onUserLeaveHintViewModel.onUserLeaveHints()
                .onEach { enterPip() }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }

        playerViewModel.playbackInfos
            .onEach { playbackInfos ->
                playbackInfos.forEach { playbackInfo ->
                    when (playbackInfo) {
                        is PlaybackInfo.MediaTitle -> {
                            binding.title.apply {
                                text = playbackInfo.title
                                isVisible = true
                            }
                        }
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateSeekData(
        binding: PlayerFragmentBinding,
        seekData: SeekData
    ) {
        binding.seekBar.apply {
            max = seekData.duration.inWholeSeconds.toInt()
            progress = seekData.position.inWholeSeconds.toInt()
            secondaryProgress = seekData.buffered.inWholeSeconds.toInt()
        }
        binding.position.text = timeFormatter.playerTime(seekData.position)
        binding.remaining.text = timeFormatter.playerTime(seekData.duration - seekData.position)
        // todo: content descriptions
    }

    private fun setPlayPause(
        imageView: ImageView,
        isPlaying: Boolean
    ) {
        val resource = if (isPlaying) {
            R.drawable.pause
        } else {
            R.drawable.play
        }
        imageView.setImageResource(resource)
    }

    private fun bindTracksToPicker(binding: PlayerFragmentBinding, available: TracksState.Available) {
        val typesToBind = mapOf(
            binding.videoTracks to TrackInfo.Type.VIDEO,
            binding.audioTracks to TrackInfo.Type.AUDIO,
            binding.textTracks to TrackInfo.Type.TEXT
        )
        typesToBind.forEach { entry ->
            if (entry.value in available.trackTypes) {
                entry.key.apply {
                    isVisible = true
                    setOnClickListener {
                        navigateToTracksPicker(playerViewModel.tracks().filter { it.type == entry.value })
                    }
                }
            }
        }
    }

    private fun navigateToTracksPicker(trackInfos: List<TrackInfo>) {
        navigator.toDialog(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos))
    }

    override fun onStart() {
        super.onStart()
        val appPlayer = playerViewModel.getPlayer()
        requirePlayerViewWrapper().attach(appPlayer)
    }

    override fun onStop() {
        super.onStop()
        requirePlayerViewWrapper().detachPlayer()
        if (!isChangingConfigurations) {
            playerViewModel.onAppBackgrounded()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        playerViewModel.onPipModeChanged(isInPictureInPictureMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerViewWrapper = null
    }

    private fun requirePlayerViewWrapper() = requireNotNull(playerViewWrapper)

    private val isChangingConfigurations get() = requireActivity().isChangingConfigurations
}