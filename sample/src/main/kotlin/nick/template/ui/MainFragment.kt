package nick.template.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import library.core.LibraryActivity
import library.common.PictureInPictureConfig
import library.common.PlayerArguments
import library.common.bundle
import nick.template.R
import nick.template.databinding.MainFragmentBinding
import nick.template.navigation.AppNavigation
import javax.inject.Inject

class MainFragment @Inject constructor(
    private val navController: NavController
) : Fragment(R.layout.main_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)

        binding.toPlayerFragment.setOnClickListener {
            val playerArguments = createPlayerArguments(binding.enablePip.isChecked)
            navController.navigate(AppNavigation.library, playerArguments.bundle())
        }

        binding.toPlayerActivity.setOnClickListener {
            val playerArguments = createPlayerArguments(binding.enablePip.isChecked)
            LibraryActivity.start(
                context = view.context,
                playerArguments = playerArguments
            )
        }
    }

    private fun createPlayerArguments(isPipEnabled: Boolean): PlayerArguments {
        return PlayerArguments(
            uri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            pipConfig = PictureInPictureConfig(
                enabled = isPipEnabled,
                onBackPresses = true
            )
        )
    }
}