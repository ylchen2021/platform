package com.boostvision.platform.media.mirror

import android.content.Intent
import android.os.IBinder
import android.os.Build
import android.app.*
import android.media.projection.MediaProjectionManager
import android.app.PendingIntent
import android.content.ComponentName

class MirrorService : Service() {
    private var mediaProjectionManager: MediaProjectionManager? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val icon = intent.getIntExtra("icon", 0)
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")
        val target = intent.getStringExtra("target")
        createNotificationChannel(icon, title?:"", content?:"", target?:"")
        val resultCode = intent.getIntExtra("code", 0)
        val data = intent.getParcelableExtra("data") as Intent?
        val mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data!!)
        if (mediaProjection == null) {
            return super.onStartCommand(intent, flags, startId)
        }
        val ip = intent.getStringExtra("ip")?:""
        val width = intent.getIntExtra("width", 0)
        val height = intent.getIntExtra("height", 0)
        val bitrate = intent.getIntExtra("bitrate", 0)
        val framerate = intent.getIntExtra("framerate", 0)
        val enableAudio = intent.getBooleanExtra("enableAudio", true)
        MirrorManager.start(mediaProjection, ip, width, height, bitrate, framerate, enableAudio)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel(icon: Int, title: String, content: String, target: String) {
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
            val builder = Notification.Builder(this, "notification_id") //获取一个Notification构造器
            builder.setContentTitle(title) // 设置下拉列表里的标题
                .setSmallIcon(icon) // 设置状态栏内的小图标
                .setContentText(content) // 设置上下文内容
                .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

            val component = ComponentName.unflattenFromString(target)
            val packageName = component?.packageName
            val className = component?.className
            val activityIntent = Intent()
            activityIntent.setClassName(packageName?:"", className?:"")
            val pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(pendingIntent)

            val notification = builder.build() // 获取构建好的Notification
            startForeground(110, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stopForeground(true)
    }
}