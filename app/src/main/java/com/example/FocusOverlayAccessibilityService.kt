package com.example

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class FocusOverlayAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName.isNotEmpty()) {
                Log.d("FocusAccessibility", "Foreground app window changed: $packageName")
                TimerService.onAccessibilityAppChanged(packageName)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("FocusAccessibility", "Focus Accessibility Service connected!")
        TimerService.isAccessibilityServiceRunning.value = true
    }
}
