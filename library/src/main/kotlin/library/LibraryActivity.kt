package library

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import library.ui.Constants
import library.common.OnUserLeaveHintViewModel
import library.common.requireNotNull

class LibraryActivity : AppCompatActivity(R.layout.library_activity) {

    private val onUserLeaveHintViewModel: OnUserLeaveHintViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val url = intent.getStringExtra(Constants.KEY_URL).requireNotNull()
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LibraryFragment.create(url))
                .commit()
        }
    }

    override fun onUserLeaveHint() {
        onUserLeaveHintViewModel.onUserLeaveHint()
    }

    companion object {
        fun start(context: Context, url: String) {
            val intent = Intent(context, LibraryActivity::class.java)
                .putExtra(Constants.KEY_URL, url)
            context.startActivity(intent)
        }
    }
}