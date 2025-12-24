package com.keepscreenon

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 檢查服務是否已經在運行
        if (OverlayService.isRunning) {
            // 服務已在運行，停止服務
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "螢幕常亮已關閉", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 檢查懸浮視窗權限
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            startOverlayService()
        }
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "請允許「顯示在其他應用程式上層」權限", Toast.LENGTH_LONG).show()
        
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            } else {
                Toast.makeText(this, "需要權限才能運行", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, "螢幕常亮已啟動", Toast.LENGTH_SHORT).show()
        finish()
    }
}
