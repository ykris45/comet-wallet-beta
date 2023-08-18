package org.ergoplatform.ios.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.uikit.UIViewController

abstract class CoroutineViewController : UIViewController() {
    private var _viewControllerScope: CoroutineScope? = null
    val viewControllerScope: CoroutineScope
        get() {
            if (_viewControllerScope == null) {
                LogUtils.logDebug(this.javaClass.simpleName, "viewControllerScope started")
                _viewControllerScope = CoroutineScope(Dispatchers.Default)
            }
            return _viewControllerScope!!
        }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        getAppDelegate().addAppActiveObserver(this)
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        getAppDelegate().removeAppActiveObserver(this)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        _viewControllerScope?.let {
            LogUtils.logDebug(this.javaClass.simpleName, "viewControllerScope cancelled")
            it.cancel()
        }
        _viewControllerScope = null
    }

    open fun onResume() {
        // called when app gets active again, override in subclasses
    }
}