package com.zack.imaquickrecord2

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordState {
    @Volatile var recording: Boolean = false
    @Volatile var path: String? = null

    fun newFile(context: Context): File {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return File(dir, "$stamp.m4a")
    }
}
