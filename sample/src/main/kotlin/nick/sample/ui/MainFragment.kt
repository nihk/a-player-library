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
            },
            links = listOf(
                PlayerArguments.Link(
                    uri = "https://multiplatform-f.akamaihd.net/i/multi/april11/cctv/cctv_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8",
                    imageUri = "https://i.imgur.com/MYmm7E1.jpg",
                    durationMillis = TimeUnit.SECONDS.toMillis(141L)
                ),
                PlayerArguments.Link(
                    uri = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4",
                    imageUri = "https://i.imgur.com/ecnSVVk.jpg",
                    durationMillis = TimeUnit.SECONDS.toMillis(43L)
                ),
                PlayerArguments.Link(
                    uri = "https://bestvpn.org/html5demos/assets/dizzy.mp4",
                    imageUri = "https://i.imgur.com/cB7oqeF.jpeg",
                    durationMillis = TimeUnit.SECONDS.toMillis(55L)
                ),
                PlayerArguments.Link(
                    uri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
                    imageUri = "https://i.imgur.com/9OPnZNk.png",
                    durationMillis = TimeUnit.SECONDS.toMillis(13L)
                ),
            )
        )
    }
}