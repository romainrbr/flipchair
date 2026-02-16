/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import app.lawnchair.preferences2.PreferenceManager2
import com.patrykmichalik.opto.core.firstBlocking

class LawnchairAccessibilityService : AccessibilityService() {

    private var lastLaunchTime = 0L

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            packageNames = null
        }
        lawnchairApp.accessibilityService = this
    }

    override fun onDestroy() {
        lawnchairApp.accessibilityService = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val isCover = isCoverScreen()
        Log.d(TAG, "Event from ${event.packageName}, isCover=$isCover")

        val prefs = PreferenceManager2.getInstance(this)
        if (prefs == null) {
            Log.d(TAG, "PreferenceManager2 is null, skipping")
            return
        }
        val prefEnabled = prefs.coverScreenAutoLaunch.firstBlocking()
        if (!prefEnabled) {
            Log.d(TAG, "coverScreenAutoLaunch pref is disabled, skipping")
            return
        }

        if (event.packageName == packageName) {
            Log.d(TAG, "Event from own package, skipping")
            return
        }

        if (!isCover) {
            Log.d(TAG, "Not cover screen, skipping")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < LAUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Cooldown active, skipping")
            return
        }
        lastLaunchTime = now

        Log.d(TAG, "Launching Lawnchair on cover screen!")
        val intent = Intent(this, LawnchairLauncher::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun isCoverScreen(): Boolean {
        // On Samsung Z Flip, the main display (display 0) state is OFF when the
        // cover screen is active. This is the most reliable detection signal since
        // Samsung doesn't expose the cover screen as a separate display and doesn't
        // update resources.displayMetrics or Configuration for the accessibility service.
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val mainDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
        val isCover = mainDisplay.state == Display.STATE_OFF
        Log.d(TAG, "Display 0 state=${mainDisplay.state}, isCover=$isCover")
        return isCover
    }

    companion object {
        private const val TAG = "LawnchairCoverScreen"
        private const val LAUNCH_COOLDOWN_MS = 2000L
    }
}
