/*
 * Copyright (C) 2016 Viking Den <vikingden@live.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.boostvision.platform.media

import java.util.*

/**
 * Utility class for media file judge, base on android.media.MediaFile.java
 * author : Viking Den <vikingden></vikingden>@live.com>
 * time   : 2016/8/9 22:38
 */
object MediaType {
    // Audio file types
    val FILE_TYPE_MP3 = 1
    val FILE_TYPE_M4A = 2
    val FILE_TYPE_WAV = 3
    val FILE_TYPE_AMR = 4
    val FILE_TYPE_AWB = 5
    val FILE_TYPE_WMA = 6
    val FILE_TYPE_OGG = 7
    val FILE_TYPE_AAC = 8
    val FILE_TYPE_MKA = 9
    val FILE_TYPE_FLAC = 10
    private val FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3
    private val LAST_AUDIO_FILE_TYPE = FILE_TYPE_FLAC

    // MIDI file types
    val FILE_TYPE_MID = 11
    val FILE_TYPE_SMF = 12
    val FILE_TYPE_IMY = 13
    private val FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID
    private val LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY

    // Video file types
    val FILE_TYPE_MP4 = 21
    val FILE_TYPE_M4V = 22
    val FILE_TYPE_3GPP = 23
    val FILE_TYPE_3GPP2 = 24
    val FILE_TYPE_WMV = 25
    val FILE_TYPE_ASF = 26
    val FILE_TYPE_MKV = 27
    val FILE_TYPE_MP2TS = 28
    val FILE_TYPE_AVI = 29
    val FILE_TYPE_WEBM = 30
    private val FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4
    private val LAST_VIDEO_FILE_TYPE = FILE_TYPE_WEBM

    // More video file types
    val FILE_TYPE_MP2PS = 200
    private val FIRST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS
    private val LAST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS

    // Image file types
    val FILE_TYPE_JPEG = 31
    val FILE_TYPE_GIF = 32
    val FILE_TYPE_PNG = 33
    val FILE_TYPE_BMP = 34
    val FILE_TYPE_WBMP = 35
    val FILE_TYPE_WEBP = 36
    private val FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG
    private val LAST_IMAGE_FILE_TYPE = FILE_TYPE_WEBP

    // Playlist file types
    val FILE_TYPE_M3U = 41
    val FILE_TYPE_PLS = 42
    val FILE_TYPE_WPL = 43
    val FILE_TYPE_HTTPLIVE = 44
    private val FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U
    private val LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_HTTPLIVE

    // Drm file types
    val FILE_TYPE_FL = 51
    private val FIRST_DRM_FILE_TYPE = FILE_TYPE_FL
    private val LAST_DRM_FILE_TYPE = FILE_TYPE_FL

    // Other popular file types
    val FILE_TYPE_TEXT = 100
    val FILE_TYPE_HTML = 101
    val FILE_TYPE_PDF = 102
    val FILE_TYPE_XML = 103
    val FILE_TYPE_MS_WORD = 104
    val FILE_TYPE_MS_EXCEL = 105
    val FILE_TYPE_MS_POWERPOINT = 106
    val FILE_TYPE_ZIP = 107
    private val sFileTypeMap = HashMap<String, MediaFileType>()
    private val sMimeTypeMap = HashMap<String, Int>()
    fun addFileType(extension: String, fileType: Int, mimeType: String) {
        sFileTypeMap[extension] = MediaFileType(fileType, mimeType)
        sMimeTypeMap[mimeType] = fileType
    }

    /**
     * check is audio or not
     * @param fileType file type integer value
     * @return if is audio type , return true;otherwise , return false
     */
    fun isAudioFileType(fileType: Int): Boolean {
        return (fileType >= FIRST_AUDIO_FILE_TYPE &&
                fileType <= LAST_AUDIO_FILE_TYPE ||
                fileType >= FIRST_MIDI_FILE_TYPE &&
                fileType <= LAST_MIDI_FILE_TYPE)
    }

    /**
     * check is video or not
     * @param fileType file type integer value
     * @return if is video type , return true ; otherwise , return false
     */
    fun isVideoFileType(fileType: Int): Boolean {
        return ((fileType >= FIRST_VIDEO_FILE_TYPE &&
                fileType <= LAST_VIDEO_FILE_TYPE)
                || (fileType >= FIRST_VIDEO_FILE_TYPE2 &&
                fileType <= LAST_VIDEO_FILE_TYPE2))
    }

    /**
     * check is image or not
     * @param fileType file type integer value
     * @return if is image type , return true ; otherwise , return false ;
     */
    fun isImageFileType(fileType: Int): Boolean {
        return (fileType >= FIRST_IMAGE_FILE_TYPE &&
                fileType <= LAST_IMAGE_FILE_TYPE)
    }

    /**
     * check is playlist or not
     * @param fileType file type integer value
     * @return if is playlist type , return true ; otherwise , return false ;
     */
    fun isPlayListFileType(fileType: Int): Boolean {
        return (fileType >= FIRST_PLAYLIST_FILE_TYPE &&
                fileType <= LAST_PLAYLIST_FILE_TYPE)
    }

