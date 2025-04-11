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

package com.android.launcher3.pageindicators

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.android.launcher3.R

/**
 * Handles logic for the pagination arrow. The foreground and background images and the pressed /
 * hovered state UX.
 */
@SuppressLint("AppCompatCustomView")
class PaginationArrow(context: Context, attrs: AttributeSet) : ImageView(context, attrs) {

    init {
        foreground = ContextCompat.getDrawable(context, R.drawable.ic_chevron_left_rounded_700)
    }

    companion object {
        const val FULLY_OPAQUE = 1f
        const val DISABLED_ARROW_OPACITY = .38f
    }
}
