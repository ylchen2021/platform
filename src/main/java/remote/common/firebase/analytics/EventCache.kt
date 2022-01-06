package remote.common.firebase.analytics

import android.os.Bundle
import remote.common.utils.TimeUtils

object EventCache {
    private const val MAX_COUNT = 200
    private val logList = arrayListOf<EventItem>()

    @Synchronized
    fun addLog(eventId: String, bundle: Bundle) {
        val date = TimeUtils.getNowTimeString("yyyy/MM/dd HH:mm:ss")
        val params = arrayListOf<Pair<String?, Any?>>()
        val keySet = bundle.keySet()
        keySet.forEach { key ->
            val value = bundle.get(key)
            params.add(Pair(key, value))
        }
        if (logList.size >= MAX_COUNT) {
            logList.removeLast()
        }
        logList.add(0, EventItem(date, eventId, params))
    }

    @Synchronized
    fun getLogList(): List<EventItem> {
        return logList
    }

    @Synchronized
    fun clearAll() {
        logList.clear()
    }
}

data class EventItem (
    val eventTime: String,
    val eventId: String,
    val eventParams: List<Pair<String?, Any?>>
)