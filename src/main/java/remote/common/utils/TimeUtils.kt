package remote.common.utils

object TimeUtils {
    fun getCurrentDay(): Int {
        return (System.currentTimeMillis() / (1000*3600*24)).toInt()
    }
}