
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
import android.content.pm.PackageManager
import android.app.ActivityManager
import android.content.ComponentName
import android.util.Log
import android.widget.Toast
import android.view.MotionEvent
import android.os.SystemClock
import android.util.DisplayMetrics
import com.example.YTController.databinding.OverlayLayoutBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var binding: OverlayLayoutBinding
    private lateinit var audioManager: AudioManager
    private var previousAppPackage: String? = null
    
    private fun simulateDoubleClickLeft() {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                // 获取屏幕尺寸
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                
                // 计算左侧区域的点击位置 (屏幕左侧1/3处，垂直居中)
                val clickX = screenWidth / 6f  // 左侧1/6处
                val clickY = screenHeight / 2f  // 垂直居中
                
                Log.d("OverlayService", "Screen: ${screenWidth}x${screenHeight}, Click: ($clickX, $clickY)")
                
                val instrumentation = Instrumentation()
                val downTime = SystemClock.uptimeMillis()
                
                // 第一次点击
                val downEvent1 = MotionEvent.obtain(
                    downTime, downTime, MotionEvent.ACTION_DOWN, clickX, clickY, 0
                )
                val upEvent1 = MotionEvent.obtain(
                    downTime, downTime + 50, MotionEvent.ACTION_UP, clickX, clickY, 0
                )
                
                instrumentation.sendPointerSync(downEvent1)
                Thread.sleep(50)
                instrumentation.sendPointerSync(upEvent1)
                
                // 等待100ms模拟双击间隔
                Thread.sleep(100)
                
                // 第二次点击
                val downTime2 = SystemClock.uptimeMillis()
                val downEvent2 = MotionEvent.obtain(
                    downTime2, downTime2, MotionEvent.ACTION_DOWN, clickX, clickY, 0
                )
                val upEvent2 = MotionEvent.obtain(
                    downTime2, downTime2 + 50, MotionEvent.ACTION_UP, clickX, clickY, 0
                )
                
                instrumentation.sendPointerSync(downEvent2)
                Thread.sleep(50)
                instrumentation.sendPointerSync(upEvent2)
                
                // 清理事件
                downEvent1.recycle()
                upEvent1.recycle()
                downEvent2.recycle()
                upEvent2.recycle()
                
                Log.d("OverlayService", "Double click simulation completed")
                
            } catch (e: Exception) {
                Log.e("OverlayService", "Error simulating double click", e)
            }
        }
    }

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
            Log.d("OverlayService", "Rewind button clicked")
            Toast.makeText(this, "Rewind clicked", Toast.LENGTH_SHORT).show()
            
            try {
                // 1. 记录当前前台应用
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningTasks = activityManager.getRunningTasks(1)
                if (runningTasks.isNotEmpty()) {
                    previousAppPackage = runningTasks[0].topActivity?.packageName
                    Log.d("OverlayService", "Current app: $previousAppPackage")
                }
                
                // 2. 切换到YouTube
                val packageManager = packageManager
                // 尝试多个可能的YouTube包名
                val youtubePackages = listOf(
                    "com.google.android.youtube",
                    "com.google.android.apps.youtube.music",
                    "com.android.youtube"
                )
                
                var intent: Intent? = null
                var foundPackage: String? = null
                
                for (pkg in youtubePackages) {
                    intent = packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        foundPackage = pkg
                        Log.d("OverlayService", "Found YouTube at: $pkg")
                        break
                    }
                }
                
                if (intent != null && foundPackage != null) {
                    Log.d("OverlayService", "Starting YouTube")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    
                    // 3. 等待800ms让YouTube启动，然后通过无障碍服务模拟双击左侧
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("OverlayService", "Calling accessibility service for double click")
                        YTAccessibilityService.performDoubleClickLeft()
                        
                        // 4. 等待双击完成后切回原来的应用
                        Handler(Looper.getMainLooper()).postDelayed({
                            previousAppPackage?.let { packageName ->
                                Log.d("OverlayService", "Switching back to: $packageName")
                                val backIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (backIntent != null) {
                                    backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(backIntent)
                                    Log.d("OverlayService", "Switched back successfully")
                                } else {
                                    Log.e("OverlayService", "Could not get launch intent for: $packageName")
                                }
                            }
                        }, 1000) // 给更多时间让双击完成
                    }, 800) // 给YouTube更多启动时间
                } else {
                    // 尝试通过Intent Action启动YouTube
                    Log.d("OverlayService", "Trying YouTube via Intent Action")
                    try {
                        val youtubeIntent = Intent(Intent.ACTION_VIEW)
                        youtubeIntent.data = android.net.Uri.parse("https://www.youtube.com")
                        youtubeIntent.setPackage("com.google.android.youtube")
                        youtubeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(youtubeIntent)
                        
                        // 等待YouTube启动，然后通过无障碍服务模拟双击左侧
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d("OverlayService", "Calling accessibility service for double click")
                            YTAccessibilityService.performDoubleClickLeft()
                            
                            // 等待双击完成后切回原来的应用
                            Handler(Looper.getMainLooper()).postDelayed({
                                Log.d("OverlayService", "Auto switching back to previous app")
                                previousAppPackage?.let { packageName ->
                                    Log.d("OverlayService", "Switching back to: $packageName")
                                    val backIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    if (backIntent != null) {
                                        backIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(backIntent)
                                        Log.d("OverlayService", "Switched back successfully")
                                    }
                                }
                            }, 1000) // 给更多时间让双击完成
                        }, 800) // 给YouTube更多启动时间
                        
                    } catch (e: Exception) {
                        Log.e("OverlayService", "YouTube not installed or no launch intent", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("OverlayService", "Error in rewind button click", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
