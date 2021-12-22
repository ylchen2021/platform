package remote.common.utils

import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import androidx.core.content.ContextCompat.startActivity

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import android.content.ActivityNotFoundException
import java.lang.Exception


object AppUtils {
    private const val TAG = "AppUtils"
    fun startBrowser(context: Context?, link: String) {
        val uri: Uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context?.startActivity(intent)
    }

    fun goToGooglePlaySelfPage(context: Context?) {
        if (context == null) {
            return
        }
        val playPackage = "com.android.vending"
        try {
            val currentPackageName = context.packageName
            if (currentPackageName != null) {
                val currentPackageUri = Uri.parse("market://details?id=" + context.packageName)
                val intent = Intent(Intent.ACTION_VIEW, currentPackageUri)
                intent.setPackage(playPackage)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            val currentPackageUri = Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
            val intent = Intent(Intent.ACTION_VIEW, currentPackageUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

}