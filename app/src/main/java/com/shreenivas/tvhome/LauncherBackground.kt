package com.shreenivas.tvhome

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
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
        val shift = minOf(45f, scrollY * 0.14f)
        activeWallpaperView?.translationY = baseOffsetY - shift
        activeAuroraView?.setParallaxShift(shift)
    }
    private class AuroraSkyView(
        ctx: Context,
        private var themeKey: String,
        private var accentColor: Int
    ) : View(ctx) {

        private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val auroraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 250, 228) }
        private val moonCutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        private val wavePath = Path()
        private val matrix = Matrix()
        private var auroraShader: LinearGradient? = null
        private var flowAnim: ValueAnimator? = null
        private var phase = 0f
        private var parallaxY = 0f

        // 55 fixed stars (xRatio, yRatio, radiusDp, twinkleOffset)
        private val stars = List(55) { i ->
            val rnd = Random(i * 997 + 13)
            floatArrayOf(
                rnd.nextFloat(),
                rnd.nextFloat() * 0.68f,
                0.8f + rnd.nextFloat() * 1.6f,
                rnd.nextFloat() * 6.28f
            )
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
                duration = 16000L
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
                intArrayOf(Color.rgb(7, 11, 30), baseColors[0], baseColors[2]),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )

            auroraShader = LinearGradient(
                0f, 0f, w.toFloat(), h * 0.6f,
                intArrayOf(
                    Color.argb(0, 46, 232, 165),
                    Color.argb(95, 42, 235, 168),   // Aurora Emerald Green
                    Color.argb(105, 85, 175, 255),  // Aurora Cyan Blue
                    Color.argb(90, 175, 95, 255),   // Aurora Violet-Magenta
                    Color.argb(0, 175, 95, 255)
                ),
                floatArrayOf(0f, 0.25f, 0.52f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
            auroraPaint.shader = auroraShader
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            // 1. Deep Cosmic Sky
            canvas.drawRect(0f, 0f, w, h, skyPaint)

            // 2. Twinkling Stars
            val d = resources.displayMetrics.density
            for (s in stars) {
                val twinkle = 0.45f + 0.55f * sin(phase * 2f + s[3])
                starPaint.color = Color.argb((twinkle * 235).toInt(), 235, 245, 255)
                canvas.drawCircle(s[0] * w, s[1] * h - parallaxY * 0.4f, s[2] * d, starPaint)
            }

            // 3. Glowing Crescent Moon (Top-Right Sky)
            val moonX = w * 0.84f
            val moonY = h * 0.19f - parallaxY * 0.6f
            val moonR = 24f * d
            moonHaloPaint.shader = RadialGradient(
                moonX, moonY, moonR * 3.4f,
                Color.argb(85, 255, 248, 210), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(moonX, moonY, moonR * 3.4f, moonHaloPaint)

            val layerId = canvas.saveLayer(moonX - moonR * 2, moonY - moonR * 2, moonX + moonR * 2, moonY + moonR * 2, null)
            canvas.drawCircle(moonX, moonY, moonR, moonPaint)
            canvas.drawCircle(moonX + moonR * 0.42f, moonY - moonR * 0.22f, moonR * 0.86f, moonCutPaint)
            canvas.restoreToCount(layerId)

            // 4. Flowing Aurora Borealis Ribbons
            auroraShader?.let {
                val drift = sin(phase) * (w * 0.08f)
                matrix.setTranslate(drift, 0f)
                it.setLocalMatrix(matrix)
            }
            drawAuroraWave(canvas, w, h, baseY = h * 0.34f, amp = h * 0.07f, offset = 0f)
            drawAuroraWave(canvas, w, h, baseY = h * 0.44f, amp = h * 0.05f, offset = 1.8f)
        }

        private fun drawAuroraWave(canvas: Canvas, w: Float, h: Float, baseY: Float, amp: Float, offset: Float) {
            wavePath.reset()
            wavePath.moveTo(0f, baseY)
            val steps = 12
            val stepW = w / steps
            for (i in 0..steps) {
                val x = i * stepW
                val y = baseY + sin(phase + offset + i * 0.55f) * amp - parallaxY
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(w, baseY + h * 0.28f)
            wavePath.lineTo(0f, baseY + h * 0.28f)
            wavePath.close()
            canvas.drawPath(wavePath, auroraPaint)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            flowAnim?.cancel()
        }
    }
}
