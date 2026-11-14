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
    private val nudgeBubble = TextView(ctx)
    private var nudgeAnimator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false

        mainPill.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(7), dp(22), dp(7))
        }

        greetingTv.apply {
            textSize = 16.5f
            letterSpacing = 0.04f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(380)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        mainPill.addView(greetingTv)
        addView(mainPill)

        nudgeBubble.apply {
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(245, 255, 240, 220))
            setPadding(dp(14), dp(6), dp(16), dp(6))
            isFocusable = false
            isClickable = false
            visibility = View.GONE
        }
        addView(nudgeBubble, LayoutParams(-2, -2).apply { leftMargin = dp(10) })

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
        val pool: List<String>? = when {
            mins >= 1350 -> listOf(
                "🌙  Even the TV needs some rest now...",
                "🔋  Human battery at 10% • Time to recharge!",
                "✨  Tomorrow's plans start with tonight's sleep",
                "🛋️  One last episode... or actual sleep?",
                "💤  Winding down • Cozy blanket awaits"
            )
            mins in 0..239 -> listOf(
                "🦉  Midnight Owl detected! Dreams on, TV off",
                "🚫  No TV zone! Your eyes deserve a break now",
                "🌌  The stars are asleep, why are you awake?",
                "⚡  Heads up: Morning alarm won't show mercy!",
                "🛌  Past midnight • Time to call it a night"
            )
            mins in 240..359 -> listOf(
                "🌅  Early bird mode! Peaceful morning ahead",
                "☕  Up before the sun! Fresh start today",
                "✨  Quiet morning, clear mind • Great going!"
            )
            else -> null
        }

        if (pool == null) {
            nudgeBubble.visibility = View.GONE
            nudgeAnimator?.cancel()
            nudgeAnimator = null
            return
        }

        val index = ((mins / 15) % pool.size).coerceAtLeast(0)
        nudgeBubble.text = pool[index]
        nudgeBubble.visibility = View.VISIBLE

        val isMidnight = mins in 0..239
        val isEarly = mins in 240..359
        val topTint = when {
            isMidnight -> Color.argb(105, 255, 95, 130)
            isEarly -> Color.argb(95, 255, 190, 95)
            else -> Color.argb(95, 135, 110, 235)
        }
        val strokeTint = when {
            isMidnight -> Color.argb(175, 255, 160, 180)
            isEarly -> Color.argb(175, 255, 220, 145)
            else -> Color.argb(170, 185, 170, 255)
        }

        nudgeBubble.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(topTint, Color.argb(48, 25, 35, 65))
        ).apply {
            cornerRadii = floatArrayOf(
                dpF(18f), dpF(18f),
                dpF(18f), dpF(18f),
                dpF(18f), dpF(18f),
                dpF(4f), dpF(4f)
            )
            setStroke(dp(1), strokeTint)
        }

        if (nudgeAnimator == null) {
            nudgeAnimator = ValueAnimator.ofFloat(0.86f, 1.0f).apply {
                duration = 2200L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { nudgeBubble.alpha = it.animatedValue as Float }
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
