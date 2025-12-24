package com.keepscreenon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "keep_screen_on_channel"
        
        @Volatile
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        acquireWakeLock()
        createOverlayView()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        
        removeOverlayView()
        releaseWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "螢幕常亮服務",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持螢幕常亮"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // 點擊通知時關閉服務
        val stopIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("螢幕常亮中")
            .setContentText("點擊應用程式圖示關閉")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "KeepScreenOn::WakeLock"
        ).apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 建立透明的 View
        overlayView = View(this).apply {
            // 完全透明
            alpha = 0f
        }

        val layoutParams = WindowManager.LayoutParams().apply {
            // 覆蓋層類型
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            // 設定為不可聚焦、不可觸控（讓觸控事件穿透）
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            
            // 透明格式
            format = PixelFormat.TRANSLUCENT
            
            // 最小尺寸（因為完全透明，尺寸不重要）
            width = 1
            height = 1
        }

        windowManager?.addView(overlayView, layoutParams)
    }

    private fun removeOverlayView() {
        overlayView?.let {
            windowManager?.removeView(it)
        }
        overlayView = null
        windowManager = null
    }
}
