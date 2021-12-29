package remote.common.ui

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.DialogFragment
import remote.common.utils.UIUtils

abstract class BaseDialog protected constructor() : DialogFragment() {
    private var onDismissCallback: (()->Unit)? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window = dialog?.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp = window.attributes
            lp.width = getLayoutWidth()
            lp.height = getLayoutHeight()
            lp.gravity = getGravity()
            window.decorView.setPadding(0, 0, 0, 0)
            window.attributes = lp
            if (getAnimation() > 0) {
                window.setWindowAnimations(getAnimation())
            }
        }
        return inflater.inflate(getLayoutRes(), container, false)
    }

    protected open fun getGravity(): Int {
        return Gravity.CENTER
    }

    protected open fun getLayoutWidth(): Int {
        return ViewGroup.LayoutParams.MATCH_PARENT
    }

    protected open fun getLayoutHeight(): Int {
        return ViewGroup.LayoutParams.WRAP_CONTENT
    }

    protected open fun getAnimation(): Int {
        return 0
    }

    protected abstract fun getLayoutRes(): Int

    protected open fun getWidthPaddingDp(): Int {
        return 15
    }

    fun setOnDismissCallback(callback: (()->Unit)) {
        onDismissCallback = callback
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismissCallback?.invoke()
    }

    override fun dismiss() {
        if (dialog?.isShowing == true) {
            super.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val dm = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(dm)
            dialog.window?.setLayout(UIUtils.getScreenWidth(context) - UIUtils.dp2px(context, getWidthPaddingDp()), getLayoutHeight())
        }
    }
}