package com.boostvision.platform.daemon

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boostvision.platform.adb.AdbController
import com.boostvision.platform.adb.AdbEvent
import com.boostvision.platform.adb.AdbEventType
import com.boostvision.platform.adb.AdbStatus
import com.boostvision.platform.network.HttpClient
import com.boostvision.platform.network.ResponseBean
import retrofit2.http.GET

object DaemonClient {
    private const val DAEMON_PACKAGE = "com.boostvision.daemon"
    private const val DAEMON_STARTER = "$DAEMON_PACKAGE/.DaemonStarter"
    private const val DAEMON_APK_LOCAL_PATH = "/data/local/tmp/"
    private const val DAEMON_APK_NAME = "daemon.apk"
    private const val DAEMON_VERSION = 3
    const val DEFAULT_PORT = 8088

    private lateinit var deviceInfoResult: LiveData<ResponseBean<DeviceInfo>>
    private val deviceInfoTrigger = MutableLiveData<String>()
    private var connectedIp = ""
    private lateinit var context: Application
    private var adbEventListener = object: AdbController.AdbEventListener {
        override fun onAdbEvent(event: AdbEvent) {
            when (event.eventType) {
                AdbEventType.STATUS -> {
                    if (event.status == AdbStatus.CONNECTED) {
                        AdbController.sendCommand("pm list packages\n")
                        connectedIp = event.param as String
                    } else if (event.status == AdbStatus.DISCONNECTED) {
                        connectedIp = ""
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
                            //check if need update daemon
                            if (connectedIp.isNotEmpty()) {
                                deviceInfoTrigger.postValue(connectedIp)
                            }
                        }
                    } else if (response.contains("pm install")) {
                        if (response.contains("Success")) {
                            launchDaemon()
                        }
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

        deviceInfoResult = Transformations.switchMap(deviceInfoTrigger) {
            HttpClient.create("http://${it}:${DEFAULT_PORT}/", DeviceRequestInterface::class.java)
                .getDeviceInfo()
        }
        deviceInfoResult.observeForever { response ->
            if (response.isSuccess()) {
                val version = response.data?.daemonVersion?:0
                if (DAEMON_VERSION > version) {
                    pushDaemonApk()
                }
            }
        }
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

    private fun installDaemon() {
        AdbController.sendCommand("pm install ${DAEMON_APK_LOCAL_PATH}${DAEMON_APK_NAME}\n")
    }

    private fun pushDaemonApk() {
        val assetsManager = context.resources.assets
        var inputStream = assetsManager.open(DAEMON_APK_NAME)
        AdbController.pushFile(inputStream, "${DAEMON_APK_LOCAL_PATH}${DAEMON_APK_NAME}")
    }

    fun launchDaemon() {
        AdbController.sendCommand("am broadcast -n $DAEMON_STARTER\n")
    }

    interface DeviceRequestInterface {
        @GET("device")
        fun getDeviceInfo(): LiveData<ResponseBean<DeviceInfo>>
    }
}