package com.boostvision.platform.media.mirror

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boostvision.platform.daemon.DaemonClient
import com.boostvision.platform.media.mirror.RtspServer.*
import com.boostvision.platform.media.mirror.stream.audio.AudioQuality
import com.boostvision.platform.media.mirror.stream.video.VideoQuality
import com.boostvision.platform.network.HttpClient
import com.boostvision.platform.network.ResponseBean
import com.boostvision.platform.utils.DeviceUtils
import okhttp3.MultipartBody
import retrofit2.http.*
import java.lang.Exception

object MirrorManager {
    private var mediaProjection: MediaProjection? = null
    private lateinit var mirrorRequestResult: LiveData<ResponseBean<Any>>
    private val mirrorRequestTrigger = MutableLiveData<MirrorRequestParams>()
    private var streamWidth = 0
    private var streamHeight = 0
    private var bitrate = 100000
    private var framerate = 30
    private var enableAudio = true
    private var status = MirrorStatus.DISCONNECTED
    private var eventListeners: MutableList<MirrorEventListener> = arrayListOf()
    private var targetIp = ""

    private lateinit var appContext: Context
    private var rtspServer: RtspServer? = null
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stop()
        }
    }

    private val sessionCallback = object : Session.Callback {
        override fun onBitrateUpdate(bitrate: Long) {}

        override fun onSessionError(reason: Int, streamType: Int, e: Exception?) {
            stop()
            notifyEvent(MirrorEventType.ERROR, null, e?.message)
        }

        override fun onPreviewStarted() {}

        override fun onSessionConfigured() {}

        override fun onSessionStarted() {}

        override fun onSessionStopped() {}
    }

    fun init(appContext: Context) {
        this.appContext = appContext
        mirrorRequestResult = Transformations.switchMap(mirrorRequestTrigger) {
            DaemonClient.launchDaemon()
            Thread.sleep(500)
            val body = hashMapOf<String, String>()
            body["action"] = it.action
            body["url"] = it.url
            HttpClient.create("http://${targetIp}:${DaemonClient.DEFAULT_PORT}/", MirrorRequestInterface::class.java)
                .mirror(body)
        }
        mirrorRequestResult.observeForever { response ->
            if (!response.isSuccess()) {
                stop()
                notifyEvent(MirrorEventType.ERROR, null, "mirror request error, msg=${response.errorMessage}")
            }
        }
        rtspServer = RtspServer()
        rtspServer?.addCallbackListener(object : RtspServer.CallbackListener {
            override fun onError(server: RtspServer?, e: Exception?, error: Int) {
                stop()
            }

            override fun onMessage(server: RtspServer?, message: Int) {
                if (message == MESSAGE_STREAMING_STOPPED) {
                    stop()
                } else if (message == MESSAGE_STREAMING_STARTED) {
                    status = MirrorStatus.MIRRORING
                    notifyEvent(MirrorEventType.STATUS, MirrorStatus.MIRRORING, null)
                }
            }
        })
    }

    fun release() {
    }

    private fun getRtspPlayUrl(): String {
        return "rtsp://${DeviceUtils.getIpAddress(appContext)}:$DEFAULT_RTSP_PORT"
    }

    fun start(projection: MediaProjection, targetIp: String, width: Int, height: Int, bitrate: Int, framerate: Int, enableAudio: Boolean) {
        streamWidth = width
        streamHeight = height
        this.bitrate = bitrate
        this.framerate = framerate
        this.enableAudio = enableAudio
        this.targetIp = targetIp
        mediaProjection = projection
        status = MirrorStatus.CONNECTING
        notifyEvent(MirrorEventType.STATUS, MirrorStatus.CONNECTING, null)
        mediaProjection?.registerCallback(projectionCallback, null)
        initSessionBuilder()
        startRtspServer()
        mirrorRequestTrigger.postValue(MirrorRequestParams("play", getRtspPlayUrl()))
    }

    fun stop() {
        stopRtspServer()
        mediaProjection?.stop()
        mirrorRequestTrigger.postValue(MirrorRequestParams("stop", ""))
        status = MirrorStatus.DISCONNECTED
        notifyEvent(MirrorEventType.STATUS, MirrorStatus.DISCONNECTED, null)
    }

    fun isMirroring(): Boolean {
        return status != MirrorStatus.DISCONNECTED
    }

    private fun initSessionBuilder() {
        SessionBuilder.getInstance()
            .setCallback(sessionCallback)
            .setPreviewOrientation(90)
            .setContext(appContext)
            .setAudioEncoder(if (enableAudio) SessionBuilder.AUDIO_AAC else SessionBuilder.AUDIO_NONE)
            .setAudioQuality(AudioQuality(16000, 32000))
            .setVideoEncoder(SessionBuilder.VIDEO_H264)
            .setVideoQuality(VideoQuality(streamWidth, streamHeight, framerate, bitrate))
            .setMediaProjection(mediaProjection)
    }

    private fun startRtspServer() {
        rtspServer?.start()
    }

    private fun stopRtspServer() {
        rtspServer?.stop()
    }

    fun addEventListener(listener: MirrorEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }

    fun removeEventListener(listener: MirrorEventListener) {
        if (eventListeners.contains(listener)) {
            eventListeners.remove(listener)
        }
    }

    private fun notifyEvent(type: MirrorEventType, status: MirrorStatus?, param: Any?) {
        eventListeners.forEach {
            it.onMirrorEvent(MirrorEvent(type, status, param))
        }
    }

    interface MirrorEventListener {
        fun onMirrorEvent(event: MirrorEvent)
    }

    interface MirrorRequestInterface {
        @POST("mirror")
        fun mirror(@Body bodyMap: Map<String, String>): LiveData<ResponseBean<Any>>
    }

    data class MirrorRequestParams(
        val action: String,
        val url: String
    )
}