package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object LauncherBackground {
    private var activeWallpaperView: ImageView? = null
    private var activeAuroraView: AuroraSkyView? = null
    private var baseZoom = 1.08f
    private var baseOffsetY = 0

    fun apply(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        val sp = lp.prefs
        val wallpaper = sp.getString(LauncherPreferences.PREF_WALLPAPER, null)
        activeWallpaperView = null
        activeAuroraView = null

        if (wallpaper != null) {
            root.background = null
            val image = ImageView(ctx).apply {
                scaleType = when (sp.getString("wp_scale_mode", "fit")) {
                    "width" -> ImageView.ScaleType.FIT_XY
                    "height" -> ImageView.ScaleType.FIT_CENTER
                    else -> ImageView.ScaleType.CENTER_CROP
                }
                baseZoom = maxOf(1.08f, sp.getFloat("wp_zoom", 1.0f))
                baseOffsetY = sp.getInt("wp_offset_y", 0)
                scaleX = baseZoom
                scaleY = baseZoom
                translationX = sp.getInt("wp_offset_x", 0).toFloat()
                translationY = baseOffsetY.toFloat()
                try {
                    setImageURI(Uri.parse(wallpaper))
                } catch (_: Exception) {
                }
            }
            root.addView(image, 0, FrameLayout.LayoutParams(-1, -1))
            activeWallpaperView = image
        } else {
            root.background = null
            val aurora = AuroraSkyView(ctx, lp.tileColor, lp.accentColor)
            root.addView(aurora, 0, FrameLayout.LayoutParams(-1, -1))
            activeAuroraView = aurora
        }
    }

    fun updateThemeAndAccent(root: FrameLayout, lp: LauncherPreferences) {
        if (lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null) == null) {
            activeAuroraView?.updateStyle(lp.tileColor, lp.accentColor)
        }
    }

    fun onScrollParallax(scrollY: Int) {
        val shift = minOf(36f, scrollY * 0.12f)
        activeWallpaperView?.translationY = baseOffsetY - shift
        activeAuroraView?.setParallaxShift(shift)
    }

    private class AuroraSkyView(
        ctx: Context,
        private var themeKey: String,
        private var accentColor: Int
    ) : View(ctx) {

        private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ribbonPaint1 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ribbonPaint2 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 250, 232) }
        private val moonCutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        private val wavePath = Path()
        private var flowAnim: ValueAnimator? = null
        private var phase = 0f
        private var parallaxY = 0f

        private data class Star(val xRatio: Float, val yRatio: Float, val radiusDp: Float, val offset: Float, val warm: Boolean)

        private val stars = List(75) { i ->
            val rnd = Random(i * 1013 + 7)
            Star(
                xRatio = rnd.nextFloat(),
                yRatio = rnd.nextFloat() * 0.72f,
                radiusDp = 0.5f + rnd.nextFloat() * 1.2f,
                offset = rnd.nextFloat() * 6.28f,
                warm = i % 5 == 0
            )
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            val blur = BlurMaskFilter(48f * resources.displayMetrics.density, BlurMaskFilter.Blur.NORMAL)
            ribbonPaint1.maskFilter = blur
            ribbonPaint2.maskFilter = blur
        }

        fun updateStyle(newTheme: String, newAccent: Int) {
            themeKey = newTheme
            accentColor = newAccent
            buildShaders(width, height)
            invalidate()
        }

        fun setParallaxShift(shift: Float) {
            parallaxY = shift
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            buildShaders(w, h)

            flowAnim?.cancel()
            flowAnim = ValueAnimator.ofFloat(0f, 6.28318f).apply {
                duration = 22000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        private fun buildShaders(w: Int, h: Int) {
            if (w <= 0 || h <= 0) return
            val baseColors = TileThemePalette.getWallpaperGradient(themeKey)
            skyPaint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.rgb(5, 8, 22), baseColors[0], baseColors[2]),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )

            ribbonPaint1.shader = LinearGradient(
                0f, h * 0.15f, w.toFloat(), h * 0.55f,
                intArrayOf(
                    Color.argb(0, 32, 210, 155),
                    Color.argb(72, 38, 225, 165),
                    Color.argb(82, 72, 165, 245),
                    Color.argb(65, 145, 85, 235),
                    Color.argb(0, 145, 85, 235)
                ),
                floatArrayOf(0f, 0.25f, 0.52f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )

            ribbonPaint2.shader = LinearGradient(
                w.toFloat(), h * 0.2f, 0f, h * 0.6f,
                intArrayOf(
                    Color.argb(0, 65, 235, 180),
                    Color.argb(55, 52, 215, 160),
                    Color.argb(60, 110, 140, 255),
                    Color.argb(0, 110, 140, 255)
                ),
                floatArrayOf(0f, 0.35f, 0.70f, 1f),
                Shader.TileMode.CLAMP
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            canvas.drawRect(0f, 0f, w, h, skyPaint)

            val d = resources.displayMetrics.density
            for (s in stars) {
                val twinkle = 0.35f + 0.65f * sin(phase * 2.5f + s.offset)
                val alpha = (twinkle * 215).toInt().coerceIn(20, 235)
                starPaint.color = if (s.warm) Color.argb(alpha, 255, 238, 205) else Color.argb(alpha, 215, 235, 255)
                canvas.drawCircle(s.xRatio * w, s.yRatio * h - parallaxY * 0.35f, s.radiusDp * d, starPaint)
            }

            val moonX = w * 0.85f
            val moonY = h * 0.17f - parallaxY * 0.5f
            val moonR = 20f * d
            moonHaloPaint.shader = RadialGradient(
                moonX, moonY, moonR * 4.2f,
                Color.argb(65, 255, 248, 215), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(moonX, moonY, moonR * 4.2f, moonHaloPaint)

            val layerId = canvas.saveLayer(moonX - moonR * 2, moonY - moonR * 2, moonX + moonR * 2, moonY + moonR * 2, null)
            canvas.drawCircle(moonX, moonY, moonR, moonPaint)
            canvas.drawCircle(moonX + moonR * 0.44f, moonY - moonR * 0.22f, moonR * 0.86f, moonCutPaint)
            canvas.restoreToCount(layerId)

            drawOrganicRibbon(canvas, w, h, baseY = h * 0.28f, thickness = h * 0.18f, amp = h * 0.055f, offset = 0f, paint = ribbonPaint1)
            drawOrganicRibbon(canvas, w, h, baseY = h * 0.36f, thickness = h * 0.14f, amp = h * 0.045f, offset = 2.1f, paint = ribbonPaint2)
        }

        private fun drawOrganicRibbon(
            canvas: Canvas, w: Float, h: Float,
            baseY: Float, thickness: Float, amp: Float, offset: Float, paint: Paint
        ) {
            wavePath.reset()
            val steps = 16
            val stepW = w / steps
            for (i in 0..steps) {
                val x = i * stepW
                val wave = sin(phase + offset + i * 0.45f) * amp + cos(phase * 0.7f + i * 0.3f) * (amp * 0.4f)
                val y = baseY + wave - parallaxY
                if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            for (i in steps downTo 0) {
                val x = i * stepW
                val wave = sin(phase + offset + i * 0.45f) * amp
                val y = baseY + thickness + wave * 0.6f - parallaxY
                wavePath.lineTo(x, y)
            }
            wavePath.close()
            canvas.drawPath(wavePath, paint)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            flowAnim?.cancel()
        }
    }
}
