package com.boostvision.platform.media.miracast.rtsp

import android.util.Log
import com.boostvision.platform.utils.DeviceUtils
import com.boostvision.platform.utils.TimeUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.*
import java.util.*
import java.util.regex.Pattern


class RtspServer(private val port: Int){

    companion object {
        const val TAG = "RtspServer"
    }

    enum class Progress {
        Listening,
        M1_sent,
        M3_sent,
        M4_sent,
        M5_sent,
        M6_received,
        M7_received,
    }

    val TAG = "RtspServer"

    /** The server name that will appear in responses.  */

    /** Port already in use.  */
    val ERROR_BIND_FAILED = 0x00

    /** A stream could not be started.  */
    val ERROR_START_FAILED = 0x01

    /** Streaming started.  */
    val MESSAGE_STREAMING_STARTED = 0X00

    /** Streaming stopped.  */
    val MESSAGE_STREAMING_STOPPED = 0X01

    var RTPPort = -1// extracted from "wfd_client_rtp_ports"


    protected var mSessions: WeakHashMap<Session, Any> = WeakHashMap<Session, Any>(2)

    private var mListenerThread = ListenerThread(port)
    private val mListeners = LinkedList<CallbackListener>()

    interface CallbackListener {
        fun onError(server: RtspServer?, e: Exception?, error: Int)
        fun onMessage(server: RtspServer?, message: Int)
    }

    fun addCallbackListener(listener: CallbackListener) {
        synchronized(mListeners) {
            if (!mListeners.isEmpty()) {
                for (cl in mListeners) {
                    if (cl === listener) return
                }
            }
            mListeners.add(listener)
        }
    }

    fun removeCallbackListener(listener: CallbackListener) {
        synchronized(mListeners) { mListeners.remove(listener) }
    }

    fun start() {
        mListenerThread.start()
    }

    fun stop() {
        if (mListenerThread != null) {
            try {
                mListenerThread.kill()
                for (session in mSessions.keys) {
                    if (session != null && session.isStreaming()) {
                        session.stop()
                    }
                }
            } catch (e: Exception) {
            } finally {
                mListenerThread = null
            }
        }
    }

    /** Returns whether or not the RTSP server is streaming to some client(s).  */
    fun isStreaming(): Boolean {
        for (session in mSessions.keys) {
            if (session != null && session.isStreaming()) {
                return true
            }
        }
        return false
    }

    /** Returns the bandwidth consumed by the RTSP server in bits per second.  */
    fun getBitrate(): Long {
        var bitrate: Long = 0
        for (session in mSessions.keys) {
            if (session != null && session.isStreaming()) {
                bitrate += session.getBitrate()
            }
        }
        return bitrate
    }

    protected fun postMessage(id: Int) {
        synchronized(mListeners) {
            if (!mListeners.isEmpty()) {
                for (cl in mListeners) {
                    cl.onMessage(this, id)
                }
            }
        }
    }

    protected fun postError(exception: Exception?, id: Int) {
        synchronized(mListeners) {
            if (!mListeners.isEmpty()) {
                for (cl in mListeners) {
                    cl.onError(this, exception, id)
                }
            }
        }
    }

    /**
     * By default the RTSP uses [UriParser] to parse the URI requested by the client
     * but you can change that behavior by override this method.
     * @param uri The uri that the client has requested
     * @param client The socket associated to the client
     * @return A proper session
     */
    @Throws(IllegalStateException::class, IOException::class)
    protected fun handleRequest(uri: String?, client: Socket): Session? {
        val session: Session = UriParser.parse(uri)
        session.setOrigin(client.localAddress.hostAddress)
        if (session.getDestination() == null) {
            session.setDestination(client.inetAddress.hostAddress)
        }
        return session
    }

    class ListenerThread(port: Int) : Thread(), Runnable {
        private var mServer: ServerSocket
        init {
            try {
                mServer = ServerSocket(port)
                start()
            } catch (e: BindException) {
                Log.e(TAG, "Port already in use !")
                postError(e, ERROR_BIND_FAILED)
                throw e
            }
        }

