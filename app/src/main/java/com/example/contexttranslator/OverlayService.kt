package com.example.contexttranslator

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val selectedText = intent?.getStringExtra("EXTRA_SELECTED_TEXT") ?: ""
        val contextText = intent?.getStringExtra("EXTRA_SURROUNDING_CONTEXT") ?: ""

        val pref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val apiKey = pref.getString("gemini_api_key", "") ?: ""

        displayOverlay(selectedText)
        requestTranslation(selectedText, contextText, apiKey)

        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun displayOverlay(selectedText: String) {
        removeOverlay()

        floatingView = LayoutInflater.from(this).inflate(R.layout.view_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 200
        }

        val tvTarget = floatingView?.findViewById<TextView>(R.id.tvTargetWord)
        val btnClose = floatingView?.findViewById<TextView>(R.id.btnClose)

        tvTarget?.text = "원문: $selectedText"
        btnClose?.setOnClickListener { stopSelf() }

        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    private fun requestTranslation(selected: String, context: String, apiKey: String) {
        serviceScope.launch {
            val repo = GeminiRepository(apiKey)
            val result = repo.translateWithContext(selected, context)

            floatingView?.let { view ->
                val progress = view.findViewById<ProgressBar>(R.id.loadingProgress)
                val tvResult = view.findViewById<TextView>(R.id.tvTranslatedText)

                progress.visibility = View.GONE
                tvResult.visibility = View.VISIBLE
                tvResult.text = result
            }
        }
    }

    private fun removeOverlay() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        serviceScope.cancel()
    }
}

