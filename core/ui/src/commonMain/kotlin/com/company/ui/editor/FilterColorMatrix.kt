package com.company.ui.editor

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.company.domain.model.editor.FilterType
import com.company.domain.model.editor.colorMatrix

/**
 * Color filter that gives the same result as the CSS filter of the design.
 *
 * @return The filter, or null for [FilterType.NONE].
 */
fun FilterType.toColorFilter(): ColorFilter? = colorMatrix()?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
