package remote.common.rate

import android.content.Context
import remote.common.utils.DeviceUtils
import remote.common.utils.SPUtils
import remote.common.utils.TimeUtils

object RateCounter {
    private const val SP_NAME = "rate_counter"
    private lateinit var spUtils: SPUtils

    const val ACTION_SHOW_RATE = "ACTION_SHOW_RATE"

    private const val SP_RATE_RECORD = "SP_RATE_RECORD"
    private const val SP_RATE_TIMES = "SP_RATE_TIMES"
    private const val SP_DISABLE_RECORD_DAY = "SP_DISABLE_RECORD_DAY"
    private var PERIOD = 7
    private var TIMES_IN_PERIOD = 3
    private var TIMES_MAX = 1
    private var recordList = mutableListOf<Int>()
    private var rateTimes = 0
    private var currentVersion = 0
    private var disableDay = 0

    fun init(context: Context) {
        spUtils = SPUtils(context, SP_NAME)
        val recordStrList = spUtils.getString(SP_RATE_RECORD, "")?.split("#")
        recordStrList?.forEach {
            if (it.isNotEmpty()) {
                recordList.add(it.toInt())
            }
        }
        currentVersion = DeviceUtils.getVersionCode(context)
        val rateTimesStr = spUtils.getString(SP_RATE_TIMES, "$currentVersion:0")?.split(":")
        if (currentVersion != rateTimesStr?.get(0)?.toInt()) {
            spUtils.saveString(SP_RATE_TIMES, "$currentVersion:0")
            rateTimes = 0
        } else {
            rateTimes = rateTimesStr.get(1).toInt()
        }
        val disableDayStr = spUtils.getString(SP_DISABLE_RECORD_DAY, "")
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
            spUtils.saveString(SP_RATE_RECORD, recordList.joinToString("#"))
        }
    }

    private fun isDisableRecord(): Boolean {
        if (disableDay == 0) {
            return false
        } else if (TimeUtils.getCurrentDay() == disableDay) {
            return true
        } else {
            spUtils.saveString(SP_DISABLE_RECORD_DAY, "")
            disableDay = 0
            return false
        }
    }

    fun showRate() {
        recordList.clear()
        disableDay = TimeUtils.getCurrentDay()
        rateTimes++
        spUtils.saveString(SP_RATE_RECORD, "")
        spUtils.saveString(SP_RATE_TIMES, "${currentVersion}:$rateTimes")
        spUtils.saveString(SP_DISABLE_RECORD_DAY, TimeUtils.getCurrentDay().toString())
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