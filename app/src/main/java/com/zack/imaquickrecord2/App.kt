package com.zack.imaquickrecord2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class App : Application() {
    lateinit var prefs: CredentialStore

    override fun onCreate() {
        super.onCreate()
        prefs = CredentialStore(this)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RECORD, "录音服务", NotificationManager.IMPORTANCE_LOW).apply {
                description = "后台录音通知"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_UPLOAD, "自动上传", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CHANNEL_RECORD = "record"
        const val CHANNEL_UPLOAD = "upload"

        fun instance(context: Context): App = context.applicationContext as App
    }
}
