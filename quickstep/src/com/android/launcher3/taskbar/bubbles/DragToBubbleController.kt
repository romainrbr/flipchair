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

package com.android.launcher3.taskbar.bubbles

import android.widget.FrameLayout
import com.android.launcher3.DropTarget.DragObject
import com.android.launcher3.dragndrop.DragController
import com.android.launcher3.dragndrop.DragOptions
import com.android.launcher3.taskbar.TaskbarActivityContext
import com.android.launcher3.taskbar.bubbles.stashing.BubbleStashController
import com.android.quickstep.SystemUiProxy

class DragToBubbleController(
    private val activity: TaskbarActivityContext,
    private val bubbleBarContainer: FrameLayout,
) : DragController.DragListener {

    fun init(
        bubbleBarViewController: BubbleBarViewController,
        bubbleStashController: BubbleStashController,
        systemUiProxy: SystemUiProxy,
    ) {}

    /** Adds bubble bar locations drop zones to the drag controller. */
    fun addBubbleBarDropTargets(dragController: DragController<*>) {}

    /** Removes bubble bar locations drop zones to the drag controller. */
    fun removeBubbleBarDropTargets(dragController: DragController<*>) {}

    /**
     * Runs the provided action once all drop target views are removed from the container. If there
     * are no drop target views currently present or being animated, the action will be executed
     * immediately.
     */
    fun runAfterDropTargetsHidden(afterHiddenAction: Runnable) {
        afterHiddenAction.run()
    }

    override fun onDragStart(dragObject: DragObject, options: DragOptions) {}

    override fun onDragEnd() {}
}
