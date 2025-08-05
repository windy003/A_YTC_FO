package com.example.YTController

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.WindowManager
import android.content.Context

class YTAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: YTAccessibilityService? = null
        
        fun performDoubleClickLeft() {
            instance?.performDoubleClick()
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("YTAccessibilityService", "Service connected")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d("YTAccessibilityService", "Service destroyed")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理事件，只用于手势注入
    }
    
    override fun onInterrupt() {
        Log.d("YTAccessibilityService", "Service interrupted")
    }
    
    fun performDoubleClick() {
        try {
            // 获取屏幕尺寸
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            Log.d("YTAccessibilityService", "Screen: ${screenWidth}x${screenHeight}")
            
            // 第一步：点击可能的画中画窗口，让视频返回全屏
            // 画中画通常在右下角，尝试点击那个位置
            val pipX = screenWidth * 0.8f  // 右侧80%位置
            val pipY = screenHeight * 0.8f  // 底部80%位置
            
            Log.d("YTAccessibilityService", "First clicking PiP area: ($pipX, $pipY)")
            
            val pipClickPath = Path()
            pipClickPath.moveTo(pipX, pipY)
            
            val pipClick = GestureDescription.StrokeDescription(pipClickPath, 0, 100)
            val pipGesture = GestureDescription.Builder()
                .addStroke(pipClick)
                .build()
            
            // 点击画中画区域
            dispatchGesture(pipGesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("YTAccessibilityService", "PiP click completed, waiting for fullscreen")
                    
                    // 等待视频返回全屏，然后执行双击快退
                    Handler(Looper.getMainLooper()).postDelayed({
                        performLeftDoubleClick(screenWidth, screenHeight)
                    }, 800) // 给800ms时间让视频返回全屏
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.e("YTAccessibilityService", "PiP click cancelled, trying direct double click")
                    // 如果PiP点击失败，直接尝试双击
                    performLeftDoubleClick(screenWidth, screenHeight)
                }
            }, null)
            
        } catch (e: Exception) {
            Log.e("YTAccessibilityService", "Error performing double click", e)
        }
    }
    
    private fun performLeftDoubleClick(screenWidth: Int, screenHeight: Int) {
        try {
            // 计算左侧区域的点击位置（用于快退）
            val clickX = screenWidth / 6f
            val clickY = screenHeight / 2f
            
            Log.d("YTAccessibilityService", "Double clicking left area: ($clickX, $clickY)")
            
            // 创建第一次点击的手势
            val firstClickPath = Path()
            firstClickPath.moveTo(clickX, clickY)
            
            val firstClick = GestureDescription.StrokeDescription(firstClickPath, 0, 100)
            val firstGesture = GestureDescription.Builder()
                .addStroke(firstClick)
                .build()
            
            // 执行第一次点击
            dispatchGesture(firstGesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d("YTAccessibilityService", "First left click completed")
                    
                    // 延迟执行第二次点击
                    Handler(Looper.getMainLooper()).postDelayed({
                        val secondClickPath = Path()
                        secondClickPath.moveTo(clickX, clickY)
                        
                        val secondClick = GestureDescription.StrokeDescription(secondClickPath, 0, 100)
                        val secondGesture = GestureDescription.Builder()
                            .addStroke(secondClick)
                            .build()
                        
                        dispatchGesture(secondGesture, object : GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                Log.d("YTAccessibilityService", "Double click rewind completed successfully")
                            }
                            
                            override fun onCancelled(gestureDescription: GestureDescription?) {
                                Log.e("YTAccessibilityService", "Second left click cancelled")
                            }
                        }, null)
                    }, 150) // 150ms间隔模拟双击
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.e("YTAccessibilityService", "First left click cancelled")
                }
            }, null)
            
        } catch (e: Exception) {
            Log.e("YTAccessibilityService", "Error performing left double click", e)
        }
    }
}