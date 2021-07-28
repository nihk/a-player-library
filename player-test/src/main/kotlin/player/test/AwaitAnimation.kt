package player.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.CoreMatchers.any
import org.hamcrest.Matcher

class AwaitAnimation(
    private val timeout: Long = 100L,
    private val loopInterval: Long = 50L
) : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return any(View::class.java)
    }

    override fun getDescription(): String {
        return "awaiting til animation has finished"
    }

    override fun perform(uiController: UiController, view: View) {
        var startTime = 0L
        var flag = false
        val animator = view.animate()
        val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator?) {
                animator.setListener(null)
                flag = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                animator.setListener(null)
                flag = true
            }
        }
        animator.setListener(listener)
        while (!flag && startTime <= timeout) {
            uiController.loopMainThreadForAtLeast(loopInterval)
            startTime += loopInterval
        }
    }
}
