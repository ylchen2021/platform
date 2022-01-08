package remote.common.ui

abstract class TabFragment: BaseFragment() {
    open fun onEnter(){}

    open fun onExit(){}
}