package com.example.contexttranslator

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenContextService : AccessibilityService() {

    companion object {
        var instance: ScreenContextService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun extractSurroundingText(): String {
        val root = rootInActiveWindow ?: return ""
        val builder = StringBuilder()
        traverse(root, builder, 0)
        return builder.toString()
    }

    private fun traverse(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (depth > 10) return

        node.text?.let {
            if (it.isNotBlank()) {
                sb.append(it).append("\n")
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverse(child, sb, depth + 1)
            child.recycle()
        }
    }
}

