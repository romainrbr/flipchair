/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.util

import android.content.Intent
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.integration.util.LauncherActivityScenarioRule

class LauncherActivityInteractor<LAUNCHER_TYPE : Launcher>(
    val launcherActivity: LauncherActivityScenarioRule<LAUNCHER_TYPE>
) {

    val uiDevice = UiDevice.getInstance(getInstrumentation())

    @JvmOverloads
    fun startAppFast(
        packageName: String,
        intent: Intent =
            launcherActivity.context.packageManager.getLaunchIntentForPackage(packageName)!!,
    ) =
        launcherActivity.context
            .startActivity(
                intent
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            .run { uiDevice.waitForIdle() }

    fun addToWorkspace(view: View) =
        TestUtil.runOnExecutorSync(Executors.MAIN_EXECUTOR) {
                view.accessibilityDelegate.performAccessibilityAction(
                    view,
                    R.id.action_add_to_workspace,
                    null,
                )
            }
            .run { uiDevice.waitForIdle() }

    /**
     * Match the behavior with how widget is added in reality with "tap to add" (even with screen
     * readers).
     */
    fun addWidgetToWorkspace(view: View) =
        launcherActivity.executeOnLauncher {
            view.performClick()
            UiDevice.getInstance(getInstrumentation()).waitForIdle()
            view.findViewById<View>(R.id.widget_add_button).performClick()
        }

    fun isInState(state: LauncherState): Boolean =
        launcherActivity.getFromLauncher { it.stateManager.state == state }!!
}
