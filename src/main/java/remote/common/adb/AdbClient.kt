package remote.common.adb

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbCrypto
import com.cgutman.adblib.AdbStream
import remote.common.utils.ByteUtils
import remote.common.utils.Logger
import java.io.IOException
import java.io.InputStream
import java.lang.StringBuilder
import java.net.Socket
import java.nio.charset.Charset

class AdbClient(private val targetIp: String, private val targetPort: Int, private val adbCrypto: AdbCrypto?, private val controller: AdbController) {
    companion object {
        private const val TAG = "AdbClient"
        private const val MSG_CONNECT = 1
        private const val MSG_DISCONNECT = 2
        private const val MSG_COMMAND = 3
        private const val MSG_PUSH = 4
        var id = 1
    }
    private var adbShellStream: AdbStream? = null
    private var adbSyncStream: AdbStream? = null
    private var adbConnection: AdbConnection? = null
    private var status = AdbStatus.DISCONNECTED
    private var socket: Socket? = null
    private var adbThread: HandlerThread = HandlerThread("adb_client_thread_${id++}")
    private var adbHandler: Handler? = null

    init {
        adbThread.start()
        adbHandler = Handler(adbThread.looper) {
            when (it.what) {
                MSG_CONNECT -> {
                    handleConnect()
                }
                MSG_DISCONNECT -> {
                    handleDisconnect()
                }
                MSG_COMMAND -> {
                    val command = it.obj as String
                    handleCommand(command)
                }
                MSG_PUSH -> {
                    var param = it.obj as PushParam
                    handlePush(param.inputStream, param.remotePath)
                }
            }
            true
        }
    }

    fun connect() {
        var msg = Message.obtain()
        msg.what = MSG_CONNECT
        adbHandler?.sendMessage(msg)
    }

    fun disconnect() {
        var msg = Message.obtain()
        msg.what = MSG_DISCONNECT
        adbHandler?.sendMessage(msg)
    }

    fun release() {
        adbHandler?.removeCallbacksAndMessages(null)
        adbThread.looper.quit()
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

    private fun handleConnect() {
        changeStatus(AdbStatus.CONNECTING)
        Thread {
            try {
                socket = Socket(targetIp, targetPort)
                adbConnection = AdbConnection.create(socket, adbCrypto)
                adbConnection?.connect()
                adbShellStream = adbConnection?.open("shell:")
                changeStatus(AdbStatus.CONNECTED)
            } catch (e: Exception) {
                e.printStackTrace()
                var errorMsg = "connect error!"
                Log.i(TAG, errorMsg, e)
                if (!isDisconnected()) {
                    notifyEvent(AdbEventType.ERROR, AdbErrorType.CONNECT_FAILED, errorMsg)
                    disconnect()
                }
            }

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
                    if (!isDisconnected()) {
                        notifyEvent(AdbEventType.ERROR, AdbErrorType.CLOSED, "connection closed")
                        disconnect()
                    }
                }
            }
        }.start()
    }

    private fun notifyEvent(type: AdbEventType, subType: Any?, param: Any?) {
        controller.notifyEvent(targetIp, type, subType, param)
    }

    private fun changeStatus(adbStatus: AdbStatus) {
        status = adbStatus
        notifyEvent(AdbEventType.STATUS, adbStatus, null)
    }

    private fun isDisconnected(): Boolean {
        return status == AdbStatus.DISCONNECTED
    }

    private fun handleDisconnect() {
        try {
            if (adbShellStream != null) {
                adbShellStream?.close()
                adbShellStream = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            if (adbSyncStream != null) {
                adbSyncStream?.close()
                adbSyncStream = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            if (adbConnection != null) {
                adbConnection?.close()
                adbConnection = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            if (socket != null) {
                socket?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        changeStatus(AdbStatus.DISCONNECTED)
    }

    fun getStatus(): AdbStatus {
        return status
    }

    private fun handleCommand(command: String) {
        if (isDisconnected()) {
            return
        }
        adbShellStream?.write(command.toByteArray())
    }

    private fun handlePush(inputStream: InputStream, remotePath: String) {
        if (isDisconnected()) {
            notifyEvent(AdbEventType.FILE_PUSHED, null, false)
            return
        }
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
            notifyEvent(AdbEventType.ERROR, AdbErrorType.CLOSED, "push failed")
            disconnect()
            e.printStackTrace()
            notifyEvent(AdbEventType.FILE_PUSHED, null, false)
        }
        notifyEvent(AdbEventType.FILE_PUSHED, null, result)
    }

    data class PushParam(
        var inputStream: InputStream,
        var remotePath: String
    )
}