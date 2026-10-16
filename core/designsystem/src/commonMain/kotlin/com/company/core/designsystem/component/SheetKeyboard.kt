package com.company.core.designsystem.component

/**
 * True when the window of a modal bottom sheet gets the keyboard insets.
 *
 * On Android the sheet is a separate window, and that window gets the keyboard insets. On iOS the M3 sheet dialog
 * moves the whole sheet above the keyboard, and gives zero keyboard insets to the sheet content.
 */
internal expect val SheetWindowHasKeyboardInsets: Boolean
