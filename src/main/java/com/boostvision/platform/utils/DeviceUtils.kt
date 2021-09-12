package com.boostvision.platform.utils

import android.os.Build

object DeviceUtils {
    fun getName(): String {
        return "${Build.BRAND}-${Build.MODEL}-Android${Build.VERSION.RELEASE}"
    }
}