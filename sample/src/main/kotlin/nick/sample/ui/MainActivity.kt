package nick.sample.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import nick.sample.databinding.MainActivityBinding
import player.core.LibraryActivity
import player.core.LibraryFragment
import player.ui.def.DefaultPlaybackUi
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PictureInPictureConfig
import player.ui.common.PlayerArguments
import player.ui.common.toBundle
import player.ui.sve.SvePlaybackUi

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<OnUserLeaveHintViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toPlayerFragment.setOnClickListener {
            val playerArguments = createPlayerArguments(binding)
            supportFragmentManager.commit {
                replace(android.R.id.content, LibraryFragment::class.java, playerArguments.toBundle())
                addToBackStack(null)
            }
        }

        binding.toPlayerActivity.setOnClickListener {
            val playerArguments = createPlayerArguments(binding)
            LibraryActivity.start(
                context = this,
                playerArguments = playerArguments
            )
        }
    }

    private fun createPlayerArguments(binding: MainActivityBinding): PlayerArguments {
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

    override fun onUserLeaveHint() {
        viewModel.onUserLeaveHint()
    }
}
