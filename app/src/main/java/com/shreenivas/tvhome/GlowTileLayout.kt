package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout

class GlowTileLayout(context: Context) : FrameLayout(context) {
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spotlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val outerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rectF = RectF()
    private val sheenRect = newRectF()
    private val shapePath = Path()

    private var tileFocused = false
    private var pulseAlpha = 0.4f
    private var breathingAnimator: ValueAnimator? = null
    private var accentColor = Color.rgb(92, 184, 255)
    private var tileShape = "squircle"
    private var tileKey = "frost"

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    private fun newRectF() = RectF()

    fun setStyle(color: Int, shape: String?, themeKey: String?) {
        accentColor = color
        tileShape = shape ?: "squircle"
        tileKey = themeKey ?: "frost"
        invalidate()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        tileFocused = gainFocus

        animate().cancel()
        setLayerType(LAYER_TYPE_HARDWARE, null)
        animate()
            .scaleX(if (gainFocus) 1.08f else 1.0f)
            .scaleY(if (gainFocus) 1.08f else 1.0f)
            .translationZ(if (gainFocus) dp(12f) else 0f)
            .setDuration(if (gainFocus) 220L else 150L)
            .setInterpolator(LauncherUtils.FLUID_SPRING)
            .withEndAction { setLayerType(LAYER_TYPE_NONE, null) }
            .start()

        if (gainFocus) startBreathing() else stopBreathing()
        invalidate()
    }

    private fun startBreathing() {
        breathingAnimator?.cancel()
        breathingAnimator = ValueAnimator.ofFloat(0.28f, 0.72f).apply {
            duration = 1300L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulseAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopBreathing() {
        breathingAnimator?.cancel()
        pulseAlpha = 0f
    }

    private fun buildShapePath(w: Float, h: Float) {
        shapePath.reset()
        val pad = dp(2.5f)
        when (tileShape) {
            "heart" -> {
                val cx = w / 2f
                val topY = h * 0.22f
                val botY = h - pad * 2f
                val leftX = w * 0.12f
                val rightX = w * 0.88f
                shapePath.moveTo(cx, botY)
                shapePath.cubicTo(leftX - w * 0.08f, h * 0.65f, leftX, h * 0.12f, cx, topY + h * 0.08f)
                shapePath.cubicTo(rightX, h * 0.12f, rightX + w * 0.08f, h * 0.65f, cx, botY)
                shapePath.close()
            }
            "circle" -> {
                val r = (minOf(w, h) / 2f) - pad
                shapePath.addCircle(w / 2f, h / 2f, r, Path.Direction.CW)
            }
            else -> {
                val radius = if (tileShape == "classic") dp(6f) else dp(20f)
                rectF.set(pad, pad, w - pad, h - pad)
                shapePath.addRoundRect(rectF, radius, radius, Path.Direction.CW)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        buildShapePath(w, h)

        val colors = TileThemePalette.getTopBottomColors(tileKey, tileFocused)
        cardPaint.shader = LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
        canvas.drawPath(shapePath, cardPaint)

        canvas.save()
        canvas.clipPath(shapePath)
        sheenRect.set(0f, 0f, w, h * 0.50f)
        glassSheenPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.50f,
            Color.argb(if (tileFocused) 115 else 68, 255, 255, 255),
            Color.argb(6, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(sheenRect, glassSheenPaint)

        if (tileFocused) {
            val centerGlow = LauncherUtils.withAlpha(accentColor, 130)
            spotlightPaint.shader = RadialGradient(
                w / 2f, h * 0.42f, maxOf(w, h) * 0.58f,
                centerGlow, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, spotlightPaint)
        }
        canvas.restore()

        if (tileFocused) {
            outerHaloPaint.strokeWidth = dp(5.5f)
            outerHaloPaint.color = LauncherUtils.withAlpha(accentColor, (pulseAlpha * 255).toInt())
            canvas.drawPath(shapePath, outerHaloPaint)

            borderPaint.shader = null
            borderPaint.strokeWidth = dp(2.2f)
            borderPaint.color = accentColor
            canvas.drawPath(shapePath, borderPaint)
        } else {
            borderPaint.strokeWidth = dp(1f)
            borderPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                Color.argb(120, 255, 255, 255),
                Color.argb(30, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(shapePath, borderPaint)
            borderPaint.shader = null
        }

        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopBreathing()
    }

    private fun dp(value: Float) = LauncherUtils.dpF(context, value)
}
