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
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import java.util.Locale

class GreetingShineCard(
    ctx: Context,
    userName: String,
    customPhrase: String,
    private var accentColor: Int
) : LinearLayout(ctx) {

    private val greetingTv = ShimmerTextView(ctx)
    private val subTagTv = TextView(ctx)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(18), dp(5), dp(20), dp(5))
        applyGlassPill()

        greetingTv.apply {
            text = formatGreeting(userName, customPhrase)
            textSize = 12.5f
            letterSpacing = 0.04f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(320)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        addView(greetingTv)

        subTagTv.apply {
            text = "✦  SMART TV"
            textSize = 8.5f
            letterSpacing = 0.12f
            setTextColor(Color.argb(185, 210, 230, 255))
            typeface = Typeface.DEFAULT_BOLD
        }
        addView(subTagTv)
    }

    fun updateGreeting(userName: String, customPhrase: String) {
        greetingTv.text = formatGreeting(userName, customPhrase)
    }

    fun setAccentColor(accent: Int) {
        accentColor = accent
        applyGlassPill()
    }

    private fun formatGreeting(userName: String, customPhrase: String): String {
        val prefix = if (customPhrase.trim().isNotEmpty()) {
            "✦  ${customPhrase.trim()}"
        } else {
            getTimeGreeting()
        }
        return "$prefix, $userName"
    }

    private fun getTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "☀  Good Morning"
            hour < 17 -> "🌤  Good Afternoon"
            else -> "🌙  Good Night"
        }
    }

    private fun applyGlassPill() {
        val pill = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(68, 255, 255, 255), Color.argb(26, 255, 255, 255))
        ).apply {
            cornerRadius = LauncherUtils.dpF(context, 18f)
            setStroke(dp(1), Color.argb(135, 255, 215, 110))
        }
        background = pill
    }

    private fun dp(v: Int) = LauncherUtils.dp(context, v)

    private class ShimmerTextView(ctx: Context) : TextView(ctx) {
        private var shader: LinearGradient? = null
        private val matrix = Matrix()
        private var translateX = 0f
        private var anim: ValueAnimator? = null

        init {
            setTextColor(Color.rgb(255, 224, 130))
            setShadowLayer(4f, 0f, 1.5f, Color.argb(140, 0, 0, 0))
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0) return
            shader = LinearGradient(
                -w * 0.4f, 0f, 0f, 0f,
                intArrayOf(
                    Color.rgb(245, 198, 75),
                    Color.rgb(255, 232, 135),
                    Color.rgb(255, 252, 220),
                    Color.rgb(255, 232, 135),
                    Color.rgb(245, 198, 75)
                ),
                floatArrayOf(0f, 0.35f, 0.5f, 0.65f, 1f),
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
