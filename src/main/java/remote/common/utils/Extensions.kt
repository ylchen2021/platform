package remote.common.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator


fun View.animateVisibility(visible: Boolean, duration: Long = 400) {
    if ((visibility == View.VISIBLE && visible) || (visibility == View.GONE && !visible)) {
        return
    }

    var fromAlpha = if (visible) 0f else 1f
    var toAlpha = if (visible) 1f else 0f
    var toVisibility = if (visible) View.VISIBLE else View.GONE
    var animator = ObjectAnimator.ofFloat(this, "alpha", fromAlpha, toAlpha)
    animator.duration = duration
    animator.addListener(object: AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            visibility = View.VISIBLE
        }

        override fun onAnimationEnd(animation: Animator) {
            visibility = toVisibility
        }
    })
    animator.start()
}

fun View.animatePosition(xDst: Float, yDst: Float, duration: Long, decelerate: Boolean, listenerAdapter: AnimatorListenerAdapter) {
    var animator = ValueAnimator.ofFloat(0f, 1f)
    animator.duration = duration
    if (decelerate) {
        animator.interpolator = DecelerateInterpolator()
    }
    var curX = translationX
    var curY = translationY
    animator.addUpdateListener {
        val percent = it.animatedValue as Float
        translationX = curX + (xDst-curX)*percent
        translationY = curY + (yDst-curY)*percent
    }
    animator.addListener(listenerAdapter)
    animator.start()
}

fun View.setFastOnClickListener(action: (view: View?) -> Unit) {
    setOnClickListener(FastCilickListener(action))
}

private class FastCilickListener(val action: (view: View?) -> Unit) : View.OnClickListener {
    override fun onClick(view: View?) {
        val clickTimeStamp = System.currentTimeMillis()
        if (clickTimeStamp - timeStamp < BLOCKING_OF_TIME) return
        action.invoke(view)
        timeStamp = clickTimeStamp
    }

    companion object {
        var timeStamp = 0L
        const val BLOCKING_OF_TIME = 500
    }
}