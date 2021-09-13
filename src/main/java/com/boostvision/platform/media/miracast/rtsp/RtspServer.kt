package com.boostvision.platform.media.miracast.rtsp


import android.util.Log
import com.boostvision.platform.media.miracast.rtp.RtpSession
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket


class RtspServer(private val iface: String){
    companion object {
        const val TAG = "RtspServer"
    }

    val TAG = "RtspServer"

    private var listenerThread: ListenerThread? = ListenerThread(iface)

    private var rtspSession: RtspSession? = null
    private var rtpSession: RtpSession? = null

    fun startListening() {
        listenerThread = ListenerThread(iface)
        listenerThread?.run()
    }

    fun stop() {
        if (listenerThread != null) {
            try {
                listenerThread?.kill()
            } catch (e: Exception) {
            } finally {
                listenerThread = null
            }
        }
    }

    inner class ListenerThread(iface: String) : Thread(), Runnable {
        private var mServer: ServerSocket? = null
        private var iface = iface
        private var localAddr: InetAddress
        private var port:Int

        init {
            var addressAndPort = iface.split(":")
            localAddr = InetAddress.getByName(addressAndPort[0])
            port = addressAndPort[1].toInt()
        }

        override fun run() {
            Log.i(TAG, "RTSP server listening on port $port")

            mServer = ServerSocket(port, 0, localAddr)
            while (true) {
                try {
                    var clientSocket = mServer!!.accept()
                    rtspSession = RtspSession(clientSocket)
                    rtspSession?.startNegotiation()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun kill() {
            try {
                mServer!!.close()
            } catch (e: IOException) {
            }
            try {
                this.join()
            } catch (ignore: InterruptedException) {
            }
        }
    }
}