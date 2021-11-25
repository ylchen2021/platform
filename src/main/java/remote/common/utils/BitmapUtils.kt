package remote.common.utils

import android.util.Base64

object BitmapUtils {
    fun convertToByteArray(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.DEFAULT)
    }
}