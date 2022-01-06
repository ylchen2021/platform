package remote.common.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object TimeUtils {
    fun getCurrentDay(): Int {
        return (System.currentTimeMillis() / (1000*3600*24)).toInt()
    }

    fun getNowTimeString(pattern: String): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(System.currentTimeMillis()))
    }
}