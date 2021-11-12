package com.boostvision.platform.daemon

import android.app.Application
import com.boostvision.platform.adb.AdbController
import com.boostvision.platform.adb.AdbEvent
import com.boostvision.platform.adb.AdbEventType
import com.boostvision.platform.adb.AdbStatus

object DaemonClient {
    private const val DAEMON_PACKAGE = "com.boostvision.daemon"
    private const val DAEMON_STARTER = "$DAEMON_PACKAGE/.DaemonStarter"
    private const val DAEMON_APK_LOCAL_PATH = "/data/local/tmp/"
    private const val DAEMON_APK_NAME = "daemon.apk"
    const val DEFAULT_PORT = 8088

    private lateinit var context: Application
    private var adbEventListener = object: AdbController.AdbEventListener {
        override fun onAdbEvent(event: AdbEvent) {
            when (event.eventType) {
                AdbEventType.STATUS -> {
                    if (event.status == AdbStatus.CONNECTED) {
                        AdbController.sendCommand("pm list packages\n")
                    }
                }
                AdbEventType.RESPONSE -> {
                    var response = event.param as String
                    if (response.contains("pm list packages")) {
                        val isDaemonInstalled = parsePackageList(response)
                        if (!isDaemonInstalled) {
                            pushDaemonApk()
                        } else {
                            launchDaemon()
                        }
                    } else if (response.contains("pm install")) {
                        if (response.contains("Success")) {
                            launchDaemon()
                        }
                    } else if (response.contains("am broadcast")) {

                    }
                }
                AdbEventType.FILE_PUSHED -> {
                    var result = event.param as Boolean
                    if (result) {
                        installDaemon()
                    }
                }
            }
        }
    }

    fun init(context: Application) {
        this.context = context
        AdbController.addEventListener(adbEventListener)
    }

    fun release() {
        AdbController.removeEventListener(adbEventListener)
    }

    private fun parsePackageList(response: String): Boolean {
        var isDaemonInstalled = false
        var packageData = response.split("\r\n") //package:xxxx.xxxx.xxx
        packageData.forEach {
            if (it.contains("package:")) {
                var packageName = it.substringAfter("package:")
                if (packageName == DAEMON_PACKAGE) {
                    isDaemonInstalled = true
                }
            }
        }
        return isDaemonInstalled
    }

    fun installDaemon() {
        AdbController.sendCommand("pm install ${DAEMON_APK_LOCAL_PATH}${DAEMON_APK_NAME}\n")
    }

    fun pushDaemonApk() {
        val assetsManager = context.resources.assets
        var inputStream = assetsManager.open(DAEMON_APK_NAME)
        AdbController.pushFile(inputStream, "${DAEMON_APK_LOCAL_PATH}${DAEMON_APK_NAME}")
    }

    fun launchDaemon() {
        AdbController.sendCommand("am broadcast -n $DAEMON_STARTER\n")
    }
}