package remote.common.adb

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
        const val TAG = "AdbClient"
    }
    private var adbShellStream: AdbStream? = null
    private var adbSyncStream: AdbStream? = null
    private var adbConnection: AdbConnection? = null
    private var status = AdbStatus.DISCONNECTED
    private var socket: Socket? = null

    fun connect() {
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
                    controller.sendEvent(AdbEventType.ERROR, AdbErrorType.CONNECT_FAILED, errorMsg)
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
                        controller.sendEvent(AdbEventType.RESPONSE, null, fullResponse)
                        responseBuilder = StringBuilder()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (!isDisconnected()) {
                        controller.sendEvent(AdbEventType.ERROR, AdbErrorType.CLOSED, "connection closed")
                        disconnect()
                    }
                }
            }
        }.start()
    }

    private fun changeStatus(adbStatus: AdbStatus) {
        status = adbStatus
        controller.sendEvent(AdbEventType.STATUS, adbStatus, targetIp)
    }

    private fun isDisconnected(): Boolean {
        return status == AdbStatus.DISCONNECTED
    }

    fun disconnect() {
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

    fun write(command: String) {
        if (isDisconnected()) {
            return
        }
        adbShellStream?.write(command.toByteArray())
    }

    fun push(inputStream: InputStream, remotePath: String): Boolean {
        if (isDisconnected()) {
            return false
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
            controller.sendEvent(AdbEventType.ERROR, AdbErrorType.CLOSED, "push failed")
            disconnect()
            e.printStackTrace()
            return false
        }
        return result
    }
}