package com.zack.imaquickrecord2

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RecordTileService : TileService() {
    override fun onStartListening() {
        update()
    }

    override fun onClick() {
        startService(Intent(this, RecordService::class.java).setAction(RecordService.ACTION_TOGGLE))
    }

    private fun update() {
        qsTile?.state = if (RecordState.recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
