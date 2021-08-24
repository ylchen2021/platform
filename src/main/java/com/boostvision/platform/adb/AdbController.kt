package com.boostvision.platform.adb

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Base64
import android.util.Log
import com.cgutman.adblib.AdbBase64
import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbCrypto
import com.cgutman.adblib.AdbStream
import java.io.File
import java.net.Socket

object AdbController {
    private const val TAG = "AdbController"
    private val base64Impl = AdbBase64 { arg0 -> Base64.encodeToString(arg0, Base64.NO_WRAP) }
    private var adbCrypto: AdbCrypto? = null
    private var adbStream: AdbStream? = null
    private var adbConnection: AdbConnection? = null
    private var adbThread = HandlerThread("adbThread")
    private var adbHandler: Handler? = null
    private var connected = false
    private var deviceIp = ""
    private var devicePort = 0
    private var statusListener: AdbStatusListener? = null
    private var uiHandler = Handler(Looper.getMainLooper())
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
                        disConnect()
                        notifyError("连接已断开，请重新连接")
                        connected = false
                        true
                    }
                    adbStream?.write(command.toByteArray())
                }
                MSG_DISCONNECT -> {
                    doDisConnect()
                }
            }
            true
        }
        adbCrypto = setupCrypto(context.filesDir, "pub.key", "priv.key")
    }

    fun release() {
        disConnect()
        adbHandler?.removeCallbacksAndMessages(null)
        adbThread.looper.quit()
    }

    fun setStatusListener(statusListener: AdbStatusListener?) {
        AdbController.statusListener = statusListener
    }

    fun connect(deviceIp: String, devicePort: Int) {
        var msg = Message.obtain()
        msg.what = MSG_CONNECT
        msg.obj = AdbConnectParam(deviceIp, devicePort)
        adbHandler?.sendMessage(msg)
    }

    fun doConnect(deviceIp: String, devicePort: Int): Boolean {
        var errorMsg: String? = null
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
            disConnect()
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
            disConnect()
            return false
        }

        AdbController.deviceIp = deviceIp
        AdbController.devicePort = devicePort
        connected = true
        statusListener?.onAdbConnected()


        // Start the receiving thread
//        readThread = Thread(Runnable {
//            while (adbStream?.isClosed == false) try {
//                // Print each thing we read from the shell stream
//                //print(String(adbStream!!.read(), Charset.forName("US-ASCII")))
//            } catch (e: UnsupportedEncodingException) {
//                e.printStackTrace()
//                return@Runnable
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//                return@Runnable
//            } catch (e: IOException) {
//                e.printStackTrace()
//                return@Runnable
//            }
//        })
//        readThread?.start()

        return true
    }

    fun disConnect() {
        var msg = Message.obtain()
        msg.what = MSG_DISCONNECT
        adbHandler?.sendMessage(msg)
    }

    fun doDisConnect() {
        if (connected) {
            adbStream?.close()
            adbStream = null
            adbConnection?.close()
            adbConnection = null
            connected = false
            statusListener?.onAdbDisconnected()
        }
    }

    fun isConnected(): Boolean {
        return connected
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
        uiHandler.post {
            statusListener?.onError(errorMsg)
        }
    }

    data class AdbConnectParam(
        val ip: String,
        val port: Int
    )

    interface AdbStatusListener {
        fun onAdbConnected()
        fun onAdbDisconnected()
        fun onError(errorMsg: String)
    }
}