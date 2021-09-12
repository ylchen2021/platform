package com.boostvision.platform.adb

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Base64
import android.util.Log
import com.boostvision.platform.utils.Logger
import com.cgutman.adblib.AdbBase64
import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbCrypto
import com.cgutman.adblib.AdbStream
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.StringBuilder
import java.net.Socket
import java.nio.charset.Charset

object AdbController {
    private const val TAG = "AdbController"
    private val base64Impl = AdbBase64 { arg0 -> Base64.encodeToString(arg0, Base64.NO_WRAP) }
    private var adbCrypto: AdbCrypto? = null
    private var adbStream: AdbStream? = null
    private var adbConnection: AdbConnection? = null
    private var adbThread = HandlerThread("adb_thread")
    private var adbHandler: Handler? = null
    private var connected = false
    private var deviceIp = ""
    private var devicePort = 0
    private var statusListeners: MutableList<AdbStatusListener> = arrayListOf()
    private var readThread: Thread? = null

    private var MSG_CONNECT = 1
    private var MSG_DISCONNECT = 2
    private var MSG_COMMAND = 3

    fun init(context: Context) {
        adbThread.start()
        adbHandler = Handler(adbThread.looper) {
            when (it.what) {
                MSG_CONNECT -> {
                    var param = it.obj as AdbConnectParam
                    doConnect(param.ip, param.port)
                }
                MSG_COMMAND -> {
                    if (!connected) {
                        notifyError("设备未连接，请先连接设备")
                    }

                    val command = it.obj as String
                    if (adbStream?.isClosed == true) {
                        disconnect()
                        notifyError("连接已断开，请重新连接")
                        connected = false
                        true
                    }
                    Logger.d(TAG, "(request)response=${command}")
                    adbStream?.write(command.toByteArray())
                }
                MSG_DISCONNECT -> {
                    doDisconnect()
                }
            }
            true
        }
        adbCrypto = setupCrypto(context.filesDir, "pub.key", "priv.key")
    }

    fun release() {
        disconnect()
        adbHandler?.removeCallbacksAndMessages(null)
        adbThread.looper.quit()
    }

    fun addStatusListener(listener: AdbStatusListener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener)
        }
    }

    fun removeStatusListener(listener: AdbStatusListener) {
        if (statusListeners.contains(listener)) {
            statusListeners.remove(listener)
        }
    }

    fun connect(deviceIp: String, devicePort: Int) {
        var msg = Message.obtain()
        msg.what = MSG_CONNECT
        msg.obj = AdbConnectParam(deviceIp, devicePort)
        adbHandler?.sendMessage(msg)
    }

    private fun doConnect(deviceIp: String, devicePort: Int): Boolean {
        var errorMsg: String? = null
        statusListeners.forEach {
            it.onAdbConnecting(deviceIp)
        }
        if (adbCrypto == null) {
            errorMsg = "adbCrypto is null, please call init() first";
            Log.e(TAG, errorMsg)
            notifyError(errorMsg)
            return false
        }

        if (connected) {
            if (AdbController.deviceIp == deviceIp && AdbController.devicePort == devicePort) {
                errorMsg = "this ip is already connnected, no need to reconnect"
                Log.i(TAG, errorMsg)
                return false
            }
            disconnect()
        }

        try {
            var socket = Socket(deviceIp, devicePort)
            adbConnection = AdbConnection.create(socket, adbCrypto)
            adbConnection?.connect()
            adbStream = adbConnection?.open("shell:")
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = "connect error!"
            Log.i(TAG, errorMsg, e)
            notifyError(errorMsg)
            disconnect()
            return false
        }

        this.deviceIp = deviceIp
        this.devicePort = devicePort
        connected = true
        statusListeners.forEach {
            it.onAdbConnected(deviceIp)
        }

        // Start the receiving thread
        readThread = Thread(Runnable {
            var responseBuilder = StringBuilder()
            while (adbStream?.isClosed == false){
                try {
                    // Print each thing we read from the shell stream
                    var response = String(adbStream!!.read(), Charset.forName("US-ASCII"))
                    if (!response.endsWith("/ \$ ")) {
                        responseBuilder.append(response)
                    } else {
                        responseBuilder.append(response)
                        var fullResponse = responseBuilder.toString()
                        Logger.d(TAG, "response=${fullResponse}")
                        statusListeners.forEach {
                            it.onResponse(fullResponse)
                        }
                        responseBuilder = StringBuilder()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@Runnable
                }
            }
        })
        readThread?.start()

        return true
    }

    fun disconnect() {
        var msg = Message.obtain()
        msg.what = MSG_DISCONNECT
        adbHandler?.sendMessage(msg)
    }

    fun doDisconnect() {
        if (connected) {
            adbStream?.close()
            adbStream = null
            adbConnection?.close()
            adbConnection = null
            connected = false
            statusListeners.forEach {
                it.onAdbDisconnected(deviceIp)
            }
        }
    }

    fun sendCommand(command: String) {
        if (command.isEmpty()) {
            Log.e(TAG, "sendCommand cmd is empty")
            return
        }
        var msg = Message.obtain()
        msg.what = MSG_COMMAND
        msg.obj = command
        adbHandler?.sendMessage(msg)
    }

    private fun setupCrypto(dirFile: File, pubKeyFile: String, privKeyFile: String): AdbCrypto? {
        val pub = File(dirFile, pubKeyFile)
        val priv = File(dirFile, privKeyFile)
        var c: AdbCrypto? = null

        // Try to load a key pair from the files
        if (pub.exists() && priv.exists()) {
            Log.i(TAG, "pub and private key exsit")
            c = try {
                AdbCrypto.loadAdbKeyPair(base64Impl, priv, pub)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "setupCrypto error", e)
                null
            }
        }
        if (c == null) {
            Log.i(TAG, "generateAdbKeyPair")
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(base64Impl)
            // Save it
            c.saveAdbKeyPair(priv, pub)
        }

        return c
    }

    private fun notifyError(errorMsg: String) {
        statusListeners.forEach {
            it.onError(errorMsg)
        }
    }

    data class AdbConnectParam(
        val ip: String,
        val port: Int
    )

    interface AdbStatusListener {
        fun onAdbConnecting(ip: String)
        fun onAdbConnected(ip: String)
        fun onAdbDisconnected(ip: String)
        fun onError(errorMsg: String)
        fun onResponse(msg: String)
    }
}