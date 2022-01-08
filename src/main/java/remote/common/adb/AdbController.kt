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
    private var adbThread = HandlerThread("adb_controller_thread")
    private var adbHandler: Handler? = null
    private var eventListeners: MutableList<AdbEventListener> = arrayListOf()
    private val clientsMap = hashMapOf<String, AdbClient>()

    fun init(context: Context) {
        adbThread.start()
        adbHandler = Handler(adbThread.looper)
        adbCrypto = setupCrypto(context.filesDir, "pub.key", "priv.key")
    }

    fun release() {
        clientsMap.values.forEach {
            if (it.getStatus() == AdbStatus.CONNECTED) {
                it.disconnect()
            }
            it.release()
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

    fun connect(ip: String, port: Int) {
        var client = getClient(ip)
        if (client == null) {
            client = AdbClient(ip, port, adbCrypto, this)
            addClient(ip, client)
        }
        client.connect()
    }

    fun disconnect(ip: String) {
        getClient(ip)?.disconnect()
    }

    fun sendCommand(ip: String, command: String) {
        if (command.isEmpty()) {
            Log.e(TAG, "sendCommand cmd is empty")
            return
        }
        getClient(ip)?.sendCommand(command)
    }

    fun pushFile(ip: String, inputStream: InputStream, remotePath: String) {
        getClient(ip)?.pushFile(inputStream, remotePath)
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

    fun notifyEvent(ip: String, type: AdbEventType, subType: Any?, param: Any?) {
        adbHandler?.post {
            eventListeners.forEach {
                it.onAdbEvent(AdbEvent(ip, type, subType, param))
            }
        }
    }

    interface AdbEventListener {
        fun onAdbEvent(adbEvent: AdbEvent)
    }
}