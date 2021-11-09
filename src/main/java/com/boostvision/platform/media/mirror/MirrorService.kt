package com.boostvision.platform.media.mirror

import android.content.Intent
import android.os.IBinder

import android.os.Build

import android.app.*

import android.graphics.BitmapFactory
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager


class MirrorService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val smallRes = intent.getIntExtra("smallicon", 0)
        val largeRes = intent.getIntExtra("largeicon", 0)
        createNotificationChannel(smallRes, largeRes, null)
        val resultCode = intent.getIntExtra("code", 0)
        val data = intent.getParcelableExtra("data") as Intent?
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data!!)
        if (mediaProjection == null) {
            return super.onStartCommand(intent, flags, startId)
        }
        MirrorManager.onMediaProjectionCreated(mediaProjection)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel(smallRes: Int, largeRes: Int, activiyClass: Class<Any>?) {
        val builder = Notification.Builder(this.applicationContext) //获取一个Notification构造器
        builder
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, smallRes)) // 设置下拉列表中的图标(大图标)
            //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
            .setSmallIcon(largeRes) // 设置状态栏内的小图标
            .setContentText("is running......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification = builder.build() // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
        startForeground(110, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopForeground(true)
    }
}