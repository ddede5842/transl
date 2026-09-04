package com.example.contexttranslator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "화면 위에 그리기 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val fullContext = ScreenContextService.instance?.extractSurroundingText() ?: ""

        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            putExtra("EXTRA_SELECTED_TEXT", selectedText)
            putExtra("EXTRA_SURROUNDING_CONTEXT", fullContext)
        }
        startService(overlayIntent)
        finish()
    }
}

