package nick.sample.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import nick.sample.R
import nick.sample.configuration.SlowPlaybackInfoResolver
import nick.sample.databinding.MainActivityBinding
import nick.sample.ui.list.ListFragment
import player.ui.common.OnUserLeaveHintViewModel
import player.ui.common.PictureInPictureConfig
import player.ui.common.PlayerArguments
import player.ui.common.toBundle
import player.ui.def.DefaultPlaybackUi
import player.ui.inline.InlinePlaybackUi
import player.ui.sve.SvePlaybackUi

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<OnUserLeaveHintViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.libraryView.initialize(LibraryConfigurationFactory().create(this))

        binding.toPlayerView.setOnClickListener { view ->
            val playerArguments = createPlayerArguments(binding, view)
            binding.libraryView.play(playerArguments)
        }

        binding.toPlayerFragment.setOnClickListener { view ->
            val playerArguments = createPlayerArguments(binding, view)
            supportFragmentManager.commit {
                replace(android.R.id.content, PlayerFragment.create(playerArguments))
                addToBackStack(null)
            }
        }

        binding.toPlayerActivity.setOnClickListener { view ->
            val playerArguments = createPlayerArguments(binding, view)
            PlayerActivity.start(
                context = this,
                playerArguments = playerArguments
            )
        }

        binding.list.setOnClickListener {
            supportFragmentManager.commit {
                replace(R.id.constraint_layout, ListFragment())
                addToBackStack(null)
            }
        }

        binding.inline.setOnClickListener { view ->
            val playerArguments = createPlayerArguments(binding, view)
            supportFragmentManager.commit {
                replace(R.id.movable_container, PlayerFragment::class.java, playerArguments.toBundle())
            }
        }
    }

    private fun createPlayerArguments(binding: MainActivityBinding, clicked: View): PlayerArguments {
        return PlayerArguments(
            id = "id",
            uri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            pipConfig = PictureInPictureConfig(
                enabled = binding.enablePip.isChecked,
                onBackPresses = true
            ),
            playbackUiFactory = when {
                clicked.id == R.id.inline -> InlinePlaybackUi.Factory::class.java
                binding.defaultUi.isChecked -> DefaultPlaybackUi.Factory::class.java
                binding.sveUi.isChecked -> SvePlaybackUi.Factory::class.java
                else -> error("Unknown state")
            }
        )
    }

    override fun onUserLeaveHint() {
        viewModel.onUserLeaveHint()
    }
}
