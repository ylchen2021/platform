package com.boostvision.platform.media

import android.content.Context
import android.net.wifi.WifiManager
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class StreamWebServer: NanoHTTPD(PORT) {

    companion object {
        const val TAG = "StreamWebServer"
        const val PORT = 8088

        fun getHttpUrl(context: Context, filePath: String): String {
            var ip = getLocalIPAddress(context)
            return "http://${ip}:${PORT}${filePath}"
        }

        private fun getLocalIPAddress(context: Context): String? {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager != null) {
                val wifiInfo = wifiManager.connectionInfo
                val sb = StringBuilder()
                sb.append(wifiInfo.ipAddress and 0xFF).append(".")
                sb.append(wifiInfo.ipAddress shr 8 and 0xFF).append(".")
                sb.append(wifiInfo.ipAddress shr 16 and 0xFF).append(".")
                sb.append(wifiInfo.ipAddress shr 24 and 0xFF)
                return sb.toString()
            }
            return ""
        }
    }

    override fun serve(uri: String, method: Method?, headers: Map<String, String>, params: Map<String?, String?>?, files: Map<String?, String?>?): Response? {
        var fileType = MediaType.getFileType(uri)
        var range: String? = null
        for (key in headers.keys) {
            if ("range" == key) {
                range = headers[key]
            }
        }
        return range?.let { getPartialResponse(fileType?.mimeType?:"", it, uri) } ?: getFullResponse(fileType?.mimeType?:"", uri)
    }

    private fun getFullResponse(mimeType: String, filePath: String): Response? {
        val fis = FileInputStream(filePath)
        return newChunkedResponse(Response.Status.OK, mimeType, fis)
    }

    private fun getPartialResponse(mimeType: String, rangeHeader: String, filePath: String): Response? {
        val file = File(filePath)
        val rangeValue = rangeHeader.trim().substring("bytes=".length)
        val fileLength: Long = file.length()
        val start: Long
        var end: Long

        if (rangeValue.endsWith("-")) {
            start = rangeValue.substringBefore("-").toLong()
            end = fileLength-1
        } else {
            val range = rangeValue.split("-").toTypedArray()
            start = range[0].toLong()
            end = if (range.size > 1 && range[1].isNotEmpty()) range[1].toLong() else fileLength - 1
        }

        if (end > fileLength - 1) {
            end = fileLength - 1
        }
        return if (start <= end) {
            val contentLength = end - start + 1
            var fileInputStream = FileInputStream(file)
            fileInputStream.skip(start)
            val response = newChunkedResponse(Response.Status.PARTIAL_CONTENT, mimeType, fileInputStream)
            response.addHeader("Content-Length", contentLength.toString() + "")
            response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
            response.addHeader("Content-Type", mimeType)
            response
        } else {
            null
        }
    }
}