
package com.example.YTController

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.YTController.R
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, OverlayService::class.java)
        if (OverlayService.isRunning) {
            stopService(intent)
        } else {
            startService(intent)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile
        if (OverlayService.isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "YT Controller On"
            tile.icon = Icon.createWithResource(this, R.drawable.ic_battery_overlay_on)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "YT Controller Off"
            tile.icon = Icon.createWithResource(this, R.drawable.ic_battery_overlay_off)
        }
        tile.updateTile()
    }
}
