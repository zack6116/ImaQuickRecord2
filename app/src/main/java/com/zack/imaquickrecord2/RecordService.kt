package com.zack.imaquickrecord2

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class RecordService : LifecycleService() {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_TOGGLE) {
            ACTION_START, ACTION_TOGGLE -> if (RecordState.recording) stopAndUpload() else start()
            ACTION_STOP -> stopAndUpload()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        val file = RecordState.newFile(this)
        output = file
        try {
            startForegroundCompat(false)
            val mediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(128_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setAudioChannels(1)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            RecordState.recording = true
            RecordState.path = file.absolutePath
            startForegroundCompat(true)
        } catch (e: Exception) {
            cleanup()
            RecordState.recording = false
            stopSelf()
        }
    }

    private fun stopAndUpload() {
        val file = output
        try {
            recorder?.resume()
            recorder?.stop()
        } catch (_: Exception) {
        }
        cleanup()
        RecordState.recording = false
        stopForegroundCompat()
        stopSelf()
        if (file != null && file.length() > 0) upload(file)
    }

    private fun upload(file: File) {
        val prefs = App.instance(this).prefs
        if (!prefs.ready) return
        val uploader = ImaUploader(prefs.clientId, prefs.apiKey, prefs.knowledgeBaseId)
        lifecycleScope.launch {
            val result = uploader.upload(file)
            if (result.isFailure) result.exceptionOrNull()?.printStackTrace()
        }
    }

    private fun cleanup() {
        recorder?.reset()
        recorder?.release()
        recorder = null
        output = null
    }

    private fun startForegroundCompat(recording: Boolean) {
        val notification = Notification.Builder(this, App.CHANNEL_RECORD)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.recording))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this,
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_START = "com.zack.imaquickrecord2.START"
        const val ACTION_STOP = "com.zack.imaquickrecord2.STOP"
        const val ACTION_TOGGLE = "com.zack.imaquickrecord2.TOGGLE"
    }
}
