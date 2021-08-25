package com.boostvision.platform.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {
    private val STATE_SAVE_IS_HIDDEN = "STATE_SAVE_IS_HIDDEN"

    protected abstract fun getLayoutRes(): Int

    protected abstract fun onInit(savedInstanceState: Bundle?)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var outView = inflater.inflate(getLayoutRes(), container, false)
        if (savedInstanceState != null) {
            val isSupportHidden = savedInstanceState.getBoolean(STATE_SAVE_IS_HIDDEN)
            val ft = parentFragmentManager.beginTransaction()
            if (isSupportHidden) {
                ft.hide(this)
            } else {
                ft.show(this)
            }
            ft.commitAllowingStateLoss()
        }
        onInit(savedInstanceState)
        return outView
    }
}