        override fun run() {
            Log.i(TAG, "RTSP server listening on port " + mServer.localPort)
            var clientSocket = mServer.accept()
            RtspThread(clientSocket).start()
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

    // One thread per client
    class RtspThread(client: Socket) : Thread(), Runnable {
        private val mClient = client
        private val mOutput: OutputStream = client.getOutputStream()
        private val mInput: BufferedReader = BufferedReader(InputStreamReader(client.getInputStream()))

        // Each client has an associated session
        companion object {
            private var playbackSessionTimeoutSecs = 30
        }
        private val mSession = Session()
        private var progress = Progress.Listening
        private var cseq: Int = 0

        private var mChosenRTPPort: Int = -1
        private var audioSupport = false
        private var mUsingPCMAudio = false
        private var playbackSessionID = -1

        override fun run() {
            Log.i(TAG, "Connection from " + mClient.inetAddress.hostAddress)
            sendM1Request()
            while (!interrupted()) {
                var lines = mInput.readLines()
                try {
                    if (lines.isEmpty()) {
                        throw SocketException("Client disconnected")
                    }
                    if (lines[0].startsWith("RTSP")) {
                        processResponse(Response.parse(lines))
                    } else {
                        processRequest(Request.parse(lines))
                    }
                } catch (e: SocketException) {
                    // Client has left
                    break
                } catch (e: Exception) {
                    // We don't understand the request :/
                }
            }

            // Streaming stops when client disconnects
            val streaming: Boolean = isStreaming()
            mSession.syncStop()
            if (streaming && !isStreaming()) {
                postMessage(MESSAGE_STREAMING_STOPPED)
            }
            mSession.release()
            try {
                mClient.close()
            } catch (ignore: IOException) {
            }
            Log.i(TAG, "Client disconnected")
        }

        private fun processResponse(response: Response) {
            when (progress) {
                Progress.M1_sent ->
                    handleM1Response(response)
                Progress.M3_sent ->
                    handleM3Response(response)
                Progress.M4_sent ->
                    handleM4Response(response)
                Progress.M5_sent ->
                    handleM5Response(response)
                else -> {
                    handleOtherResponse(response)
                }
            }
        }

        private fun processRequest(request: Request) {
            when (request.method) {
                "OPTIONS" ->
                    handleOptionsRequest(request)
                "SETUP" ->
                    handleSetupRequest(request)
                else ->
                    sendErrorResponse("405 Method Not Allowed", cseq)
            }
        }

        private fun handleOptionsRequest(request: Request) {
            if (request.method != "OPTIONS"){
                return
            }

            var response = StringBuilder("RTSP/1.0 200 OK\r\n")
            response.append(commonResponseParams(cseq))
            response.append("Public: org.wfa.wfd1.0, SETUP, TEARDOWN, PLAY, PAUSE, GET_PARAMETER, SET_PARAMETER\r\n\r\n")
            outputSend(request.toString())

            sendM3Request()
            progress = Progress.M3_sent
        }

        private fun handleSetupRequest(request: Request) {
            if (playbackSessionID != -1) {
                sendErrorResponse( "400 Bad Request", cseq)
                return
            }

            var transport = request.headers["transport"]
            if (transport == null) {
                sendErrorResponse( "400 Bad Request", cseq)
            }


        }

        private fun sendErrorResponse(errorDetail: String, cseq: Int) {
            var response = StringBuilder("RTSP/1.0 $errorDetail\r\n")
            response.append(commonResponseParams(cseq))
            response.append("\r\n")
            outputSend(response.toString())
        }

        private fun sendM1Request() {
            var response = StringBuilder("OPTIONS * RTSP/1.0\r\n")
            response.append(commonResponseParams(cseq))
            response.append("Require: org.wfa.wfd1.0\r\n")
            outputSend(response.toString())
        }

        private fun sendM3Request() {
            var body = """wfd_content_protection
            wfd_video_formats
            wfd_audio_codecs
            wfd_client_rtp_ports
            """;

            var request = StringBuilder("GET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\r\n")
            request.append(commonResponseParams(cseq))

            request.append("Content-Type: text/parameters\r\n");
            request.append("Content-Length: ${body.length}}\r\n")
            request.append(body)
            outputSend(request.toString())
        }

        private fun sendM4Request() {
            var body = """wfd_video_formats: 28 00 02 02 00000020 00000000 00000000 00 0000 0000 00 none none\r\n
            wfd_audio_codecs: ${if (mUsingPCMAudio) "LPCM 00000002 00" else "AAC 00000001 00"}\r\n
            wfd_presentation_URL: rtsp://${mClient.localAddress}/wfd1.0/streamid=0 none\r\n
            wfd_client_rtp_ports: RTP/AVP/UDP;unicast $mChosenRTPPort 0 mode=play\r\n"""

            var request = StringBuilder("SET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\r\n")
            request.append(commonResponseParams(cseq))

            request.append("Content-Type: text/parameters\r\n")
            request.append("Content-Length: ${body.length}\r\n")
            request.append("\r\n")
            request.append(body)

            outputSend(request.toString())
        }

        private fun sendTriggerRequest(triggerType: String) {
            var body = "wfd_trigger_method: $triggerType\r\n"

            var request = StringBuilder("SET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\n")
            request.append(commonResponseParams(cseq))

            request.append("Content-Type: text/parameters\r\n")
            request.append("Content-Length: ${body.length}\r\n")
            request.append("\r\n")
            request.append(body)

            outputSend(request.toString())
        }

        private fun outputSend(content: String) {
            mOutput.write(content.toByteArray(charset("UTF-8")))
            mOutput.flush()
        }

        private fun commonResponseParams(cseq: Int, playbackSessionID: Int = -1): String {
            var commonResponse = StringBuilder()
            commonResponse.append("Date: ")
            commonResponse.append(TimeUtils.millis2String(System.currentTimeMillis(), "EEEE, dd MMM yyyy HH:mm:ss zzzz"));
            commonResponse.append("\r\n")
            commonResponse.append("Server: ${DeviceUtils.getName()}\r\n")
            commonResponse.append("User-Agent: ${DeviceUtils.getName()}\r\n");
            if (cseq >= 0) {
                commonResponse.append("CSeq: ${cseq}\r\n")
            }
            if (playbackSessionID >= 0) {
                commonResponse.append("Session: ${playbackSessionID};timeout=${playbackSessionTimeoutSecs}\r\n")
            }
            return commonResponse.toString()
        }

        private fun handleM1Response(response: Response) {

        }

        private fun handleM4Response(response: Response) {
            if (response.status != 200) {
                return
            }
            sendTriggerRequest("SETUP")
            progress = Progress.M5_sent
        }

        private fun handleM5Response(response: Response) {
            if (response.status != 200) {
                return
            }
        }

        private fun handleM3Response(response: Response) {
            if (response.status != 200){
                return
            }

            var wfd_client_rtp_ports = response.headers["wfd_client_rtp_ports"]
            val portRegex = Pattern.compile("RTP/AVP/UDP;unicast (\\d+) (\\d+) mode=play", Pattern.CASE_INSENSITIVE)
            var matcher = portRegex.matcher(wfd_client_rtp_ports)
            matcher.find()
            mChosenRTPPort = matcher.group(1).toInt()

            var audio_codecs = response.headers["wfd_audio_codecs"]
            if (audio_codecs == null || audio_codecs == "none") {
                audioSupport = false
            } else {
                var supportAAC = audio_codecs.substring(audio_codecs.indexOf("AAC ")+4, audio_codecs.indexOf("AAC ")+12).toInt() == 1
                var supportLPCM = audio_codecs.substring(audio_codecs.indexOf("LPCM ")+5, audio_codecs.indexOf("AAC ")+13).toInt() == 2
                if (supportAAC) {
                    mUsingPCMAudio = false
                } else if (supportLPCM){
                    mUsingPCMAudio = true
                }
            }

            sendM4Request()
            progress = Progress.M4_sent
        }

        private fun handleOtherResponse(response: Response) {

        }
    }
}