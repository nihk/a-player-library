package nick.sample.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import player.core.LibraryActivity
import player.ui.shared.PictureInPictureConfig
import nick.sample.R
import nick.sample.databinding.MainFragmentBinding
import nick.sample.navigation.AppNavigation
import player.ui.def.DefaultPlaybackUi
import player.ui.shared.PlayerArguments
import player.ui.shared.toBundle
import player.ui.sve.SvePlaybackUi
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainFragment @Inject constructor(
    private val navController: NavController
) : Fragment(R.layout.main_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)

        binding.toPlayerFragment.setOnClickListener {
            val playerArguments = createPlayerArguments(binding)
            navController.navigate(AppNavigation.library, playerArguments.toBundle())
        }

        binding.toPlayerActivity.setOnClickListener {
            val playerArguments = createPlayerArguments(binding)
            LibraryActivity.start(
                context = view.context,
                playerArguments = playerArguments
            )
        }
    }

    private fun createPlayerArguments(binding: MainFragmentBinding): PlayerArguments {
        return PlayerArguments(
            uri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            pipConfig = PictureInPictureConfig(
                enabled = binding.enablePip.isChecked,
                onBackPresses = true
            ),
            playbackUiFactory = if (binding.defaultUi.isChecked) {
                DefaultPlaybackUi.Factory::class.java
            } else {
                SvePlaybackUi.Factory::class.java
            }
        )
    }
}