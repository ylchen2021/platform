package remote.common.utils

import android.util.Log
import tv.remote.platform.BuildConfig

object Logger {
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    fun v(tag: String?, msg: String) {
        Log.d(tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}