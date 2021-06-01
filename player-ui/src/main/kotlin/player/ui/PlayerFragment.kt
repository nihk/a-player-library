package player.ui

import android.os.Bundle
import android.view.View
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
import player.common.PlayerArguments
import player.common.PlayerViewWrapper
import player.common.toPlayerArguments
import player.ui.databinding.PlayerFragmentBinding
import player.ui.playbackui.PlaybackUi

class PlayerFragment(
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val pipController: PipController,
    private val errorRenderer: ErrorRenderer,
    private val playbackUiFactory: PlaybackUi.Factory
) : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels { vmFactory.create(this, playerArguments.mainUri) }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by activityViewModels()
    private var playerViewWrapper: PlayerViewWrapper? = null
    private var playbackUi: PlaybackUi? = null
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
        playbackUi = playbackUiFactory.create(playerViewModel)
        binding.playbackUi.addView(requirePlaybackUi().view)

        listenToPlayer(binding)
    }

    private fun listenToPlayer(binding: PlayerFragmentBinding) {
        playerViewModel.playerEvents()
            .onEach { playerEvent ->
                requirePlayerViewWrapper().onEvent(playerEvent)
                requirePlaybackUi().onPlayerEvent(playerEvent)
                pipController.onEvent(playerEvent)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.uiStates()
            .onEach { uiState ->
                binding.progressBar.isVisible = uiState.showLoading
                requirePlaybackUi().onUiState(uiState)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.tracksStates()
            .onEach { tracksState -> requirePlaybackUi().onTracksState(tracksState) }
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

    override fun onStart() {
        super.onStart()
        val appPlayer = playerViewModel.getPlayer()
        requirePlayerViewWrapper().attach(appPlayer)
    }

    override fun onStop() {
        super.onStop()
        requirePlayerViewWrapper().detachPlayer()
        if (!requireActivity().isChangingConfigurations) {
            playerViewModel.onAppBackgrounded()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        playerViewModel.onPipModeChanged(isInPictureInPictureMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerViewWrapper = null
        playbackUi = null
    }

    private fun requirePlayerViewWrapper() = requireNotNull(playerViewWrapper)
    private fun requirePlaybackUi() = requireNotNull(playbackUi)
}