package com.boostvision.platform.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.util.*

object BitmapUtils {
    fun convertToByteArray(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.DEFAULT)
    }
}