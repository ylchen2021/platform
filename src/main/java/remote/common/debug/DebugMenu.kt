package remote.common.debug

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import remote.common.firebase.analytics.EventCache
import tv.remote.platform.R

object DebugMenu {
    fun attach(activity: FragmentActivity) {
        val debugMenu = LayoutInflater.from(activity).inflate(R.layout.menu_debug, null, false)
        debugMenu.findViewById<View>(R.id.tv_events).setOnClickListener {
            EventsListDialog().show(activity.supportFragmentManager, "")
        }
        debugMenu.findViewById<View>(R.id.tv_clear_events).setOnClickListener {
            EventCache.clearAll()
            Toast.makeText(activity, "All events cleared.", Toast.LENGTH_SHORT).show()
        }
        val wm = activity.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT
        )
        lp.token = activity.window.decorView.windowToken
        lp.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.x = 0
        lp.y = 0
        wm.addView(debugMenu, lp)
    }
}