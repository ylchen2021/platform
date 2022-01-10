package remote.common.rate

import android.content.Context
import android.content.SharedPreferences
import remote.common.utils.DeviceUtils
import remote.common.utils.TimeUtils

object RateCounter {
    const val ACTION_SHOW_RATE = "ACTION_SHOW_RATE"

    private const val SP_RATE_RECORD = "SP_RATE_RECORD"
    private const val SP_RATE_TIMES = "SP_RATE_TIMES"
    private const val SP_DISABLE_RECORD_DAY = "SP_DISABLE_RECORD_DAY"
    private var PERIOD = 7
    private var TIMES_IN_PERIOD = 3
    private var TIMES_MAX = 1
    private lateinit var sp: SharedPreferences
    private var recordList = mutableListOf<Int>()
    private var rateTimes = 0
    private var currentVersion = 0
    private var disableDay = 0

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
        val disableDayStr = sp.getString(SP_DISABLE_RECORD_DAY, "")
        if (disableDayStr != null && disableDayStr.isNotEmpty()) {
            disableDay = disableDayStr.toInt()
        } else {
            disableDay = 0
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
        var day = TimeUtils.getCurrentDay()
        if (!recordList.contains(day) && !isDisableRecord()) {
            recordList.add(day)
            val editor = sp.edit()
            editor.putString(SP_RATE_RECORD, recordList.joinToString("#"))
            editor.apply()
        }
    }

    private fun isDisableRecord(): Boolean {
        if (disableDay == 0) {
            return false
        } else if (TimeUtils.getCurrentDay() == disableDay) {
            return true
        } else {
            val editor = sp.edit()
            editor.putString(SP_DISABLE_RECORD_DAY, "")
            editor.apply()
            disableDay = 0
            return false
        }
    }

    fun showRate() {
        recordList.clear()
        disableDay = TimeUtils.getCurrentDay()
        rateTimes++
        val editor = sp.edit()
        editor.putString(SP_RATE_RECORD, "")
        editor.putString(SP_RATE_TIMES, "${currentVersion}:$rateTimes")
        editor.putString(SP_DISABLE_RECORD_DAY, TimeUtils.getCurrentDay().toString())
        editor.apply()
    }

    fun canShowRate(): Boolean {
        var currentDay = TimeUtils.getCurrentDay()
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