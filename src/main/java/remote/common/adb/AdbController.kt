package remote.common.adb

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Base64
import android.util.Log
import remote.common.utils.Logger
import com.cgutman.adblib.AdbBase64
import com.cgutman.adblib.AdbCrypto
import java.io.*

object AdbController {
    private const val TAG = "AdbController"
    private val base64Impl = AdbBase64 { arg0 -> Base64.encodeToString(arg0, Base64.NO_WRAP) }
    private var adbCrypto: AdbCrypto? = null
    private var adbThread = HandlerThread("adb_thread")
    private var adbHandler: Handler? = null
    private var eventListeners: MutableList<AdbEventListener> = arrayListOf()
    private val clientsMap = hashMapOf<String, AdbClient>()

    private var MSG_CONNECT = 1
    private var MSG_DISCONNECT = 2
    private var MSG_COMMAND = 3
    private var MSG_PUSH = 4
    private var MSG_EVENT = 5

    fun init(context: Context) {
        adbThread.start()
        adbHandler = Handler(adbThread.looper) {
            when (it.what) {
                MSG_CONNECT -> {
                    val param = it.obj as AdbConnectParam
                    val newClient = AdbClient(param.ip, param.port, adbCrypto, this)
                    addClient(param.ip, newClient)
                    newClient.connect()
                }
                MSG_DISCONNECT -> {
                    var ip = it.obj as String
                    getClient(ip)?.disconnect()
                }
                MSG_COMMAND -> {
                    val command = it.obj as String
                    Logger.d(TAG, "(request)response=${command}")
                    val clientList = getConnectedClient()
                    clientList.forEach { client ->
                        client?.write(command)
                    }
                }
                MSG_PUSH -> {
                    var param = it.obj as PushParam
                    val clientList = getConnectedClient()
                    clientList.forEach { client ->
                        if (client != null) {
                            var result = client.push(param.inputStream, param.remotePath)
                            notifyEvent(AdbEventType.FILE_PUSHED, null, result)
                        }
                    }
                }
                MSG_EVENT -> {
                    var param = it.obj as AdbEventParam
                    notifyEvent(param.type, param.status, param.param)
                }
            }
            true
        }
        adbCrypto = setupCrypto(context.filesDir, "pub.key", "priv.key")
    }

    fun release() {
        clientsMap.values.forEach {
            if (it.getStatus() == AdbStatus.CONNECTED) {
                it.disconnect()
            }
        }
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

    fun disconnect(ip: String) {
        var msg = Message.obtain()
        msg.what = MSG_DISCONNECT
        msg.obj = ip
        adbHandler?.sendMessage(msg)
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

    private fun getConnectedClient(): List<AdbClient?> {
        val outList = mutableListOf<AdbClient>()
        clientsMap.values.forEach {
            if (it.getStatus() == AdbStatus.CONNECTED) {
                outList.add(it)
            }
        }
        return outList
    }

    private fun getClient(ip: String): AdbClient? {
        return clientsMap[ip]
    }

    private fun addClient(ip: String, newClient: AdbClient) {
        clientsMap[ip] = newClient
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

    fun sendEvent(type: AdbEventType, status: Any?, param: Any?) {
        var msg = Message.obtain()
        msg.what = MSG_EVENT
        msg.obj = AdbEventParam(type, status, param)
        adbHandler?.sendMessage(msg)
    }

    private fun notifyEvent(type: AdbEventType, status: Any?, param: Any?) {
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

    data class AdbEventParam(
        var type: AdbEventType,
        var status: Any?,
        var param: Any?
    )

    interface AdbEventListener {
        fun onAdbEvent(adbEvent: AdbEvent)
    }
}