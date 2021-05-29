package library.ui

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
import kotlinx.coroutines.launch
import library.common.OnUserLeaveHintViewModel
import library.common.PlaybackUiFactory
import library.common.PlayerArguments
import library.common.PlayerEvent
import library.common.PlayerViewWrapper
import library.common.SeekData
import library.common.ShareDelegate
import library.common.TimeFormatter
import library.common.TrackInfo
import library.common.toPlayerArguments
import library.ui.databinding.PlayerFragmentBinding
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
        if (playerArguments.pipConfig?.onBackPresses != true) return

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

        shareDelegate?.run {
            // todo
//            playerViewWrapper.bindShare { share(requireActivity(), playerArguments.uri) }
        }

        bindControls(binding)
        launchFlows(binding)
    }

    private fun bindControls(binding: PlayerFragmentBinding) {
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
            val amount = playerArguments.seekConfiguration.backwardAmount.toDuration(DurationUnit.MILLISECONDS)
            playerViewModel.seekRelative(amount)
        }
    }

    private fun launchFlows(binding: PlayerFragmentBinding) {
        playerViewModel.playerEvents()
            .onEach { playerEvent ->
                requirePlayerViewWrapper().onEvent(playerEvent)
                pipController.onEvent(playerEvent)
                when (playerEvent) {
                    is PlayerEvent.Initial -> {
                        setPlayPause(
                            imageView = binding.playPause,
                            isPlaying = playerEvent.playerState.isPlaying
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
                // todo: port over OP progress bar loading UI
//                playerViewWrapper.setLoading(uiState.isResolvingMedia)
                if (!seekBarListener.isSeekBarBeingTouched) {
                    updateSeekData(binding, uiState.seekData)
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.tracksStates()
            .onEach { tracksState ->
                if (tracksState == TracksState.Available) {
                    bindTracksToPicker()
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
    }

    private fun updateSeekData(
        binding: PlayerFragmentBinding,
        seekData: SeekData
    ) {
        binding.seekBar.apply {
            max = seekData.duration.inSeconds.toInt()
            progress = seekData.position.inSeconds.toInt()
            secondaryProgress = seekData.buffered.inSeconds.toInt()
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

    private fun bindTracksToPicker() {
        val typesToBind = listOf(TrackInfo.Type.VIDEO, TrackInfo.Type.AUDIO, TrackInfo.Type.TEXT)
        typesToBind.forEach { type ->
            if (playerViewModel.tracks().any { it.type == type }) {
                // todo: add cc/video/audio track buttons
//                bindTracks(type) { navigateToTracksPicker(playerViewModel.tracks().filter { it.type == type }) }
            }
        }
    }

    private fun navigateToTracksPicker(trackInfos: List<TrackInfo>) {
        navigator.toDialog(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos))
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.bind(requirePlayerViewWrapper())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        playerViewModel.unbind(requirePlayerViewWrapper(), requireActivity().isChangingConfigurations)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        playerViewModel.onPipModeChanged(isInPictureInPictureMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerViewWrapper = null
    }

    private fun requirePlayerViewWrapper() = requireNotNull(playerViewWrapper)
}