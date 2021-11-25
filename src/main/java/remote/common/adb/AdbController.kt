package remote.common.adb

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Base64
import android.util.Log
import remote.common.utils.Logger
import com.cgutman.adblib.AdbBase64
import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbCrypto
import com.cgutman.adblib.AdbStream
import remote.common.utils.ByteUtils
import java.io.*
import java.lang.StringBuilder
import java.net.Socket
import java.nio.charset.Charset

object AdbController {
    private const val TAG = "AdbController"
    private val base64Impl = AdbBase64 { arg0 -> Base64.encodeToString(arg0, Base64.NO_WRAP) }
    private var adbCrypto: AdbCrypto? = null
    private var adbShellStream: AdbStream? = null
    private var adbSyncStream: AdbStream? = null
    private var adbConnection: AdbConnection? = null
    private var adbThread = HandlerThread("adb_thread")
    private var adbHandler: Handler? = null
    private var connected = false
    private var deviceIp = ""
    private var devicePort = 0
    private var eventListeners: MutableList<AdbEventListener> = arrayListOf()
    private var readThread: Thread? = null

    private var MSG_CONNECT = 1
    private var MSG_DISCONNECT = 2
    private var MSG_COMMAND = 3
    private var MSG_PUSH = 4

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
                        notifyEvent(AdbEventType.ERROR, null, "设备未连接，请先连接设备")
                    }

                    val command = it.obj as String
                    if (adbShellStream?.isClosed == true) {
                        disconnect()
                    } else {
                        Logger.d(TAG, "(request)response=${command}")
                        adbShellStream?.write(command.toByteArray())
                    }
                }
                MSG_DISCONNECT -> {
                    doDisconnect()
                }
                MSG_PUSH -> {
                    var param = it.obj as PushParam
                    var result = doPush(param.inputStream, param.remotePath)
                    notifyEvent(AdbEventType.FILE_PUSHED, null, result)
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

    fun addEventListener(listener: AdbEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }

    fun removeEventListener(listener: AdbEventListener) {
        if (eventListeners.contains(listener)) {
            eventListeners.remove(listener)
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
        notifyEvent(AdbEventType.STATUS, AdbStatus.CONNECTING, deviceIp)
        if (adbCrypto == null) {
            errorMsg = "adbCrypto is null, please call init() first";
            Log.e(TAG, errorMsg)
            notifyEvent(AdbEventType.ERROR, null, errorMsg)
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
            adbShellStream = adbConnection?.open("shell:")
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = "connect error!"
            Log.i(TAG, errorMsg, e)
            notifyEvent(AdbEventType.ERROR, null, errorMsg)
            disconnect()
            return false
        }

        AdbController.deviceIp = deviceIp
        AdbController.devicePort = devicePort
        connected = true
        notifyEvent(AdbEventType.STATUS, AdbStatus.CONNECTED, deviceIp)

        // Start the receiving thread
        readThread = Thread(Runnable {
            var responseBuilder = StringBuilder()
            while (adbShellStream?.isClosed == false){
                try {
                    // Print each thing we read from the shell stream
                    var response = String(adbShellStream!!.read(), Charset.forName("US-ASCII"))
                    if (!response.endsWith("/ \$ ")) {
                        responseBuilder.append(response)
                    } else {
                        responseBuilder.append(response)
                        var fullResponse = responseBuilder.toString()
                        Logger.d(TAG, "response=${fullResponse}")
                        notifyEvent(AdbEventType.RESPONSE, null, fullResponse)
                        responseBuilder = StringBuilder()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    disconnect()
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
        if (adbShellStream != null) {
            adbShellStream?.close()
            adbShellStream = null
        }
        if (adbSyncStream != null) {
            adbSyncStream?.close()
            adbSyncStream = null
        }
        if (adbConnection != null) {
            adbConnection?.close()
            adbConnection = null
        }
        connected = false
        notifyEvent(AdbEventType.STATUS, AdbStatus.DISCONNECTED, deviceIp)
        notifyEvent(AdbEventType.ERROR, null, "连接已断开，请重新连接")
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

    fun pushFile(inputStream: InputStream, remotePath: String) {
        var msg = Message.obtain()
        msg.what = MSG_PUSH
        msg.obj = PushParam(inputStream, remotePath)
        adbHandler?.sendMessage(msg)
    }

    private fun doPush(inputStream: InputStream, remotePath: String): Boolean {
        var result = false
        try {
            if (adbSyncStream == null) {
                adbSyncStream = adbConnection?.open("sync:")
            }
            val sendId = "SEND"
            val mode = ",33206"
            val length = (remotePath + mode).length

            adbSyncStream?.write(ByteUtils.concat(sendId.toByteArray(), ByteUtils.intToByteArray(length)))
            adbSyncStream?.write(remotePath.toByteArray())
            adbSyncStream?.write(mode.toByteArray())

            val buff = ByteArray(adbConnection!!.maxData)
            val byteDATA = "DATA".toByteArray()
            val byteExtra = ByteArray(byteDATA.size+4)
            System.arraycopy(byteDATA, 0, byteExtra, 0, byteDATA.size)
            val byteLengthTmp = ByteArray(4)

            var sent: Long = 0
            while (true) {
                val read = inputStream.read(buff)
                if (read < 0) {
                    break
                }
                ByteUtils.intToBytes(read, byteLengthTmp)
                System.arraycopy(byteLengthTmp, 0, byteExtra, byteDATA.size, 4)
                adbSyncStream?.write(byteExtra)
                if (read == buff.size) {
                    adbSyncStream?.write(buff)
                } else {
                    val tmp = ByteArray(read)
                    System.arraycopy(buff, 0, tmp, 0, read)
                    adbSyncStream?.write(tmp)
                }
                sent += read.toLong()
                Logger.d(TAG, "push sent=${sent/1024}KB")
            }

            adbSyncStream?.write(ByteUtils.concat("DONE".toByteArray(), ByteUtils.intToByteArray(System.currentTimeMillis().toInt())))
            val res = adbSyncStream?.read()
            // TODO: test if res contains "OKEY" or "FAIL"
            result = String(res!!).startsWith("OKAY")
            adbSyncStream?.write(ByteUtils.concat("QUIT".toByteArray(), ByteUtils.intToByteArray(0)))
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return result
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

    private fun notifyEvent(type: AdbEventType, status: AdbStatus?, param: Any) {
        eventListeners.forEach {
            it.onAdbEvent(AdbEvent(type, status, param))
        }
    }

    data class AdbConnectParam(
        val ip: String,
        val port: Int
    )

    data class PushParam(
        var inputStream: InputStream,
        var remotePath: String
    )

    interface AdbEventListener {
        fun onAdbEvent(adbEvent: AdbEvent)
    }
}