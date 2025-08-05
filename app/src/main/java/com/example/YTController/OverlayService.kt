
package com.example.YTController

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import android.media.AudioManager
import android.app.Instrumentation
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import com.example.YTController.databinding.OverlayLayoutBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var binding: OverlayLayoutBinding
    private lateinit var audioManager: AudioManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        binding = OverlayLayoutBinding.inflate(LayoutInflater.from(this))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(binding.root, params)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.playPauseButton.setOnClickListener {
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            audioManager.dispatchMediaKeyEvent(keyEvent)
            val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            audioManager.dispatchMediaKeyEvent(keyEventUp)
        }

        binding.rewindButton.setOnClickListener {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                val instrumentation = Instrumentation()
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        windowManager.removeView(binding.root)
    }

    companion object {
        var isRunning = false
    }
}
