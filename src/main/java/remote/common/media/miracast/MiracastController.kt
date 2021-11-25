package remote.common.media.miracast

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

object MiracastController {
    fun goToMiracastSetting(activity: Activity): Boolean {
        val wifiActionIntent = Intent("android.settings.WIFI_DISPLAY_SETTINGS")
        val castActionIntent = Intent("android.settings.CAST_SETTINGS")

        var systemResolveInfo: ResolveInfo? = getSystemResolveInfo(activity, wifiActionIntent)
        if (systemResolveInfo != null) {
            try {
                val systemWifiIntent = Intent()
                systemWifiIntent.setClassName(
                    systemResolveInfo.activityInfo.applicationInfo.packageName,
                    systemResolveInfo.activityInfo.name
                )
                startSettingsActivity(systemWifiIntent, activity)
                return true
            } catch (ignored: ActivityNotFoundException) {
                ignored.printStackTrace()
            }
        }

        systemResolveInfo = getSystemResolveInfo(activity, castActionIntent)
        if (systemResolveInfo != null) {
            try {
                val systemCastIntent = Intent()
                systemCastIntent.setClassName(
                    systemResolveInfo.activityInfo.applicationInfo.packageName,
                    systemResolveInfo.activityInfo.name
                )
                startSettingsActivity(systemCastIntent, activity)
                return true
            } catch (ignored: ActivityNotFoundException) {
                ignored.printStackTrace()
            }
        }
        return false
    }

    private fun getSystemResolveInfo(context: Context, intent: Intent): ResolveInfo? {
        val pm = context.packageManager
        val list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in list) {
            try {
                val activityInfo = pm.getApplicationInfo(info.activityInfo.packageName, 0)
                if (activityInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    return info
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
        }
        return null
    }

    private fun startSettingsActivity(intent: Intent, activity: Activity): Boolean {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        try {
            activity.startActivity(intent)
            return true
        } catch (e: SecurityException) {
            // We don't have permission to launch this activity, alert the user and return.
            e.printStackTrace()
        }
        return false
    }
}