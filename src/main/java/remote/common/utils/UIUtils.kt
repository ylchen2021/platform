package remote.common.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

object UIUtils {
    private var sScreenWidth = 0
    private var sScreenHeight = 0
    private var sDensity = 0f

    /**
     * 转换 dp -> px
     */
    fun dp2px(context: Context?, dp: Int): Int {
        return dp2px(context,dp.toFloat())
    }

    /**
     * 转换 dp -> px
     */
    fun dp2px(context: Context?, dp: Float): Int {
        if (context == null) return 0
        return (context.resources.displayMetrics.density * dp + 0.5f).toInt()
    }

    /**
     * 转换 px -> dp
     */
    fun px2dp(context: Context, px: Int): Int {
        return (px / context.resources.displayMetrics.density + 0.5f).toInt()
    }

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(context: Context?): Int {
        if (context == null) {
            return sScreenWidth
        }

        if (sScreenWidth == 0) {
            val displayMetrics = DisplayMetrics()
            val wm = getWindowManager(context)
            wm!!.defaultDisplay.getRealMetrics(displayMetrics)
            sScreenWidth = displayMetrics.widthPixels
        }
        return sScreenWidth
    }

    /**
     * 获取屏幕高度
     */
    fun getScreenHeight(context: Context?): Int {
        if (context == null) {
            return sScreenHeight
        }

        if (sScreenHeight == 0) {
            val displayMetrics = DisplayMetrics()
            val wm = getWindowManager(context)
            wm!!.defaultDisplay.getRealMetrics(displayMetrics)
            sScreenHeight = displayMetrics.heightPixels
        }
        return sScreenHeight
    }

    /**
     * 获取屏幕密度
     */
    fun getDensity(context: Context?): Float {
        if (context == null) {
            return sDensity
        }

        if (sDensity == 0f) {
            val displayMetrics = DisplayMetrics()
            val wm = getWindowManager(context)
            wm!!.defaultDisplay.getMetrics(displayMetrics)
            sDensity = displayMetrics.density
        }
        return sDensity
    }

    private fun getWindowManager(context: Context?): WindowManager? {
        if (context == null) {
            return null
        }

        return context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
}