    /**
     * check is drm or not
     * @param fileType file type integer value
     * @return if is drm type , return true ; otherwise , return false ;
     */
    fun isDrmFileType(fileType: Int): Boolean {
        return (fileType >= FIRST_DRM_FILE_TYPE &&
                fileType <= LAST_DRM_FILE_TYPE)
    }

    /**
     * get file's extension by file' path
     * @param path file's path
     * @return MediaFileType if the given file extension exist , or null
     */
    fun getFileType(path: String): MediaFileType? {
        val lastDot = path.lastIndexOf('.')
        return if (lastDot < 0) null else sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase(Locale.ROOT))
    }

    /**
     * check the given mime type is mime type media or not
     * @param mimeType mime type to check
     * @return if the given mime type is mime type media,return true ;otherwise , false
     */
    fun isMimeTypeMedia(mimeType: String): Boolean {
        val fileType = getFileTypeForMimeType(mimeType)
        return (isAudioFileType(fileType) || isVideoFileType(fileType)
                || isImageFileType(fileType) || isPlayListFileType(fileType))
    }

    /**
     * generates a title based on file name
     * @param path file's path
     *
     * @return file'name without extension
     */
    fun getFileTitle(path: String): String {
        // extract file name after last slash
        var path = path
        var lastSlash = path.lastIndexOf('/')
        if (lastSlash >= 0) {
            lastSlash++
            if (lastSlash < path.length) {
                path = path.substring(lastSlash)
            }
        }
        // truncate the file extension (if any)
        val lastDot = path.lastIndexOf('.')
        if (lastDot > 0) {
            path = path.substring(0, lastDot)
        }
        return path
    }

    /**
     * get mine type integer value
     * @param mimeType mime type to get
     * @return return mime type value if exist ;or zero value if not exist
     */
    fun getFileTypeForMimeType(mimeType: String): Int {
        val value = sMimeTypeMap[mimeType]
        return (value?.toInt() ?: 0)
    }

    /**
     * get file's mime type base on path
     * @param path file path
     * @return return mime type if exist , or null
     */
    fun getMimeTypeForFile(path: String): String? {
        val mediaFileType = getFileType(path)
        return (mediaFileType?.mimeType)
    }

    class MediaFileType internal constructor(val fileType: Int, val mimeType: String)

    init {
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg")
        addFileType("MPGA", FILE_TYPE_MP3, "audio/mpeg")
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4")
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav")
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr")
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb")
        addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma")
        addFileType("OGG", FILE_TYPE_OGG, "audio/ogg")
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg")
        addFileType("OGA", FILE_TYPE_OGG, "application/ogg")
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac")
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac-adts")
        addFileType("MKA", FILE_TYPE_MKA, "audio/x-matroska")
        addFileType("MID", FILE_TYPE_MID, "audio/midi")
        addFileType("MIDI", FILE_TYPE_MID, "audio/midi")
        addFileType("XMF", FILE_TYPE_MID, "audio/midi")
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi")
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi")
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody")
        addFileType("RTX", FILE_TYPE_MID, "audio/midi")
        addFileType("OTA", FILE_TYPE_MID, "audio/midi")
        addFileType("MXMF", FILE_TYPE_MID, "audio/midi")
        addFileType("MPEG", FILE_TYPE_MP4, "video/mpeg")
        addFileType("MPG", FILE_TYPE_MP4, "video/mpeg")
        addFileType("MP4", FILE_TYPE_MP4, "video/mp4")
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4")
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp")
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp")
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2")
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2")
        addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska")
        addFileType("WEBM", FILE_TYPE_WEBM, "video/webm")
        addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts")
        addFileType("AVI", FILE_TYPE_AVI, "video/avi")
        addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv")
        addFileType("ASF", FILE_TYPE_ASF, "video/x-ms-asf")
        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg")
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg")
        addFileType("GIF", FILE_TYPE_GIF, "image/gif")
        addFileType("PNG", FILE_TYPE_PNG, "image/png")
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp")
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp")
        addFileType("WEBP", FILE_TYPE_WEBP, "image/webp")
        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl")
        addFileType("M3U", FILE_TYPE_M3U, "application/x-mpegurl")
        addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls")
        addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl")
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "application/vnd.apple.mpegurl")
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/mpegurl")
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/x-mpegurl")
        addFileType("FL", FILE_TYPE_FL, "application/x-android-drm-fl")
        addFileType("TXT", FILE_TYPE_TEXT, "text/plain")
        addFileType("HTM", FILE_TYPE_HTML, "text/html")
        addFileType("HTML", FILE_TYPE_HTML, "text/html")
        addFileType("PDF", FILE_TYPE_PDF, "application/pdf")
        addFileType("DOC", FILE_TYPE_MS_WORD, "application/msword")
        addFileType("XLS", FILE_TYPE_MS_EXCEL, "application/vnd.ms-excel")
        addFileType("PPT", FILE_TYPE_MS_POWERPOINT, "application/mspowerpoint")
        addFileType("FLAC", FILE_TYPE_FLAC, "audio/flac")
        addFileType("ZIP", FILE_TYPE_ZIP, "application/zip")
        addFileType("MPG", FILE_TYPE_MP2PS, "video/mp2p")
        addFileType("MPEG", FILE_TYPE_MP2PS, "video/mp2p")
    }
}