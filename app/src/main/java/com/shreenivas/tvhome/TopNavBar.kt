package com.shreenivas.tvhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
    private var receiverRegistered = false

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateClockAndGreeting()
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(0, dp(2), 0, dp(10))

        // 1. Sleek Clock & Date
        val clockBox = LinearLayout(context).apply { orientation = VERTICAL }
        dateTv = makeText("", 15f, Color.WHITE, true).apply {
            setShadowLayer(4f, 0f, 2f, Color.argb(110, 0, 0, 0))
        }
        timeTv = makeText("", 11f, Color.argb(210, 220, 240, 255), false)
        clockBox.addView(dateTv)
        clockBox.addView(timeTv)
        addView(clockBox)

        // 2. Vertical Divider
        val divider = View(context).apply { setBackgroundColor(Color.argb(65, 255, 255, 255)) }
        addView(divider, LayoutParams(dp(1), dp(28)).apply {
            leftMargin = dp(16)
            rightMargin = dp(16)
        })

        // 3. Greeting Card
        greetingCard = GreetingShineCard(context, lp.userName, lp.greetingPhrase, lp.accentColor)
        addView(greetingCard, LayoutParams(-2, dp(42)))
        updateClockAndGreeting()

        addView(View(context), LayoutParams(0, 1, 1f))

        // 4. Voice Search Pill
        val voicePill = makeGlassPill("🎙  Voice Search").apply {
            setOnClickListener { callbacks.onVoiceSearch() }
        }
        addView(voicePill, LayoutParams(-2, dp(40)))

        // 5. Wi-Fi Pill
        val wifiPill = makeGlassPill("📶  WI-FI").apply {
            setOnClickListener { callbacks.onWifiClick() }
        }
        addView(wifiPill, LayoutParams(-2, dp(40)).apply { leftMargin = dp(12) })

        // 6. Customize Circle Button
        val customizeBtn = makeCircleButton("✦").apply {
            setOnClickListener { callbacks.onCustomizeClick() }
        }
        addView(customizeBtn, LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(12) })

        // 7. Quick Settings Circle Button
        val quickBtn = makeCircleButton("⚙").apply {
            setOnClickListener { callbacks.onQuickSettingsClick() }
        }
        addView(quickBtn, LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(10) })
    }

    private fun updateClockAndGreeting() {
        val now = Date()
        dateTv.text = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now)
        timeTv.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
        refreshGreeting()
    }

    fun refreshGreeting() {
        greetingCard.updateGreeting(lp.userName, lp.greetingPhrase)
        greetingCard.setAccentColor(lp.accentColor)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            context.registerReceiver(timeReceiver, filter)
            receiverRegistered = true
            updateClockAndGreeting()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (receiverRegistered) {
            try { context.unregisterReceiver(timeReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    private fun makeGlassPill(label: String): LinearLayout {
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), 0, dp(18), 0)
            isFocusable = true
            isClickable = true
            addView(makeText(label, 12f, Color.argb(245, 255, 255, 255), true))
            background = pillBg(false)
            setOnFocusChangeListener { v, focused ->
                LauncherUtils.applyFocusSpring(v, focused, 1.07f, 8f)
                v.background = pillBg(focused)
            }
        }
    }

    private fun makeCircleButton(symbol: String): TextView {
        return TextView(context).apply {
            text = symbol
            textSize = 15.5f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = circleBg(false)
            setOnFocusChangeListener { v, focused ->
                LauncherUtils.applyFocusSpring(v, focused, 1.12f, 8f)
                if (symbol == "⚙") v.animate().rotation(if (focused) 45f else 0f).setDuration(200).start()
                v.background = circleBg(focused)
            }
        }
    }

    private fun pillBg(focused: Boolean) = GradientDrawable().apply {
        setColor(if (focused) Color.argb(105, 255, 255, 255) else Color.argb(48, 16, 26, 48))
        setStroke(dp(if (focused) 2 else 1), if (focused) lp.accentColor else Color.argb(85, 255, 255, 255))
        cornerRadius = LauncherUtils.dpF(context, 22f)
    }

    private fun circleBg(focused: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (focused) Color.argb(105, 255, 255, 255) else Color.argb(52, 16, 26, 48))
        setStroke(dp(if (focused) 2 else 1), if (focused) lp.accentColor else Color.argb(90, 255, 255, 255))
    }

    private fun makeText(value: String, size: Float, color: Int, bold: Boolean) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun dp(value: Int) = LauncherUtils.dp(context, value)
}
