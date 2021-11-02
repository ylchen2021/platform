package com.boostvision.platform.media.mirror

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.boostvision.platform.media.mirror.RtspServer.MESSAGE_STREAMING_STARTED
import com.boostvision.platform.media.mirror.RtspServer.MESSAGE_STREAMING_STOPPED
import com.boostvision.platform.media.mirror.stream.audio.AudioQuality
import com.boostvision.platform.media.mirror.stream.video.VideoQuality
import com.tbruyelle.rxpermissions2.RxPermissions
import java.lang.Exception

object MirrorManager {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var session: Session? = null
    private var streamWidth = 0
    private var streamHeight = 0
    private var bitrate = 100000
    private var framerate = 30
    private var enableAudio = true
    private var status = MirrorStatus.DISCONNECTED
    private var eventListeners: MutableList<MirrorEventListener> = arrayListOf()
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    private var appContext: Context? = null
    private var rtspServer: RtspServer? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopMirror()
        }
    }

    private val sessionCallback = object : Session.Callback {
        override fun onBitrateUpdate(bitrate: Long) {}

        override fun onSessionError(reason: Int, streamType: Int, e: Exception?) {}

        override fun onPreviewStarted() {}

        override fun onSessionConfigured() {}

        override fun onSessionStarted() {}

        override fun onSessionStopped() {}
    }

    fun init(appContext: Context) {
        this.appContext = appContext
    }

    fun release() {}

    fun prepareForMirror(fragment: Fragment) {
        activityResultLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.data == null) {
                return@registerForActivityResult
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(it.resultCode, it.data!!)
            if (mediaProjection == null) {
                Log.e("@@", "media projection is null")
                return@registerForActivityResult
            }
            status = MirrorStatus.CONNECTING
            notifyEvent(MirrorEventType.STATUS, MirrorStatus.CONNECTING, null)
            mediaProjection?.registerCallback(projectionCallback, null)
            initSessionBuilder()
            startRtspServer()
        }
        rtspServer = RtspServer()
        rtspServer?.addCallbackListener(object : RtspServer.CallbackListener {
            override fun onError(server: RtspServer?, e: Exception?, error: Int) {
                stopMirror()
            }

            override fun onMessage(server: RtspServer?, message: Int) {
                if (message == MESSAGE_STREAMING_STOPPED) {
                    stopMirror()
                } else if (message == MESSAGE_STREAMING_STARTED) {
                    status = MirrorStatus.MIRRORING
                    notifyEvent(MirrorEventType.STATUS, MirrorStatus.MIRRORING, null)
                }
            }
        })
    }

    fun startMirror(fragment: Fragment, width: Int, height: Int, bitrate: Int, framerate: Int, enableAudio: Boolean) {
        streamWidth = width
        streamHeight = height
        this.bitrate = MirrorManager.bitrate
        this.framerate = MirrorManager.framerate
        this.enableAudio = true
        checkAudioPermissions(fragment)
    }

    @SuppressLint("CheckResult")
    private fun checkAudioPermissions(fragment: Fragment) {
        RxPermissions(fragment).request(
            Manifest.permission.RECORD_AUDIO)
            .subscribe { granted ->
                if (!granted) {
                    enableAudio = false
                }
                mediaProjectionManager = fragment.context?.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                activityResultLauncher?.launch(captureIntent)
            }
    }

    fun stopMirror() {
        stopRtspServer()
        mediaProjection?.stop()
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
}