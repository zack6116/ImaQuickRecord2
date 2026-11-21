package com.zack.imaquickrecord2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var recordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        recordButton = findViewById(R.id.recordButton)
        val clientId = findViewById<EditText>(R.id.clientId)
        val apiKey = findViewById<EditText>(R.id.apiKey)
        val knowledgeBaseId = findViewById<EditText>(R.id.knowledgeBaseId)
        val prefs = App.instance(this).prefs
        clientId.setText(prefs.clientId)
        apiKey.setText(prefs.apiKey)
        knowledgeBaseId.setText(prefs.knowledgeBaseId)
        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.clientId = clientId.text.toString()
            prefs.apiKey = apiKey.text.toString()
            prefs.knowledgeBaseId = knowledgeBaseId.text.toString()
            Toast.makeText(this, R.string.saved_credentials, Toast.LENGTH_SHORT).show()
        }
        recordButton.setOnClickListener { toggle() }
        requestAudioPermission()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun toggle() {
        if (!permissionGranted()) {
            requestAudioPermission()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        startForegroundService(Intent(this, RecordService::class.java).setAction(RecordService.ACTION_TOGGLE))
    }

    private fun permissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        if (!permissionGranted()) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }

    private fun refresh() {
        if (RecordState.recording) {
            status.text = getString(R.string.recording)
            recordButton.setText(R.string.stop)
        } else {
            status.text = getString(R.string.ready)
            recordButton.setText(R.string.record)
        }
    }
}
