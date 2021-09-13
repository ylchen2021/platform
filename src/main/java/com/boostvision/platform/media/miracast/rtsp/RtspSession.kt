package com.boostvision.platform.media.miracast.rtsp

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.boostvision.platform.media.miracast.rtp.RtpSession
import com.boostvision.platform.media.miracast.rtp.TransportMode
import com.boostvision.platform.utils.DeviceUtils
import com.boostvision.platform.utils.Logger
import com.boostvision.platform.utils.TimeUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

class RtspSession(private val mClient: Socket) {
    companion object {
        private const val TAG = "RtspSession"
        private var playbackSessionTimeoutSecs = 30
        private var playbackSessionTimeoutUs = playbackSessionTimeoutSecs * 1000000L
    }

    private var mOutput: OutputStream = mClient.getOutputStream()
    private var mInput: BufferedReader = BufferedReader(InputStreamReader(mClient.getInputStream()))
    private var responseHandlers: HashMap<Int, (Response)->ErrorCode> = hashMapOf()
    private var rtpSession: RtpSession? = null
    private var mNextCSeq: Int = 1
    private var mChosenRTPPort: Int = -1
    private var audioSupport = false
    private var mUsingPCMAudio = false
    private var playbackSessionID = -1
    private var rtpPort = -1
    private var rtcpPort = -1
    private var transportMode = TransportMode.TRANSPORT_UDP
    private var handlerThread = HandlerThread("rtsp_session")
    private var handler: Handler

    private val mListeners = arrayListOf<CallbackListener>()

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    interface CallbackListener {
        fun onError(errorCode: ErrorCode)
    }

