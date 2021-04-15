package library

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import library.ui.Constants
import library.common.OnUserLeaveHintViewModel
import library.common.PictureInPictureConfig
import library.common.requireNotNull

class LibraryActivity : AppCompatActivity(R.layout.library_activity) {

    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val url = intent.getStringExtra(Constants.KEY_URL).requireNotNull()
            val pipConfig = intent.getParcelableExtra<PictureInPictureConfig>(Constants.KEY_PIP_CONFIG)
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LibraryFragment.create(url, pipConfig))
                .commit()
        }
    }

    override fun onUserLeaveHint() {
        onUserLeaveHintViewModel.onUserLeaveHint()
    }

    companion object {
        fun start(
            context: Context,
            url: String,
            pictureInPictureConfig: PictureInPictureConfig? = null
        ) {
            val intent = Intent(context, LibraryActivity::class.java)
                .putExtra(Constants.KEY_URL, url)
                .putExtra(Constants.KEY_PIP_CONFIG, pictureInPictureConfig)
            context.startActivity(intent)
        }
    }
}