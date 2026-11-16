package com.datathecodie.witgrid.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Stable
class BoardHaptics internal constructor(
    private val enabled: Boolean,
    private val hapticFeedback: HapticFeedback,
) {
    fun mark() = performIfEnabled(HapticFeedbackType.SegmentTick)

    fun placement() = performIfEnabled(HapticFeedbackType.Confirm)

    fun clear() = performIfEnabled(HapticFeedbackType.LongPress)

    fun undo() = performIfEnabled(HapticFeedbackType.GestureEnd)

    private fun performIfEnabled(type: HapticFeedbackType) {
        if (enabled) hapticFeedback.performHapticFeedback(type)
    }
}

@Composable
fun rememberBoardHaptics(enabled: Boolean): BoardHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback, enabled) { BoardHaptics(enabled, hapticFeedback) }
}
