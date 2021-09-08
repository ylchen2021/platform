package com.boostvision.platform.media.miracast.rtsp

import android.util.Log
import java.net.SocketException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class Request {
    var method: String? = null
    var uri: String? = null
    var headers = HashMap<String, String>()

    companion object {
        // Parse method & uri
        val regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP", Pattern.CASE_INSENSITIVE)

        // Parse a request header
        val rexegHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE)

        /** Parse the method, uri & headers of a RTSP request  */
        fun parse(lines: List<String>): Request {
            val request = Request()
            var matcher = regexMethod.matcher(lines[0])
            matcher.find()
            request.method = matcher.group(1)
            request.uri = matcher.group(2)

            for (i in 1..lines.size) {
                if (lines[i].length > 3) {
                    matcher = rexegHeader.matcher(lines[i])
                    matcher.find()
                    request.headers[matcher.group(1).toLowerCase(Locale.US)] = matcher.group(2)
                }
            }
            // It's not an error, it's just easier to follow what's happening in logcat with the request in red
            Log.e(RtspServer.TAG, request.method + " " + request.uri)
            return request
        }
    }
}