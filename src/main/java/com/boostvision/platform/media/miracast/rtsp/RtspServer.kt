package com.boostvision.platform.media.miracast.rtsp


import android.util.Log
import com.boostvision.platform.media.miracast.rtp.RtpSession
import java.io.IOException
import java.net.*
import java.util.*


class RtspServer(private val port: Int){
    companion object {
        const val TAG = "RtspServer"
    }

    val TAG = "RtspServer"

    private var listenerThread: ListenerThread? = ListenerThread(port)

    private var rtspSession: RtspSession? = null
    private var rtpSession: RtpSession? = null

    fun start() {
        if (listenerThread != null) {
            try {
                listenerThread?.kill()
            } catch (e: Exception) {
            } finally {
                listenerThread = null
            }
        }
        listenerThread = ListenerThread(port)
        listenerThread?.start()
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

    inner class ListenerThread(port: Int) : Thread(), Runnable {
        private var mServer: ServerSocket
        init {
            try {
                mServer = ServerSocket(port)
                start()
            } catch (e: BindException) {
                Log.e(TAG, "Port already in use !")
                throw e
            }
        }

        override fun run() {
            Log.i(TAG, "RTSP server listening on port " + mServer.localPort)
            var clientSocket = mServer.accept()
            rtspSession = RtspSession(clientSocket)
            rtspSession?.start()
        }

        fun kill() {
            try {
                mServer.close()
            } catch (e: IOException) {
            }
            try {
                this.join()
            } catch (ignore: InterruptedException) {
            }
        }
    }
}