package com.shreenivas.tvhome

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class NightNudgeReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_NIGHT_NUDGE = "com.shreenivas.tvhome.ACTION_NIGHT_NUDGE"

        // Exact checkpoints in minutes from midnight:
        // 00:00..03:30 every 10 mins | 04:00..05:30 every 30 mins | 22:30..23:45 every 15 mins
        private val CHECKPOINTS: List<Int> = buildList {
            for (m in 0..210 step 10) add(m)        // 12:00 AM to 3:30 AM (every 10 mins)
            for (m in 240..330 step 30) add(m)      // 4:00 AM to 5:30 AM (every 30 mins)
            for (m in 1350..1425 step 15) add(m)    // 10:30 PM to 11:45 PM (every 15 mins)
        }

        fun scheduleNextNudge(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = Calendar.getInstance()
            val curMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val nextTarget = CHECKPOINTS.firstOrNull { it > curMins }
            val triggerCal = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (nextTarget != null) {
                    set(Calendar.HOUR_OF_DAY, nextTarget / 60)
                    set(Calendar.MINUTE, nextTarget % 60)
                } else {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }
            }

            val intent = Intent(context, NightNudgeReceiver::class.java).apply { action = ACTION_NIGHT_NUDGE }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getBroadcast(context, 707, intent, flags)

            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pi)
            } catch (_: Exception) {
                am.set(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pi)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val cal = Calendar.getInstance()
        val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val appCtx = context.applicationContext

        val (msg, durationMs, moodType) = getNudgeConfig(mins)
        if (msg != null) {
            showFloatingBubble(appCtx, msg, durationMs, moodType)
        }
        scheduleNextNudge(appCtx)
    }

    private data class NudgeConfig(val message: String?, val durationMs: Long, val mood: Int)

    private fun getNudgeConfig(mins: Int): NudgeConfig = when {
        // 10:30 PM to 11:59 PM (Every 15 mins -> 10 seconds, Indigo)
        mins >= 1350 -> NudgeConfig(
            listOf(
                "🌙  Even the TV needs some rest now...",
                "🔋  Human battery at 10% • Time to recharge!",
                "✨  Tomorrow's plans start with tonight's sleep",
                "🛋️  One last episode... or actual sleep?",
                "💤  Winding down • Cozy blanket awaits"
            ).random(),
            10_000L,
            1
        )
        // 12:00 AM to 3:30 AM (Every 10 mins -> 10 seconds, Crimson/Rose)
        mins in 0..215 -> NudgeConfig(
            listOf(
                "🦉  Midnight Owl detected! Dreams on, TV off",
                "🚫  No TV zone! Your eyes deserve a break now",
                "🌌  The stars are asleep, why are you awake?",
                "⚡  Heads up: Morning alarm won't show mercy!",
                "🛌  Past midnight • Time to call it a night"
            ).random(),
            10_000L,
            2
        )
        // 4:00 AM to 5:59 AM (Every 30 mins -> 7 seconds, Warm Sunrise)
        mins in 240..359 -> NudgeConfig(
            listOf(
                "🌅  Got up early! Have a peaceful morning ✨",
                "☕  Up before the sun! Fresh start today",
                "✨  Quiet morning, clear mind • Great going!"
            ).random(),
            7_000L,
            3
        )
        else -> NudgeConfig(null, 0L, 0)
    }

    private fun showFloatingBubble(ctx: Context, message: String, durationMs: Long, mood: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
            return
        }

        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val colors = when (mood) {
            2 -> intArrayOf(Color.argb(232, 195, 55, 95), Color.argb(238, 95, 28, 72))
            3 -> intArrayOf(Color.argb(230, 215, 135, 55), Color.argb(238, 135, 72, 32))
            else -> intArrayOf(Color.argb(228, 55, 75, 165), Color.argb(236, 28, 38, 95))
        }

        val bubble = TextView(ctx).apply {
            text = message
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(LauncherUtils.dp(ctx, 20), LauncherUtils.dp(ctx, 12), LauncherUtils.dp(ctx, 22), LauncherUtils.dp(ctx, 12))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 22f)
                setStroke(LauncherUtils.dp(ctx, 1), Color.argb(175, 255, 255, 255))
            }
            alpha = 0f
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = LauncherUtils.dp(ctx, 36)
            y = LauncherUtils.dp(ctx, 28)
        }

        try {
            wm.addView(bubble, params)
            bubble.animate().alpha(1f).setDuration(350L).start()

            Handler(Looper.getMainLooper()).postDelayed({
                bubble.animate().alpha(0f).setDuration(350L).withEndAction {
                    try { wm.removeView(bubble) } catch (_: Exception) {}
                }.start()
            }, durationMs)
        } catch (_: Exception) {
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        }
    }
}
