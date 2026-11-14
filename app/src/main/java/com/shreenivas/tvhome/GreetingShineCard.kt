package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class GreetingShineCard(
    ctx: Context,
    private var userName: String,
    private var customPhrase: String,
    private var accentColor: Int
) : LinearLayout(ctx) {

    private val mainPill = LinearLayout(ctx)
    private val greetingTv = ShimmerTextView(ctx)
    private var externalBubbleTv: TextView? = null
    private var nudgeAnimator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false

        mainPill.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(6), dp(18), dp(6))
        }

        greetingTv.apply {
            textSize = 15.5f
            letterSpacing = 0.03f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(280)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        mainPill.addView(greetingTv)
        addView(mainPill)
        refreshState()
    }

    fun attachOverlayBubble(bubble: TextView) {
        externalBubbleTv = bubble
        refreshState()
    }

    fun updateGreeting(name: String, phrase: String) {
        userName = name
        customPhrase = phrase
        refreshState()
    }

    fun setAccentColor(accent: Int) {
        accentColor = accent
        refreshState()
    }

    private fun refreshState() {
        val cal = Calendar.getInstance()
        val totalMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val (prefix, topCol, botCol, strokeCol) = getTimeAtmosphere(totalMins)
        val finalLead = if (customPhrase.trim().isNotEmpty()) "✦  ${customPhrase.trim()}" else prefix
        greetingTv.text = "$finalLead, $userName"

        mainPill.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topCol, botCol)
        ).apply {
            cornerRadius = LauncherUtils.dpF(context, 22f)
            setStroke(dp(1), strokeCol)
        }

        updateNudgeBubble(totalMins)
    }

    private data class Atmosphere(val greeting: String, val top: Int, val bot: Int, val stroke: Int)

    private fun getTimeAtmosphere(mins: Int): Atmosphere = when {
        mins in 240..359 -> Atmosphere(
            "🌅  Got up early! Good Morning",
            Color.argb(85, 255, 195, 110),
            Color.argb(35, 255, 150, 80),
            Color.argb(165, 255, 210, 125)
        )
        mins in 360..719 -> Atmosphere(
            "☀  Good Morning",
            Color.argb(75, 255, 225, 140),
            Color.argb(30, 255, 200, 110),
            Color.argb(145, 255, 220, 120)
        )
        mins in 720..959 -> Atmosphere(
            "🌤  Good Afternoon",
            Color.argb(68, 160, 220, 255),
            Color.argb(28, 110, 180, 255),
            Color.argb(135, 175, 225, 255)
        )
        mins in 960..1139 -> Atmosphere(
            "🌇  Good Evening",
            Color.argb(85, 255, 145, 110),
            Color.argb(38, 190, 95, 145),
            Color.argb(165, 255, 165, 120)
        )
        else -> Atmosphere(
            "🌙  Good Night",
            Color.argb(75, 115, 130, 220),
            Color.argb(35, 65, 80, 165),
            Color.argb(145, 170, 185, 255)
        )
    }

    private fun updateNudgeBubble(mins: Int) {
        val bubble = externalBubbleTv ?: return
        val pool: List<String>? = when {
            mins >= 1350 -> listOf(
                "🌙  Even the TV needs some rest now...",
                "🔋  Human battery at 10% • Recharge!",
                "✨  Tomorrow starts with tonight's sleep",
                "🛋️  One last episode... or actual sleep?",
                "💤  Winding down • Cozy blanket awaits"
            )
            mins in 0..239 -> listOf(
                "🦉  Midnight Owl! Dreams on, TV off",
                "🚫  No TV zone! Eyes deserve a break",
                "🌌  Stars are asleep, why are you awake?",
                "⚡  Morning alarm won't show mercy!",
                "🛌  Past midnight • Call it a night"
            )
            mins in 240..359 -> listOf(
                "🌅  Early bird mode! Peaceful morning",
                "☕  Up before the sun! Fresh start",
                "✨  Quiet morning, clear mind!"
            )
            else -> null
        }

        if (pool == null) {
            bubble.visibility = View.GONE
            nudgeAnimator?.cancel()
            nudgeAnimator = null
            return
        }

        val index = ((mins / 15) % pool.size).coerceAtLeast(0)
        bubble.text = pool[index]
        bubble.visibility = View.VISIBLE

        val isMidnight = mins in 0..239
        val isEarly = mins in 240..359
        val topTint = when {
            isMidnight -> Color.argb(215, 185, 55, 95)
            isEarly -> Color.argb(210, 195, 130, 55)
            else -> Color.argb(215, 48, 65, 145)
        }
        val strokeTint = when {
            isMidnight -> Color.argb(210, 255, 160, 180)
            isEarly -> Color.argb(210, 255, 220, 145)
            else -> Color.argb(200, 175, 195, 255)
        }

        bubble.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(topTint, Color.argb(225, 18, 26, 54))
        ).apply {
            cornerRadii = floatArrayOf(
                dpF(4f), dpF(4f),
                dpF(16f), dpF(16f),
                dpF(16f), dpF(16f),
                dpF(16f), dpF(16f)
            )
            setStroke(dp(1), strokeTint)
        }

        if (nudgeAnimator == null) {
            nudgeAnimator = ValueAnimator.ofFloat(0.88f, 1.0f).apply {
                duration = 2200L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { bubble.alpha = it.animatedValue as Float }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        nudgeAnimator?.cancel()
        nudgeAnimator = null
    }

    private fun dp(v: Int) = LauncherUtils.dp(context, v)
    private fun dpF(v: Float) = LauncherUtils.dpF(context, v)

    private class ShimmerTextView(ctx: Context) : TextView(ctx) {
        private var shader: LinearGradient? = null
        private val matrix = Matrix()
        private var translateX = 0f
        private var anim: ValueAnimator? = null

        init {
            setTextColor(Color.rgb(255, 232, 155))
            setShadowLayer(4f, 0f, 1.5f, Color.argb(150, 0, 0, 0))
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0) return
            shader = LinearGradient(
                -w * 0.4f, 0f, 0f, 0f,
                intArrayOf(
                    Color.rgb(245, 200, 80),
                    Color.rgb(255, 252, 225),
                    Color.rgb(245, 200, 80)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            anim?.cancel()
            anim = ValueAnimator.ofFloat(-w * 0.15f, w * 1.6f).apply {
                duration = 2800L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    translateX = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            shader?.let {
                matrix.setTranslate(translateX, 0f)
                it.setLocalMatrix(matrix)
            }
            super.onDraw(canvas)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            anim?.cancel()
        }
    }
}
