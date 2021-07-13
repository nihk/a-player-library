package player.ui.controller

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import player.common.PlayerViewWrapper
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PipController
import player.ui.common.PlaybackUi
import player.ui.common.PlayerArguments
import player.ui.common.toPlayerArguments
import player.ui.core.R
import player.ui.core.databinding.PlayerFragmentBinding

// todo: to what extent can i move PipController more into PlaybackUi
class PlayerFragment(
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val errorRenderer: ErrorRenderer,
    private val pipControllerFactory: PipController.Factory,
    private val playbackUiFactories: List<PlaybackUi.Factory>
) : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels { vmFactory.create(this, playerArguments.uri) }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by activityViewModels()
    private var playbackUi: PlaybackUi? = null
    private val playerArguments: PlayerArguments get() = requireArguments().toPlayerArguments()
    private val pipController: PipController by lazy { pipControllerFactory.create(playerViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpBackPressHandling()
    }

    private fun setUpBackPressHandling() {
        val pipConfig = playerArguments.pipConfig
        val pipOnBackPress = pipConfig?.enabled == true && pipConfig.onBackPresses
        if (!pipOnBackPress) return

        val onBackPressed = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val result = enterPip()
                if (result == PipController.Result.DidNotEnterPip) {
                    remove()
                    requireActivity().onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressed)
    }

    private fun enterPip(): PipController.Result {
        return pipController.enterPip()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PlayerFragmentBinding.bind(view)
        val playbackUiFactory = playbackUiFactories.first { factory ->
            playerArguments.playbackUiFactory.isAssignableFrom(factory::class.java)
        }
        playbackUi = playbackUiFactory.create(
            host = requireActivity(),
            playerViewWrapperFactory = playerViewWrapperFactory,
            pipController = pipController,
            playerController = playerViewModel,
            playerArguments = playerArguments,
            registryOwner = this
        )
        binding.playbackUi.addView(requirePlaybackUi().view)

        listenToPlayer()
    }

    private fun listenToPlayer() {
        playerViewModel.playerEvents()
            .onEach { playerEvent ->
                requirePlaybackUi().onPlayerEvent(playerEvent)
                pipController.onEvent(playerEvent)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.uiStates()
            .onEach { uiState -> requirePlaybackUi().onUiState(uiState) }
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
            pipController.events()
                .launchIn(viewLifecycleOwner.lifecycleScope)

            onUserLeaveHintViewModel.onUserLeaveHints()
                .onEach { enterPip() }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }
    }

    override fun onStart() {
        super.onStart()
        val appPlayer = playerViewModel.getPlayer()
        requirePlaybackUi().attach(appPlayer)
    }

    override fun onStop() {
        super.onStop()
        requirePlaybackUi().detachPlayer()
        if (!requireActivity().isChangingConfigurations) {
            playerViewModel.onAppBackgrounded()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        playerViewModel.onPipModeChanged(isInPictureInPictureMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playbackUi = null
    }

    private fun requirePlaybackUi() = requireNotNull(playbackUi)
}
