package remote.common.ui

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity: AppCompatActivity() {
    protected abstract fun getContentRes(): Int

    private var resultHandler: ((Int, Int, Intent?)->Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentRes())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultHandler?.invoke(requestCode, resultCode, data)
    }

    fun setOnActivityResultHandler(handler: (Int, Int, Intent?)->Unit) {
        resultHandler = handler
    }
}