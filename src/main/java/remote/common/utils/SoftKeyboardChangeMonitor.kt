package remote.common.utils

import android.app.Activity
import android.graphics.Rect
import android.view.View

class SoftKeyboardChangeMonitor(activity: Activity?) {
    private var rootView: View? = activity?.window?.decorView
    var rootViewVisibleHeight = 0
    private var onSoftKeyBoardChangeListener: OnSoftKeyBoardChangeListener? = null

    init {
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val r = Rect()
            rootView?.getWindowVisibleDisplayFrame(r)
            val visibleHeight: Int = r.height()
            if (rootViewVisibleHeight == 0) {
                rootViewVisibleHeight = visibleHeight
                return@addOnGlobalLayoutListener
            }

            if (rootViewVisibleHeight == visibleHeight) {
                return@addOnGlobalLayoutListener
            }

            if (rootViewVisibleHeight - visibleHeight > 300) {
                if (onSoftKeyBoardChangeListener != null) {
                    onSoftKeyBoardChangeListener?.keyBoardShow(rootViewVisibleHeight - visibleHeight)
                }
                rootViewVisibleHeight = visibleHeight
                return@addOnGlobalLayoutListener
            }

            if (visibleHeight - rootViewVisibleHeight > 300) {
                if (onSoftKeyBoardChangeListener != null) {
                    onSoftKeyBoardChangeListener?.keyBoardHide(visibleHeight - rootViewVisibleHeight)
                }
                rootViewVisibleHeight = visibleHeight
                return@addOnGlobalLayoutListener
            }
        }
    }

    interface OnSoftKeyBoardChangeListener {
        fun keyBoardShow(height: Int)
        fun keyBoardHide(height: Int)
    }

    fun setListener(listener: OnSoftKeyBoardChangeListener?) {
        onSoftKeyBoardChangeListener = listener
    }
}