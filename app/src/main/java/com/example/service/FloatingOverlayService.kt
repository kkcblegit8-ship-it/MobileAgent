package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.agent.AgentOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var rootOverlayView: FrameLayout? = null
    private var bubbleView: TextView? = null
    private var hudCardView: LinearLayout? = null
    private var statusTickerText: TextView? = null
    private var targetAppBadge: TextView? = null
    private var promptInput: EditText? = null
    private var voiceButton: TextView? = null

    private var windowParams: WindowManager.LayoutParams? = null
    private var isHudExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        if (Settings.canDrawOverlays(this)) {
            attachSystemOverlayWindow()
        }
        observeAgentState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetApp = intent?.getStringExtra(EXTRA_TARGET_APP).orEmpty()
        if (targetApp.isNotBlank()) {
            _currentTargetApp.value = targetApp
            targetAppBadge?.text = "TARGET: ${targetApp.uppercase()}"
        }
        if (rootOverlayView == null && Settings.canDrawOverlays(this)) {
            attachSystemOverlayWindow()
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "nexus_overlay_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NexusCode Floating Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the NexusCode floating agent bubble active over other apps"
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun attachSystemOverlayWindow() {
        if (rootOverlayView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(140)
        }
        windowParams = params

        val container = FrameLayout(this)

        // 1. Collapsed Floating Cyber-Orb Bubble
        val orb = TextView(this).apply {
            text = ">_"
            setTextColor(Color.parseColor("#10B981"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EE090A0F"))
                setStroke(dp(2), Color.parseColor("#10B981"))
            }
            val size = dp(56)
            layoutParams = FrameLayout.LayoutParams(size, size)
            elevation = dp(10).toFloat()
        }
        bubbleView = orb

        // 2. Expanded Mini Command HUD over other apps
        val hud = buildExpandedHudCard()
        hud.visibility = View.GONE
        hudCardView = hud

        container.addView(orb)
        container.addView(hud)

        setupDragAndTapBehavior(orb, params)

        runCatching {
            wm.addView(container, params)
            rootOverlayView = container
            _isOverlayActive.value = true
        }
    }

    private fun buildExpandedHudCard(): LinearLayout {
        val orchestrator = AgentOrchestrator.getInstance(this)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#F50D0F18"))
                setStroke(dp(1), Color.parseColor("#06B6D4"))
            }
            layoutParams = FrameLayout.LayoutParams(dp(310), FrameLayout.LayoutParams.WRAP_CONTENT)

            // Header Row
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleView = TextView(context).apply {
                text = "NEXUS // OVERLAY AGENT"
                setTextColor(Color.parseColor("#10B981"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val minimizeBtn = TextView(context).apply {
                text = "[—]"
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.MONOSPACE
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { toggleHudExpanded(false) }
            }
            headerRow.addView(titleView)
            headerRow.addView(minimizeBtn)
            addView(headerRow)

            // Target App Pill
            val targetBadge = TextView(context).apply {
                text = "TARGET: ${_currentTargetApp.value.uppercase()}"
                setTextColor(Color.parseColor("#06B6D4"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = Typeface.MONOSPACE
                setPadding(0, dp(4), 0, dp(6))
            }
            targetAppBadge = targetBadge
            addView(targetBadge)

            // Live Agent Step Ticker
            val ticker = TextView(context).apply {
                text = orchestrator.liveOverlayStep.value
                setTextColor(Color.parseColor("#E2E8F0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.MONOSPACE
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.parseColor("#191C28"))
                }
            }
            statusTickerText = ticker
            addView(ticker)

            // Prompt Input Box
            val input = EditText(context).apply {
                hint = "Type command or tap Voice..."
                setHintTextColor(Color.parseColor("#64748B"))
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.MONOSPACE
                setSingleLine(true)
                imeOptions = EditorInfo.IME_ACTION_SEND
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.parseColor("#090A0F"))
                    setStroke(dp(1), Color.parseColor("#262A3B"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }
            promptInput = input
            addView(input)

            // Action Buttons Row (Send, Voice Mic, Cockpit)
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }

            val sendBtn = TextView(context).apply {
                text = "RUN >"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#031811"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.parseColor("#10B981"))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    val cmd = input.text?.toString()?.trim().orEmpty()
                    if (cmd.isNotEmpty()) {
                        input.setText("")
                        orchestrator.submitPrompt(cmd, source = "OVERLAY_HUD")
                    }
                }
            }

            val micBtn = TextView(context).apply {
                text = "🎤 VOICE"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.parseColor("#8B5CF6"))
                }
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.leftMargin = dp(6)
                lp.rightMargin = dp(6)
                layoutParams = lp
                setOnClickListener {
                    if (orchestrator.voiceEngine.isListening.value) {
                        orchestrator.voiceEngine.stopListening()
                    } else {
                        orchestrator.voiceEngine.startListening { spokenText ->
                            input.setText(spokenText)
                            orchestrator.submitPrompt(spokenText, source = "VOICE")
                        }
                    }
                }
            }
            voiceButton = micBtn

            val openCockpitBtn = TextView(context).apply {
                text = "APP ↗"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#06B6D4"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.parseColor("#191C28"))
                    setStroke(dp(1), Color.parseColor("#06B6D4"))
                }
                setOnClickListener {
                    toggleHudExpanded(false)
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(appIntent)
                }
            }

            actionRow.addView(sendBtn)
            actionRow.addView(micBtn)
            actionRow.addView(openCockpitBtn)
            addView(actionRow)
        }
    }

    private fun toggleHudExpanded(expand: Boolean) {
        isHudExpanded = expand
        val params = windowParams ?: return
        val wm = windowManager ?: return
        val container = rootOverlayView ?: return

        if (expand) {
            bubbleView?.visibility = View.GONE
            hudCardView?.visibility = View.VISIBLE
            // Allow keyboard focus so user can type into EditText over other apps
            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            hudCardView?.visibility = View.GONE
            bubbleView?.visibility = View.VISIBLE
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        runCatching { wm.updateViewLayout(container, params) }
    }

    private fun setupDragAndTapBehavior(orb: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        orb.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    runCatching { windowManager?.updateViewLayout(rootOverlayView, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.rawX - initialTouchX)
                    val dy = abs(event.rawY - initialTouchY)
                    if (dx < dp(10) && dy < dp(10)) {
                        toggleHudExpanded(true)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun observeAgentState() {
        val orchestrator = AgentOrchestrator.getInstance(this)
        serviceScope.launch {
            orchestrator.liveOverlayStep.collect { step ->
                statusTickerText?.text = step
            }
        }
        serviceScope.launch {
            orchestrator.voiceEngine.isListening.collect { listening ->
                voiceButton?.text = if (listening) "🔴 STOP" else "🎤 VOICE"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        rootOverlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        rootOverlayView = null
        _isOverlayActive.value = false
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_TARGET_APP = "extra_target_app"
        private const val NOTIFICATION_ID = 4091

        private val _isOverlayActive = MutableStateFlow(false)
        val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

        private val _currentTargetApp = MutableStateFlow("YouTube / System")
        val currentTargetApp: StateFlow<String> = _currentTargetApp.asStateFlow()

        fun setOverlayActiveState(active: Boolean, targetApp: String) {
            _isOverlayActive.value = active
            if (targetApp.isNotBlank()) {
                _currentTargetApp.value = targetApp
            }
        }
    }
}
