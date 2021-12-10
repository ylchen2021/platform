package remote.common.utils

import android.util.Log

object Logger {
    fun d(tag: String, msg: String) {
        if (AppUtils.isDebug) {
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