package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TopNavBar(
    context: Context,
    private val lp: LauncherPreferences,
    private val callbacks: NavCallbacks
) : LinearLayout(context) {

    interface NavCallbacks {
        fun onVoiceSearch()
        fun onWifiClick()
        fun onCustomizeClick()
        fun onQuickSettingsClick()
    }

    private val dateTv: TextView
    private val timeTv: TextView
    private val greetingCard: GreetingShineCard
    private val ramTv: TextView
    private val wifiPill: LinearLayout
    private val wifiLabelTv: TextView
    private val wifiBubbleTv: TextView

    private var receiverRegistered = false
    private val handler = Handler(Looper.getMainLooper())
    private var swingAnim: ValueAnimator? = null
    private var breatheAnim: ValueAnimator? = null
    private var wifiStateMode = -1 // 0 = Disconnected, 1 = Connecting, 2 = Connected
    private var showConnectingAlt = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateClockRamAndWifi()
        }
    }

    private val cycleRunnable = object : Runnable {
        override fun run() {
            if (wifiStateMode == 1) {
                // Connecting: Alternate text smoothly between "WI-FI" and "Connecting..."
                showConnectingAlt = !showConnectingAlt
                wifiLabelTv.animate().alpha(0f).setDuration(180L).withEndAction {
                    wifiLabelTv.text = if (showConnectingAlt) "◌  Connecting..." else "📶  WI-FI"
                    wifiLabelTv.animate().alpha(1f).setDuration(220L).start()
                }.start()
                handler.postDelayed(this, 1600L)
            } else if (wifiStateMode == 0) {
                // No Internet: Smooth drop-in -> stay 4s -> Airflow Mist Dissolve -> repeat every 10s
                triggerMistBubbleCycle()
                handler.postDelayed(this, 10_000L)
            }
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(0, dp(2), 0, dp(14))

        val clockBox = LinearLayout(context).apply { orientation = VERTICAL }
        dateTv = makeText("", 19f, Color.WHITE, true).apply { setShadowLayer(4f, 0f, 2f, Color.argb(140, 0, 0, 0)) }
        timeTv = makeText("", 14.5f, Color.argb(230, 225, 242, 255), true)
        clockBox.addView(dateTv); clockBox.addView(timeTv)
        addView(clockBox)

        addView(View(context).apply { setBackgroundColor(Color.argb(65, 255, 255, 255)) },
            LayoutParams(dp(1), dp(28)).apply { leftMargin = dp(16); rightMargin = dp(16) })

        greetingCard = GreetingShineCard(context, lp.userName, lp.greetingPhrase, lp.accentColor)
        addView(greetingCard, LayoutParams(-2, dp(42)))

        addView(View(context), LayoutParams(0, 1, 1f))

        // Voice Search Pill
        val voicePill = makeGlassPill("🎙  Voice Search").apply { setOnClickListener { callbacks.onVoiceSearch() } }
        addView(voicePill, LayoutParams(-2, dp(40)))

        // Live RAM Status Pill (Before Wi-Fi)
        ramTv = makeText("⚡ RAM", 13f, Color.WHITE, true)
        val ramPill = LinearLayout(context).apply {
            gravity = Gravity.CENTER; setPadding(dp(14), 0, dp(14), 0); isFocusable = true; isClickable = true
            addView(ramTv); background = pillBg(false, lp.accentColor)
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.06f, 6f); v.background = pillBg(f, lp.accentColor) }
            setOnClickListener {
                System.gc()
                updateRamInfo()
                Toast.makeText(context, "Memory refreshed • ${ramTv.text}", Toast.LENGTH_SHORT).show()
            }
        }
        addView(ramPill, LayoutParams(-2, dp(40)).apply { leftMargin = dp(10) })

        // Wi-Fi Container with Hanging Swinging / Mist-Dissolving Bubble
        val wifiWrapper = FrameLayout(context).apply { clipChildren = false; clipToPadding = false }
        wifiLabelTv = makeText("📶  WI-FI", 14f, Color.WHITE, true)
        wifiPill = LinearLayout(context).apply {
            gravity = Gravity.CENTER; setPadding(dp(16), 0, dp(16), 0); isFocusable = true; isClickable = true
            addView(wifiLabelTv); background = pillBg(false, lp.accentColor)
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.07f, 8f) }
            setOnClickListener { callbacks.onWifiClick() }
        }
        wifiWrapper.addView(wifiPill, FrameLayout.LayoutParams(-2, dp(40), Gravity.TOP or Gravity.CENTER_HORIZONTAL))

        wifiBubbleTv = TextView(context).apply {
            textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            setPadding(dp(10), dp(3), dp(10), dp(3)); isSingleLine = true; visibility = View.GONE
        }
        wifiWrapper.addView(wifiBubbleTv, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(43)
        })
        addView(wifiWrapper, LayoutParams(-2, dp(40)).apply { leftMargin = dp(10) })

        addView(makeCircleButton("✦").apply { setOnClickListener { callbacks.onCustomizeClick() } },
            LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(10) })
        addView(makeCircleButton("⚙").apply { setOnClickListener { callbacks.onQuickSettingsClick() } },
            LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(10) })

        updateClockRamAndWifi()
    }
