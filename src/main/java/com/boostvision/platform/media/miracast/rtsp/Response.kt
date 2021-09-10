package com.boostvision.platform.media.miracast.rtsp

import android.util.Log
import java.io.BufferedReader
import java.net.SocketException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class Response(private val mRequest: Request? = null) {
    var status = 0
    var cseq = -1
    var headers = HashMap<String, String>()

    companion object {
        val regexStatus = Pattern.compile("RTSP/\\d.\\d (\\d+) (\\w+)", Pattern.CASE_INSENSITIVE)
        val rexegHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE)

        fun parse(lines: List<String>, responseOut: Response): ErrorCode {
            var matcher: Matcher
            // Parsing request method & URI
            if (lines == null) throw SocketException("Connection lost")
            matcher = regexStatus.matcher(lines[0])
            matcher.find()
            responseOut.status = matcher.group(1).toInt()

            // Parsing headers of the request
            for (i in 1..lines.size) {
                if (lines[i].length > 3) {
                    matcher = rexegHeader.matcher(lines[i])
                    matcher.find()
                    if (matcher.group(1) == "CSeq") {
                        responseOut.cseq = matcher.group(1).toInt()
                    } else {
                        responseOut.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2))
                    }
                }
            }


            Log.d(RtspServer.TAG, "Response from server: " + responseOut.status)
            return ErrorCode.OK
        }
    }
}