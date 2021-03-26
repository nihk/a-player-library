package library.ui

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import library.common.OnUserLeaveHintViewModel
import library.common.PictureInPictureConfig
import library.common.PlayerViewWrapper
import library.common.ShareDelegate
import library.common.TrackInfo
import library.common.requireNotNull
import library.ui.databinding.PlayerFragmentBinding

class PlayerFragment(
    private val vmFactory: PlayerViewModel.Factory,
    private val playerViewWrapperFactory: PlayerViewWrapper.Factory,
    private val shareDelegate: ShareDelegate?,
    private val pictureInPictureConfig: PictureInPictureConfig?
) : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels { vmFactory.create(this) }
    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by activityViewModels()
    private var playerViewWrapper: PlayerViewWrapper? = null
    private val url: String get() = requireArguments().getString(Constants.KEY_URL).requireNotNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(TracksPickerFragment.KEY_PICK_RESULT) { _, bundle ->
            val action = TracksPickerFragment.getTrackInfoAction(bundle)
            playerViewModel.handleTrackInfoAction(action)
        }

        val enterPipOnBackPresses = pictureInPictureConfig?.onBackPresses == true
        val onBackPressed = object : OnBackPressedCallback(enterPipOnBackPresses) {
            override fun handleOnBackPressed() {
                enterPip { abort() }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressed)
    }

    private fun enterPip(onFailedToEnterPip: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onFailedToEnterPip()
            return
        }

        // Didn't find any better way to check at runtime for PIP Activity flag set
        try {
            requireActivity().enterPictureInPictureMode(
                PictureInPictureParams
                    .Builder()
                    .build()
            )
        } catch (throwable: Throwable) {
            onFailedToEnterPip()
        }
    }

    private fun OnBackPressedCallback.abort() {
        isEnabled = false
        requireActivity().onBackPressed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = PlayerFragmentBinding.bind(view)
        val playerViewWrapper = playerViewWrapperFactory.create(view.context)
        binding.container.addView(playerViewWrapper.view)

        shareDelegate?.run {
            playerViewWrapper.bindShare { share(requireActivity(), url) }
        }

        playerViewWrapper.bindPlay { playerViewModel.play() }
        playerViewWrapper.bindPause { playerViewModel.pause() }

        playerViewModel.playerEvents()
            .onEach { playerEvent -> playerViewWrapper.onEvent(playerEvent) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.tracksStates()
            .filter { tracksState -> tracksState == TracksState.Available }
            .onEach {
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
            .launchIn(viewLifecycleOwner.lifecycleScope)

        playerViewModel.errors()
            .onEach { message -> Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        onUserLeaveHintViewModel.onUserLeaveHints()
            .onEach { if (pictureInPictureConfig?.onUserLeaveHints == true) enterPip() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        this.playerViewWrapper = playerViewWrapper
    }

    private fun navigateToTracksPicker(trackInfos: List<TrackInfo>) {
        parentFragmentManager.beginTransaction()
            .add(TracksPickerFragment::class.java, TracksPickerFragment.args(trackInfos), null)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.bind(requireNotNull(playerViewWrapper), url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        playerViewModel.unbind(requireNotNull(playerViewWrapper), requireActivity().isChangingConfigurations)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerViewWrapper = null
    }

    companion object {
        fun args(url: String): Bundle {
            return bundleOf(Constants.KEY_URL to url)
        }
    }
}