package library.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import library.common.OnUserLeaveHintViewModel
import library.common.PlayerArguments
import library.common.PlayerViewWrapper
import library.common.ShareDelegate
import library.common.TrackInfo
import library.common.toPlayerArguments
import library.ui.databinding.PlayerFragmentBinding

class PlayerFragment(
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val shareDelegate: ShareDelegate?,
    private val pipController: PipController,
    private val errorRenderer: ErrorRenderer
) : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels { vmFactory.create(this) }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by activityViewModels()
    private var playerViewWrapper: PlayerViewWrapper? = null
    private val playerArguments: PlayerArguments get() = requireArguments().toPlayerArguments()

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
                enterPip {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressed)
    }

    private fun enterPip(onFailedToEnterPip: () -> Unit = {}) {
        val result = pipController.enterPip(playerViewModel.isPlaying())
        if (result == EnterPipResult.DidNotEnterPip) {
            onFailedToEnterPip()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PlayerFragmentBinding.bind(view)
        val playerViewWrapper = playerViewWrapperFactory.create(view.context)
        binding.container.addView(playerViewWrapper.view)

        shareDelegate?.run {
            playerViewWrapper.bindShare { share(requireActivity(), playerArguments.uri) }
        }

        playerViewWrapper.bindPlay { playerViewModel.play() }
        playerViewWrapper.bindPause { playerViewModel.pause() }

        playerViewModel.playerEvents()
            .onEach { playerEvent -> playerViewWrapper.onEvent(playerEvent) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.uiStates()
            .onEach { uiState ->
                if (uiState.tracksState == TracksState.Available) {
                    if (playerViewModel.textTracks().isNotEmpty()) {
                        playerViewWrapper.bindTextTracksPicker { navigateToTracksPicker(playerViewModel.textTracks()) }
                    }
                    if (playerViewModel.audioTracks().isNotEmpty()) {
                        playerViewWrapper.bindAudioTracksPicker { navigateToTracksPicker(playerViewModel.audioTracks()) }
                    }
                    if (playerViewModel.videoTracks().isNotEmpty()) {
                        playerViewWrapper.bindVideoTracksPicker { navigateToTracksPicker(playerViewModel.videoTracks()) }
                    }
                }

                playerViewWrapper.setControllerUsability(uiState.useController)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.errors()
            .onEach { message -> errorRenderer.render(view, message) }
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

        this.playerViewWrapper = playerViewWrapper
    }

    private fun navigateToTracksPicker(trackInfos: List<TrackInfo>) {
        parentFragmentManager.beginTransaction()
            .add(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos), null)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.bind(requireNotNull(playerViewWrapper), playerArguments.uri)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        playerViewModel.unbind(requireNotNull(playerViewWrapper), requireActivity().isChangingConfigurations)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        playerViewModel.onPipModeChanged(isInPictureInPictureMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerViewWrapper = null
    }
}