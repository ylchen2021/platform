package remote.common.utils

import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE

object AppUtils {
    var isDebug = false

    fun init(context: Context) {
        isDebug = context.applicationInfo.flags and FLAG_DEBUGGABLE !== 0
    }
}