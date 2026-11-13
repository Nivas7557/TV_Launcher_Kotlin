package com.shreenivas.tvhome

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object WifiPanelBuilder {

    fun build(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onRefresh: () -> Unit) {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiOn = wm?.isWifiEnabled ?: true

        @Suppress("DEPRECATION")
        val info = wm?.connectionInfo
        val rawSpeed = info?.linkSpeed ?: -1
        val freq = info?.frequency ?: 0
        val speedTxt = when {
            !isWifiOn -> "Disabled"
            rawSpeed > 0 -> "$rawSpeed Mbps"
            freq > 4900 -> "5 GHz Band"
            freq > 2400 -> "2.4 GHz Band"
            else -> "Ready"
        }

        addLabel(ctx, panel, "CONNECTION TELEMETRY")
        val teleRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        teleRow.addView(
            makeStatCard(ctx, "📶", "Status", if (isWifiOn) "Active" else "Off", lp.accentColor),
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(ctx, 6) }
        )
        teleRow.addView(
            makeStatCard(ctx, "⚡", "Link Rate", speedTxt, lp.accentColor),
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        panel.addView(teleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        val toggleCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            isFocusable = true
            isClickable = true
            background = glassCardBg(ctx, false, lp.accentColor)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f)
                v.background = glassCardBg(ctx, f, lp.accentColor)
            }
            setOnClickListener {
                @Suppress("DEPRECATION")
                try {
                    wm?.isWifiEnabled = !isWifiOn
                    onRefresh()
                } catch (_: Exception) {
                    openSystemWifi(ctx)
                }
            }
        }
        val tMid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        tMid.addView(makeText(ctx, "Wi-Fi Radio", 11f, Color.WHITE, true))
        tMid.addView(makeText(ctx, if (isWifiOn) "Scanning nearby networks" else "Tap to enable Wi-Fi", 8.5f, Color.argb(185, 210, 230, 245), false))
        toggleCard.addView(tMid, LinearLayout.LayoutParams(0, -2, 1f))

        val pill = makeText(ctx, if (isWifiOn) "ON" else "OFF", 9.5f, Color.WHITE, true).apply {
            setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4))
            background = GradientDrawable().apply {
                setColor(if (isWifiOn) lp.accentColor else Color.argb(65, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 10f)
            }
        }
        toggleCard.addView(pill)
        panel.addView(toggleCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        val headRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        headRow.addView(makeText(ctx, "AVAILABLE NETWORKS", 9f, Color.argb(195, 200, 225, 245), true))
        headRow.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        val refBtn = makeText(ctx, "↻ Refresh", 9.5f, lp.accentColor, true).apply {
            isFocusable = true
            setPadding(dp(ctx, 6), dp(ctx, 2), dp(ctx, 6), dp(ctx, 2))
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.06f, 3f) }
            setOnClickListener { Toast.makeText(ctx, "Scanning Wi-Fi...", Toast.LENGTH_SHORT).show(); onRefresh() }
        }
        headRow.addView(refBtn)
        panel.addView(headRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 4); bottomMargin = dp(ctx, 6) })

        if (!loadRealWifi(ctx, panel, wm, lp.accentColor)) {
            addWifiRow(ctx, panel, "Open System Wi-Fi", "Manage saved & hidden networks", "SYS", false, lp.accentColor)
        }

        val moreBtn = makeText(ctx, "⚙  System Network Settings ›", 10.5f, lp.accentColor, true).apply {
            gravity = Gravity.CENTER; isFocusable = true; setPadding(0, dp(ctx, 10), 0, dp(ctx, 8))
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.04f, 3f) }
            setOnClickListener { openSystemWifi(ctx) }
        }
        panel.addView(moreBtn)
    }

    @Suppress("DEPRECATION")
    private fun loadRealWifi(ctx: Context, panel: LinearLayout, wm: WifiManager?, accent: Int): Boolean {
        return try {
            wm?.startScan()
            val results: List<ScanResult> = wm?.scanResults ?: return false
            val seen = HashSet<String>()
            var count = 0
            for (sr in results) {
                val ssid = sr.SSID ?: continue
                if (ssid.isEmpty() || !seen.add(ssid)) continue
                val band = if (sr.frequency > 4900) "5 GHz" else "2.4 GHz"
                val locked = sr.capabilities?.contains("WPA") == true || sr.capabilities?.contains("WEP") == true
                addWifiRow(ctx, panel, ssid, "Signal ${WifiManager.calculateSignalLevel(sr.level, 5)}/4", band, locked, accent)
                if (++count >= 5) break
            }
            count > 0
        } catch (_: SecurityException) { false }
    }

    private fun makeStatCard(ctx: Context, icon: String, title: String, value: String, accent: Int) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8)); background = glassCardBg(ctx, false, accent)
        addView(makeText(ctx, "$icon  $title", 8.5f, Color.argb(190, 210, 230, 245), true))
        addView(makeText(ctx, value, 11f, Color.WHITE, true).apply { setPadding(0, dp(ctx, 3), 0, 0) })
    }

    private fun addWifiRow(ctx: Context, panel: LinearLayout, ssid: String, sub: String, band: String, locked: Boolean, accent: Int) {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8)); isFocusable = true; background = glassCardBg(ctx, false, accent)
            setOnFocusChangeListener { v, f -> LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f); v.background = glassCardBg(ctx, f, accent) }
            setOnClickListener { openSystemWifi(ctx) }
        }
        card.addView(makeText(ctx, "📶", 13f, accent, true).apply { setPadding(0, 0, dp(ctx, 8), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(makeText(ctx, ssid, 10.5f, Color.WHITE, true))
        mid.addView(makeText(ctx, "$sub • $band", 8f, Color.argb(180, 210, 230, 245), false))
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        if (locked) card.addView(makeText(ctx, "🔒", 10f, Color.WHITE, false))
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
    }

    private fun openSystemWifi(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { ctx.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)); return } catch (_: Exception) {}
        }
        try { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (_: Exception) {}
    }

    private fun glassCardBg(ctx: Context, foc: Boolean, accent: Int) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (foc) intArrayOf(Color.argb(115, 255, 255, 255), Color.argb(75, 255, 255, 255))
        else intArrayOf(Color.argb(52, 255, 255, 255), Color.argb(26, 255, 255, 255))
    ).apply { cornerRadius = LauncherUtils.dpF(ctx, 12f); setStroke(dp(ctx, if (foc) 2 else 1), if (foc) accent else Color.argb(65, 255, 255, 255)) }

    private fun addLabel(ctx: Context, panel: LinearLayout, txt: String) = panel.addView(makeText(ctx, txt, 9f, Color.argb(195, 200, 225, 245), true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 4); bottomMargin = dp(ctx, 5) })
    private fun makeText(ctx: Context, txt: String, sz: Float, col: Int, bold: Boolean) = TextView(ctx).apply { text = txt; textSize = sz; setTextColor(col); isSingleLine = true; if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
