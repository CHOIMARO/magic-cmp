package com.company.ui.editor

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import java.io.File

internal actual fun fileFont(path: String, weight: FontWeight): Font = Font(File(path), weight)
