package com.company.core.designsystem.component

import androidx.compose.runtime.Composable

/**
 * Handles the system back action while [enabled] is true.
 *
 * Android uses the Activity back dispatcher. iOS has no system back button, so it does nothing.
 *
 * @param enabled Set to true to handle the back action.
 * @param onBack Action on back.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
