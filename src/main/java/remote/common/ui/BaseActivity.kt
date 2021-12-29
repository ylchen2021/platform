package remote.common.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity: AppCompatActivity() {
    protected abstract fun getContentRes(): Int
    private var animInEnter: Int? = null
    private var animInExit: Int? = null
    private var animOutEnter: Int? = null
    private var animOutExit: Int? = null

    private var resultHandler: ((Int, Int, Intent?)->Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (animInEnter != null && animInExit != null) {
            overridePendingTransition(animInEnter?:0, animInExit?:0)
        }
        setContentView(getContentRes())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultHandler?.invoke(requestCode, resultCode, data)
    }

    protected fun initAnimations(inEnter: Int, inExit: Int, outEnter: Int, outExit: Int) {
        animInEnter = inEnter
        animInExit = inExit
        animOutEnter = outEnter
        animOutExit = outExit
    }

    override fun finish() {
        super.finish()
        if (animOutEnter != null && animOutExit != null) {
            overridePendingTransition(animOutEnter?:0, animOutExit?:0)
        }
    }

    fun setOnActivityResultHandler(handler: (requestCode: Int, resultCode: Int, data: Intent?)->Unit) {
        resultHandler = handler
    }
}