    fun addCallbackListener(listener: CallbackListener) {
        synchronized(mListeners) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener)
            }
        }
    }

    fun removeCallbackListener(listener: CallbackListener) {
        synchronized(mListeners) {
            if (mListeners.contains(listener)) {
                mListeners.remove(listener)
            }
        }
    }

    fun notifyError(errorCode: ErrorCode) {
        mListeners.forEach {
            it.onError(errorCode)
        }
    }

    fun startNegotiation() {
        workingThread.start()
    }

    private var workingThread = object: Thread() {
        override fun run() {
            Log.i(RtspServer.TAG, "Connection from " + mClient.inetAddress.hostAddress)
            var err = ErrorCode.OK
            sendM1Request()
            while (!interrupted()) {
                var lines = mInput.readLines()
                try {
                    Logger.d(TAG, "reading from socket=${lines}")
                    if (lines.isEmpty()) {
                        throw SocketException("Client disconnected")
                    }
                    if (lines[0].startsWith("RTSP")) {
                        var response = Response()
                        err = Response.parse(lines, response)
                        if (err != ErrorCode.OK) {
                            sendErrorResponse("400 Bad Request", response.cseq)
                            return
                        }
                        err = processResponse(response)
                    } else {
                        var request = Request()
                        err = Request.parse(lines, request)
                        if (err != ErrorCode.OK) {
                            sendErrorResponse("400 Bad Request", request.cseq)
                            return
                        }
                        err = processRequest(request)
                    }
                } catch (e: SocketException) {
                    // Client has left
                    break
                } catch (e: Exception) {
                    // We don't understand the request :/
                }
                if (err != ErrorCode.OK) {
                    notifyError(err)
                }
            }

            Log.i(RtspServer.TAG, "Client disconnected")
        }
    }

    private fun processResponse(response: Response): ErrorCode {
        var responseFunc = responseHandlers[response.cseq]
        return responseFunc?.invoke(response)?:ErrorCode.FAILED
    }

    private fun processRequest(request: Request): ErrorCode {
        var err = ErrorCode.OK
        when (request.method) {
            "OPTIONS" ->
                err = handleOptionsRequest(request)
            "SETUP" ->
                err = handleSetupRequest(request)
            "PLAY" ->
                err = handlePlayRequest(request)
            else -> {
                sendErrorResponse("405 Method Not Allowed", request.cseq)
                err = ErrorCode.ERROR_UNSUPPORTED
            }
        }
        return err
    }

    private fun handleOptionsRequest(request: Request): ErrorCode {
        var response = StringBuilder("RTSP/1.0 200 OK\r\n")
        response.append(commonResponseParams(request.cseq))
        response.append("Public: org.wfa.wfd1.0, SETUP, TEARDOWN, PLAY, PAUSE, GET_PARAMETER, SET_PARAMETER\r\n\r\n")
        var err = outputSend(request.toString())

        if (err == ErrorCode.OK) {
            err = sendM3Request()
        }
        return err
    }

    private fun handleSetupRequest(request: Request): ErrorCode {
        if (playbackSessionID != -1) {
            sendErrorResponse( "400 Bad Request", request.cseq)
            return ErrorCode.ERROR_MALFORMED
        }

        var transport = request.headers["transport"]
        if (transport == null) {
            sendErrorResponse( "400 Bad Request", request.cseq)
            return ErrorCode.ERROR_MALFORMED
        }

        if (transport.containsKey("RTP/AVP/TCP")) {
            if (transport.containsKey("interleaved")) {
                if (matchPort(true, transport["interleaved"]?:"")) {
                    transportMode = TransportMode.TRANSPORT_TCP_INTERLEAVED
                }
            }
            if (transportMode != TransportMode.TRANSPORT_TCP_INTERLEAVED) {
                if (!transport.containsKey("client_port") || !matchPort(false, transport["client_port"]?:"")) {
                    sendErrorResponse("400 Bad Request", request.cseq)
                    return ErrorCode.ERROR_MALFORMED
                }
            }
        } else if ((transport.containsKey("RTP/AVP") || transport.containsKey("RTP/AVP/UDP")) && transport.containsKey("client_port")) {
            if (!matchPort(false, transport["client_port"]?:"")) {
                sendErrorResponse("400 Bad Request", request.cseq)
                return ErrorCode.ERROR_MALFORMED
            }
        } else if (transport.containsKey("RTP/AVP/UDP") && transport.containsKey("unicast") && !transport.containsKey("client_port")) {
            rtpPort = 19000
            rtcpPort = -1
        } else {
            sendErrorResponse("461 Unsupported Transport", request.cseq)
            return ErrorCode.ERROR_UNSUPPORTED
        }

        playbackSessionID = Random().nextInt()
        if (request.uri?.startsWith("rtsp://") == false) {
            sendErrorResponse("400 Bad Request", request.cseq)
            return ErrorCode.ERROR_MALFORMED
        } else if (request.uri?.endsWith("/wfd1.0/streamid=0") == false) {
            sendErrorResponse("404 Not found", request.cseq)
            return ErrorCode.ERROR_MALFORMED
        }

        rtpSession = RtpSession(mClient.remoteSocketAddress.toString(), rtpPort, rtcpPort, transportMode)
        var response = StringBuilder("RTSP/1.0 200 OK\r\n")
        response.append(commonResponseParams(request.cseq))

        if (transportMode == TransportMode.TRANSPORT_TCP_INTERLEAVED) {
            response.append("Transport: RTP/AVP/TCP;interleaved=${rtpPort}-${rtcpPort};\r\n")
        } else {
            var serverRtp = rtpSession?.getServerPort()?:0
            var transportString = "UDP";
            if (transportMode == TransportMode.TRANSPORT_TCP) {
                transportString = "TCP";
            }
            if (rtcpPort >= 0) {
                response.append("Transport: RTP/AVP/${transportString};unicast;client_port=${rtpPort}-${rtcpPort};server_port=${serverRtp}-${serverRtp+1}\r\n")
            } else {
                response.append("Transport: RTP/AVP/${transportString};unicast;client_port=${rtpPort};server_port=${serverRtp}\r\n")
            }
        }
        response.append("\r\n")
        var error = outputSend(request.toString())

        handler.postDelayed({

        },playbackSessionTimeoutSecs - 5000000L)

        return error
    }

    private fun matchPort(matchBoth: Boolean, transport: String): Boolean {
        if (transport.contains("-")) {
            val portRegex = Pattern.compile("(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE)
            var matcher = portRegex.matcher(transport)
            matcher.find()
            if (matcher.groupCount() < 2) {
                return false
            }
            rtpPort = (matcher.group(1) ?: "-1").toInt()
            rtcpPort = (matcher.group(2) ?: "-1").toInt()
        } else {
            if (matchBoth) {
                return false
            }
            rtpPort = transport.toInt()
            rtcpPort = -1
        }
        return true
    }

    private fun handlePlayRequest(request: Request): ErrorCode{
        if (!request.headers.containsKey("Session") && playbackSessionID == -1) {
            sendErrorResponse("454 Session Not Found", request.cseq)
            return ErrorCode.ERROR_MALFORMED
        }

        var response = StringBuilder("RTSP/1.0 200 OK\r\n")
        response.append(commonResponseParams(request.cseq))
        response.append("Range: npt=now-\r\n");
        response.append("\r\n")
        var err = outputSend(response.toString())
        return err
    }

    private fun sendErrorResponse(errorDetail: String, cseq: Int) {
        var response = StringBuilder("RTSP/1.0 $errorDetail\r\n")
        response.append(commonResponseParams(cseq))
        response.append("\r\n")
        outputSend(response.toString())
    }

    private fun sendM1Request(): ErrorCode {
        var response = StringBuilder("OPTIONS * RTSP/1.0\r\n")
        response.append(commonResponseParams(mNextCSeq))
        response.append("Require: org.wfa.wfd1.0\r\n")
        var err = outputSend(response.toString())

        if (err == ErrorCode.OK) {
            responseHandlers[mNextCSeq] = {
                handleM1Response(it)
            }
        }

        mNextCSeq++

        return err
    }

    private fun sendM3Request(): ErrorCode{
        var body = """wfd_content_protection
            wfd_video_formats
            wfd_audio_codecs
            wfd_client_rtp_ports
            """;

        var request = StringBuilder("GET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\r\n")
        request.append(commonResponseParams(mNextCSeq))

        request.append("Content-Type: text/parameters\r\n");
        request.append("Content-Length: ${body.length}}\r\n")
        request.append(body)
        var err = outputSend(request.toString())
        if (err == ErrorCode.OK) {
            responseHandlers[mNextCSeq] = {
                handleM3Response(it)
            }
        }

        mNextCSeq++

        return err
    }

    private fun sendM4Request(): ErrorCode {
        var body = """wfd_video_formats: 28 00 02 02 00000020 00000000 00000000 00 0000 0000 00 none none\r\n
            wfd_audio_codecs: ${if (mUsingPCMAudio) "LPCM 00000002 00" else "AAC 00000001 00"}\r\n
            wfd_presentation_URL: rtsp://${mClient.localAddress}/wfd1.0/streamid=0 none\r\n
            wfd_client_rtp_ports: RTP/AVP/UDP;unicast $mChosenRTPPort 0 mode=play\r\n"""

        var request = StringBuilder("SET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\r\n")
        request.append(commonResponseParams(mNextCSeq))

        request.append("Content-Type: text/parameters\r\n")
        request.append("Content-Length: ${body.length}\r\n")
        request.append("\r\n")
        request.append(body)

        var err = outputSend(request.toString())
        if (err == ErrorCode.OK) {
            responseHandlers[mNextCSeq] = {
                handleM4Response(it)
            }
        }

        mNextCSeq++

        return err
    }

    private fun sendTriggerRequest(triggerType: String): ErrorCode {
        var body = "wfd_trigger_method: $triggerType\r\n"

        var request = StringBuilder("SET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\n")
        request.append(commonResponseParams(mNextCSeq))

        request.append("Content-Type: text/parameters\r\n")
        request.append("Content-Length: ${body.length}\r\n")
        request.append("\r\n")
        request.append(body)

        var err = outputSend(request.toString())
        if (err == ErrorCode.OK) {
            responseHandlers[mNextCSeq] = {
                handleM5Response(it)
            }
        }

        mNextCSeq++

        return err
    }

    private fun sendM16Request() {
        var request = StringBuilder("GET_PARAMETER rtsp://localhost/wfd1.0 RTSP/1.0\r\n")
        request.append(commonResponseParams(mNextCSeq))
        request.append("Session: ${playbackSessionID}\r\n")
        request.append("\r\n")
        outputSend(request.toString())

        mNextCSeq++
    }

    private fun outputSend(content: String): ErrorCode {
        Logger.d(TAG, "writting to socket=${content}")
        try {
            mOutput.write(content.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
            return ErrorCode.FAILED
        }
        return ErrorCode.OK
    }

    private fun commonResponseParams(cseq: Int, playbackSessionID: Int = -1): String {
        var commonResponse = StringBuilder()
        commonResponse.append("Date: ")
        commonResponse.append(TimeUtils.millis2String(System.currentTimeMillis(), "EEEE, dd MMM yyyy HH:mm:ss zzzz"))
        commonResponse.append("\r\n")
        commonResponse.append("Server: ${DeviceUtils.getName()}\r\n")
        if (cseq >= 0) {
            commonResponse.append("CSeq: ${cseq}\r\n")
        }
        if (playbackSessionID >= 0) {
            commonResponse.append("Session: ${playbackSessionID};timeout=${playbackSessionTimeoutSecs}\r\n")
        }
        return commonResponse.toString()
    }

    private fun handleM1Response(response: Response): ErrorCode {
        if (response.status == 200) {
            return ErrorCode.OK
        }
        return ErrorCode.FAILED
    }

    private fun handleM4Response(response: Response): ErrorCode {
        if (response.status != 200) {
            return ErrorCode.FAILED
        }
        return sendTriggerRequest("SETUP")
    }

    private fun handleM5Response(response: Response): ErrorCode {
        if (response.status == 200) {
            return ErrorCode.OK
        }
        return ErrorCode.FAILED
    }

    private fun handleM3Response(response: Response): ErrorCode {
        if (response.status != 200){
            return ErrorCode.FAILED
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

        return sendM4Request()
    }

    private fun handleOtherResponse(response: Response) {

    }

    data class ResponseHandler(
        var cseq: Int,
        var handler: (Response)->Unit
    )
}