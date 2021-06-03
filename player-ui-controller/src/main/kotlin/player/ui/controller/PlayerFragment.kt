package player.ui.controller

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
import player.common.ui.OnUserLeaveHintViewModel
import player.common.ui.PlayerViewWrapper
import player.common.ui.EnterPipResult
import player.common.ui.PipEvent
import player.common.ui.PlaybackUi
import player.common.ui.PlayerArguments
import player.common.ui.SharedDependencies
import player.common.ui.toPlayerArguments
import player.ui.controller.databinding.PlayerFragmentBinding

class PlayerFragment(
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val deps: SharedDependencies,
    private val errorRenderer: ErrorRenderer
) : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels { vmFactory.create(this, playerArguments.uri) }
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
        val onBackPressed = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (deps.navigator.pop()) {
                    return
                }

                fun yield() {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }

                val pipConfig = playerArguments.pipConfig
                val pipOnBackPress = pipConfig?.enabled == true && pipConfig.onBackPresses
                if (pipOnBackPress) {
                    val result = enterPip()
                    if (result == EnterPipResult.DidNotEnterPip) {
                        yield()
                    }
                } else {
                    yield()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressed)
    }

    private fun enterPip(): EnterPipResult {
        return deps.pipController.enterPip(playerViewModel.isPlaying())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PlayerFragmentBinding.bind(view)
        playerViewWrapper = playerViewWrapperFactory.create(view.context)
        binding.playerContainer.addView(requirePlayerViewWrapper().view)
        playbackUi = playerArguments.playbackUiFactory.newInstance()
            .create(deps, playerViewModel, playerArguments)
        binding.playbackUi.addView(requirePlaybackUi().view)

        listenToPlayer(binding)
    }

    private fun listenToPlayer(binding: PlayerFragmentBinding) {
        playerViewModel.playerEvents()
            .onEach { playerEvent ->
                requirePlayerViewWrapper().onEvent(playerEvent)
                requirePlaybackUi().onPlayerEvent(playerEvent)
                deps.pipController.onEvent(playerEvent)
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

        playerViewModel.playbackInfos
            .onEach { playbackInfos -> requirePlaybackUi().onPlaybackInfos(playbackInfos) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        if (playerArguments.pipConfig?.enabled == true) {
            deps.pipController.events()
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
