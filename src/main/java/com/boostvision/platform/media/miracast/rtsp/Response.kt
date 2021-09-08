package com.boostvision.platform.media.miracast.rtsp

import android.util.Log
import java.io.BufferedReader
import java.net.SocketException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class Response(private val mRequest: Request? = null) {
    var status = 0
    var headers = HashMap<String, String>()

    companion object {
        val regexStatus = Pattern.compile("RTSP/\\d.\\d (\\d+) (\\w+)", Pattern.CASE_INSENSITIVE)

        // Parses a request header
        val rexegHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE)

        // Parses a Session header
        val rexegSession = Pattern.compile("(\\d+)", Pattern.CASE_INSENSITIVE)

        // Parses a Transport header
        val rexegTransport = Pattern.compile("client_port=(\\d+)-(\\d+).+server_port=(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE)


        fun parse(lines: List<String>): Response {
            val response = Response()
            var matcher: Matcher
            // Parsing request method & URI
            if (lines == null) throw SocketException("Connection lost")
            matcher = regexStatus.matcher(lines[0])
            matcher.find()
            response.status = matcher.group(1).toInt()

            // Parsing headers of the request
            for (i in 1..lines.size) {
                if (lines[i].length > 3) {
                    matcher = rexegHeader.matcher(lines[i])
                    matcher.find()
                    response.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2))
                }
            }


            Log.d(RtspServer.TAG, "Response from server: " + response.status)
            return response
        }
    }
}