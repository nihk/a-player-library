package player.ui.def

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {
    private val root by lazy { FrameLayout(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(root)
    }

    fun attach(view: View) {
        root.addView(view)
    }
}
