package remote.common.rate

import android.content.Context
import android.content.SharedPreferences
import remote.common.utils.DeviceUtils

object RateCounter {
    const val ACTION_SHOW_RATE = "ACTION_SHOW_RATE"

    private const val SP_RATE_RECORD = "SP_RATE_RECORD"
    private const val SP_RATE_TIMES = "SP_RATE_TIMES"
    private var PERIOD = 7
    private var TIMES_IN_PERIOD = 3
    private var TIMES_MAX = 1
    private lateinit var sp: SharedPreferences
    private var recordList = mutableListOf<Int>()
    private var rateTimes = 0
    private var currentVersion = 0

    fun init(context: Context, sharedPreferences: SharedPreferences) {
        sp = sharedPreferences
        val recordStrList = sp.getString(SP_RATE_RECORD, "")?.split("#")
        recordStrList?.forEach {
            if (it.isNotEmpty()) {
                recordList.add(it.toInt())
            }
        }
        currentVersion = DeviceUtils.getVersionCode(context)
        val rateTimesStr = sp.getString(SP_RATE_TIMES, "$currentVersion:0")?.split(":")
        if (currentVersion != rateTimesStr?.get(0)?.toInt()) {
            val editor = sp.edit()
            editor.putString(SP_RATE_TIMES, "$currentVersion:0")
            editor.apply()
            rateTimes = 0
        } else {
            rateTimes = rateTimesStr.get(1).toInt()
        }
    }

    fun setPeriod(period: Int): RateCounter {
        PERIOD = period
        return this
    }

    fun setTimesInPeriod(timesInPeriod: Int): RateCounter {
        TIMES_IN_PERIOD = timesInPeriod
        return this
    }

    fun setTimesMax(timesMax: Int): RateCounter {
        TIMES_MAX = timesMax
        return this
    }

    fun recordToday() {
        var day = (System.currentTimeMillis() / (1000*3600*24)).toInt()
        if (!recordList.contains(day)) {
            recordList.add(day)
            val editor = sp.edit()
            editor.putString(SP_RATE_RECORD, recordList.joinToString("#"))
            editor.apply()
        }
    }

    fun showRate() {
        recordList.clear()
        rateTimes++
        val editor = sp.edit()
        editor.putString(SP_RATE_RECORD, "")
        editor.putString(SP_RATE_TIMES, "${currentVersion}:$rateTimes")
        editor.apply()
    }

    fun canShowRate(): Boolean {
        var currentDay = (System.currentTimeMillis() / (1000 * 3600 * 24)).toInt()
        var validStart = currentDay - PERIOD
        var listDays = mutableListOf<Int>()
        recordList.forEach {
            if (it > validStart) {
                listDays.add(it)
            }
        }
        if (listDays.size >= TIMES_IN_PERIOD && rateTimes < TIMES_MAX) {
            return true
        }
        return false
    }
}