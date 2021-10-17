package com.boostvision.platform.daemon

import android.app.Application
import android.content.Context
import com.boostvision.platform.adb.AdbController
import com.boostvision.platform.adb.AdbEvent
import com.boostvision.platform.adb.AdbEventType
import java.io.File
import java.io.FileInputStream

object DaemonClient {
    private const val DAEMON_PACKAGE = "com.boostvision.daemon"
    private const val DAEMON_STARTER = "$DAEMON_PACKAGE/.DaemonStarter"
    private const val DAEMON_APK_LOCAL_PATH = "/data/local/tmp/"
    private const val DAEMON_APK_NAME = "daemon.apk"
    private lateinit var context: Application
    private var adbEventListener = object: AdbController.AdbEventListener {
        override fun onAdbEvent(event: AdbEvent) {
            when (event.eventType) {
                AdbEventType.CONNECTED -> {
                    AdbController.sendCommand("pm list packages\n")
                }
                AdbEventType.RESPONSE -> {
                    var response = event.param as String
                    if (response.contains("pm list packages")) {
                        val isDaemonInstalled = parsePackageList(response)
                        if (!isDaemonInstalled) {
                            installDaemonApk()
                        } else {
                            startDaemon()
                        }
                    } else if (response.contains("pm install")) {
                        if (response.contains("Success")) {
                            startDaemon()
                        }
                    } else if (response.contains("am broadcast")) {

                    }
                }
                AdbEventType.FILE_PUSHED -> {
                    var result = event.param as Boolean
                    if (result) {
                        AdbController.sendCommand("pm install ${DAEMON_APK_LOCAL_PATH}${DAEMON_APK_NAME}\n")
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

    private fun installDaemonApk() {
        val assetsManager = context.resources.assets
        var inputStream = assetsManager.open(DAEMON_APK_NAME)
//        var file = File("/data/local/tmp/daemon.apk")
//        var fis = FileInputStream(file)
        AdbController.pushFile(inputStream, "${DAEMON_APK_LOCAL_PATH}${DAEMON_APK_NAME}")
    }

    private fun startDaemon() {
        AdbController.sendCommand("am broadcast -n $DAEMON_STARTER\n")
    }
}