package com.shreenivas.tvhome

import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup

object DrawerFocusTrap {

    fun handleKeyEvent(
        event: KeyEvent,
        isDrawerOpen: Boolean,
        drawerLayout: ViewGroup,
        topNavBar: ViewGroup
    ): Boolean {
        if (!isDrawerOpen || event.action != KeyEvent.ACTION_DOWN) return false

        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_DPAD_LEFT &&
            code != KeyEvent.KEYCODE_DPAD_RIGHT &&
            code != KeyEvent.KEYCODE_DPAD_UP &&
            code != KeyEvent.KEYCODE_DPAD_DOWN
        ) return false

        var current = drawerLayout.findFocus()
        val inDrawer = current != null
        if (current == null) current = topNavBar.findFocus()

        if (inDrawer && current != null) {
            val tag = current.tag as? String ?: ""
            if (tag.startsWith("pill_")) {
                if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    val subRow = current.parent as? ViewGroup ?: return true
                    val dir = if (code == KeyEvent.KEYCODE_DPAD_LEFT) View.FOCUS_LEFT else View.FOCUS_RIGHT
                    val nextPill = FocusFinder.getInstance().findNextFocus(subRow, current, dir)
                    (nextPill ?: current).requestFocus()
                    return true
                }
                if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    CustomizeMenuBuilder.closeSubMenuAndNavigate(code == KeyEvent.KEYCODE_DPAD_DOWN)
                    return true
                }
            }
        }

        if (code == KeyEvent.KEYCODE_DPAD_UP && inDrawer && current != null) {
            val nextUp = FocusFinder.getInstance().findNextFocus(drawerLayout, current, View.FOCUS_UP)
            if (nextUp == null) {
                topNavBar.requestFocus()
                return true
            }
        }

        if (code == KeyEvent.KEYCODE_DPAD_DOWN && !inDrawer && current != null) {
            drawerLayout.requestFocus()
            return true
        }

        val container = if (inDrawer) drawerLayout else topNavBar
        if (current != null) {
            val dir = when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
                KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
                else -> View.FOCUS_DOWN
            }
            val next = FocusFinder.getInstance().findNextFocus(container, current, dir)
            if (next != null) {
                next.requestFocus()
                return true
            }
            return inDrawer
        }

        drawerLayout.requestFocus()
        return true
    }
}
