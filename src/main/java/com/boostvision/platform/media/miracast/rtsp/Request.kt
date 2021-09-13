package com.boostvision.platform.media.miracast.rtsp

import android.util.Log
import java.net.SocketException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.HashMap

class Request {
    var method: String? = null
    var uri: String? = null
    var cseq = -1
    var headers = HashMap<String, HashMap<String, String>>()

    companion object {
        // Parse method & uri
        val regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP", Pattern.CASE_INSENSITIVE)

        // Parse a request header
        val rexegHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE)

        /** Parse the method, uri & headers of a RTSP request  */
        fun parse(lines: List<String>, requestOut: Request): ErrorCode {
            var matcher = regexMethod.matcher(lines[0])
            matcher.find()
            requestOut.method = matcher.group(1)
            requestOut.uri = matcher.group(2)

            for (i in 1..lines.size) {
                if (lines[i].length > 3) {
                    matcher = rexegHeader.matcher(lines[i])
                    matcher.find()
                    if (matcher.group(1) == "CSeq") {
                        requestOut.cseq = matcher.group(2).toInt()
                        continue
                    }
                    var headerContentStr = matcher.group(2)
                    var headerContent = hashMapOf<String, String>()
                    requestOut.headers[matcher.group(1).toLowerCase(Locale.US)] = headerContent
                    var headerContentList = headerContentStr.split(";")
                    headerContentList.forEach {
                        if (it.contains("=")) {
                            val attribute = it.split("=")
                            headerContent[attribute[0]] = attribute[1]
                        } else {
                            headerContent[it] = ""
                        }
                    }
                }
            }
            // It's not an error, it's just easier to follow what's happening in logcat with the request in red
            Log.e(RtspServer.TAG, requestOut.method + " " + requestOut.uri)
            return ErrorCode.OK
        }
    }
}