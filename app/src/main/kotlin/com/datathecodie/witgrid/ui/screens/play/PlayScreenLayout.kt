package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.components.Chip
import com.datathecodie.witgrid.ui.components.IconActionButton
import com.datathecodie.witgrid.ui.components.ToolBarItem
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayScreenLayout(
    levelRef: LevelRef,
    elapsedTimeMillis: Long,
    canUndo: Boolean,
    canRedo: Boolean,
    saveFailed: Boolean,
    introCoachLine: String?,
    statusMessage: String?,
    hintOperationPending: Boolean,
    enlargedBoardMode: Boolean = false,
    onToggleEnlargedBoard: () -> Unit = {},
    board: @Composable () -> Unit,
    gameControls: @Composable () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRequestHint: () -> Unit,
    onBack: () -> Unit,
    onRestart: () -> Unit = {},
    hintCredits: String? = null,
    modeTools: (@Composable () -> Unit)? = null,
) {
    var timerVisible by remember(levelRef) { mutableStateOf(true) }
    var rulesVisible by remember(levelRef) { mutableStateOf(false) }
    var restartVisible by remember(levelRef) { mutableStateOf(false) }
    val colors = WitgridTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = isLandscapeLayout(maxWidth, maxHeight)
        Scaffold(
            containerColor = colors.paper,
            contentColor = colors.ink,
            topBar = {
                PlayHeader(levelRef, elapsedTimeMillis, timerVisible, enlargedBoardMode,
                    onBack, { timerVisible = !timerVisible }, onToggleEnlargedBoard, { rulesVisible = true })
            },
            bottomBar = {
                if (!landscape) {
                    Column {
                        HorizontalDivider(color = colors.cardEdge)
                        PlayToolbar(
                            canUndo = canUndo,
                            canRedo = canRedo,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onRestart = { restartVisible = true },
                            onRequestHint = onRequestHint,
                            hintCredits = hintCredits,
                            hintOperationPending = hintOperationPending,
                            modeTools = modeTools,
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (landscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayBoard(
                            board = board,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f),
                            verticalPadding = 4.dp,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            introCoachLine?.let {
                                Text(
                                    it,
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            PlayMessages(null, statusMessage, saveFailed, enlargedBoardMode)
                            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                gameControls()
                            }
                            HorizontalDivider(color = colors.cardEdge, modifier = Modifier.padding(vertical = 4.dp))
                            PlayToolbar(
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onUndo = onUndo,
                                onRedo = onRedo,
                                onRestart = { restartVisible = true },
                                onRequestHint = onRequestHint,
                                hintCredits = hintCredits,
                                hintOperationPending = hintOperationPending,
                                modeTools = modeTools,
                            )
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().widthIn(max = 840.dp).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        introCoachLine?.let {
                            Text(it, Modifier.fillMaxWidth().padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(
                            Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            PlayBoard(board, Modifier.weight(1f, fill = false).fillMaxWidth(), verticalPadding = 16.dp)
                            PlayMessages(null, statusMessage, saveFailed, enlargedBoardMode)
                            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { gameControls() }
                        }
                    }
                }
            }
        }
    }
    if (rulesVisible) {
        ModalBottomSheet(onDismissRequest = { rulesVisible = false }, containerColor = colors.card) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text(stringResource(R.string.play_rules_title, stringResource(gameNameResource(levelRef.gameId))),
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(playRulesResource(levelRef.gameId)), Modifier.padding(top = 16.dp, bottom = 32.dp),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (restartVisible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { restartVisible = false },
            title = { Text(stringResource(R.string.button_restart)) },
            text = { Text(stringResource(R.string.play_restart_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { restartVisible = false; onRestart() }) {
                    Text(stringResource(R.string.button_restart))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { restartVisible = false }) {
                    Text(stringResource(R.string.play_cancel))
                }
            },
        )
    }
}

/** Compact assist actions leave room for the board and preserve the action icons. */
@Composable
private fun RowScope.LargeTextAction(
    icon: Painter,
    label: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    badge: String? = null,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = WitgridTheme.colors.ink,
            disabledContentColor = WitgridTheme.colors.mutedInk.copy(alpha = 0.38f),
        ),
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(24.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, softWrap = false, maxLines = 1)
        badge?.let {
            Text(it, Modifier.padding(start = 4.dp).background(WitgridTheme.colors.accent, CircleShape)
                .padding(horizontal = 5.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall, color = WitgridTheme.colors.onAccent)
        }
    }
}

@Composable
private fun PlayToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRestart: () -> Unit,
    onRequestHint: () -> Unit,
    hintCredits: String?,
    hintOperationPending: Boolean,
    modeTools: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val toolbarTypography = MaterialTheme.typography.copy(
        labelMedium = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
    )
    MaterialTheme(typography = toolbarTypography) {
        if (LocalDensity.current.fontScale > 1.1f) {
            Column(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).testTag("play-toolbar")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ToolBarItem(playIcon(PlayIcon.UNDO), stringResource(R.string.button_undo), onUndo,
                        Modifier.weight(1f), enabled = canUndo)
                    ToolBarItem(playIcon(PlayIcon.REDO), stringResource(R.string.button_redo), onRedo,
                        Modifier.weight(1f), enabled = canRedo)
                    if (modeTools == null) {
                        ToolBarItem(playIcon(PlayIcon.RESTART), stringResource(R.string.button_restart),
                            onRestart, Modifier.weight(1f))
                    } else {
                        Row(Modifier.weight(2f)) { modeTools() }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    LargeTextAction(playIcon(PlayIcon.HINT), R.string.button_hint,
                        onRequestHint, !hintOperationPending, hintCredits)
                }
            }
        } else {
            Row(
                modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).testTag("play-toolbar"),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToolBarItem(playIcon(PlayIcon.UNDO), stringResource(R.string.button_undo), onUndo,
                    Modifier.weight(1f), enabled = canUndo)
                ToolBarItem(playIcon(PlayIcon.REDO), stringResource(R.string.button_redo), onRedo,
                    Modifier.weight(1f), enabled = canRedo)
                if (modeTools == null) {
                    ToolBarItem(playIcon(PlayIcon.RESTART), stringResource(R.string.button_restart),
                        onRestart, Modifier.weight(1f))
                } else {
                    Row(Modifier.weight(2f)) { modeTools() }
                }
                ToolBarItem(playIcon(PlayIcon.HINT), stringResource(R.string.button_hint), onRequestHint,
                    Modifier.weight(1.1f), badge = hintCredits, enabled = !hintOperationPending)
            }
        }
    }
}

@Composable
private fun PlayBoard(board: @Composable () -> Unit, modifier: Modifier, verticalPadding: Dp = 16.dp) {
    BoxWithConstraints(modifier.padding(vertical = verticalPadding), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight).coerceAtLeast(1.dp)
        Box(Modifier.size(side).testTag("play-board"), contentAlignment = Alignment.Center) { board() }
    }
}

@Composable
private fun PlayMessages(coach: String?, status: String?, saveFailed: Boolean, enlarged: Boolean) {
    val colors = WitgridTheme.colors
    coach?.let { Text(it, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium) }
    status?.let {
        Text(it, Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("play-status"),
            style = MaterialTheme.typography.bodySmall, color = colors.mutedInk)
    }
    if (saveFailed) Text(stringResource(R.string.play_save_failed), color = MaterialTheme.colorScheme.error)
    if (enlarged) Text(stringResource(R.string.board_enlarged_gesture_help), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun PlayHeader(
    levelRef: LevelRef, elapsed: Long, timerVisible: Boolean, enlarged: Boolean,
    onBack: () -> Unit, onTimer: () -> Unit, onZoom: () -> Unit, onRules: () -> Unit,
) {
    val subtitle = when {
        levelRef.daily -> stringResource(R.string.play_daily_subtitle, stringResource(packNameResource(levelRef.pack)))
        levelRef.isIntro -> stringResource(R.string.play_intro_subtitle, stringResource(packNameResource(levelRef.pack)))
        else -> stringResource(R.string.play_level_subtitle, stringResource(packNameResource(levelRef.pack)),
            levelRef.levelNumber ?: ((levelRef.bankIndex ?: 0) + 1L))
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        val compact = isPlayHeaderCompact(maxWidth, LocalDensity.current.fontScale)
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconActionButton(painterResource(R.drawable.ic_back), stringResource(R.string.button_back), onBack)
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(stringResource(gameNameResource(levelRef.gameId)), Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (!compact) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = WitgridTheme.colors.mutedInk)
                }
                if (!compact) {
                    ZoomAction(enlarged, onZoom)
                    TimerChip(elapsed, timerVisible, onTimer)
                }
                IconActionButton(painterResource(R.drawable.ic_info), stringResource(R.string.play_rules), onRules)
            }
            if (compact) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subtitle, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = WitgridTheme.colors.mutedInk)
                    TimerChip(elapsed, timerVisible, onTimer)
                    ZoomAction(enlarged, onZoom)
                }
            }
        }
    }
}

@Composable
private fun ZoomAction(enlarged: Boolean, onZoom: () -> Unit) {
    IconActionButton(playIcon(if (enlarged) PlayIcon.FIT else PlayIcon.ZOOM),
        stringResource(if (enlarged) R.string.button_board_fit else R.string.button_board_enlarge), onZoom)
}

@Composable
private fun TimerChip(elapsed: Long, visible: Boolean, onTimer: () -> Unit) {
    val description = stringResource(if (visible) R.string.button_hide_timer else R.string.button_show_timer)
    Chip(if (visible) formatDuration(elapsed) else stringResource(R.string.play_timer_hidden),
        Modifier.semantics { contentDescription = description }, onClick = onTimer)
}

// The side column needs room for the keypad and toolbar; near-square windows keep the stacked layout.
internal fun isLandscapeLayout(maxWidth: Dp, maxHeight: Dp): Boolean = maxWidth - maxHeight >= LANDSCAPE_SIDE_COLUMN_MIN

private val LANDSCAPE_SIDE_COLUMN_MIN = 240.dp

internal fun isPlayHeaderCompact(maxWidth: Dp, fontScale: Float): Boolean =
    (maxWidth < 440.dp || fontScale > 1.1f) && maxWidth < 600.dp

internal fun packNameResource(pack: Pack): Int = when (pack) {
    Pack.EASY -> R.string.pack_easy
    Pack.MEDIUM -> R.string.pack_medium
    Pack.HARD -> R.string.pack_hard
    Pack.EXPERT -> R.string.pack_expert
}

internal fun playRulesResource(gameId: String): Int = when (gameId) {
    "crowns" -> R.string.play_rules_crowns
    "binary" -> R.string.play_rules_binary
    "nonogram" -> R.string.play_rules_nonogram
    "sudoku" -> R.string.play_rules_sudoku
    "skyscrapers" -> R.string.play_rules_skyscrapers
    "futoshiki" -> R.string.play_rules_futoshiki
    "path" -> R.string.play_rules_path
    "killersudoku" -> R.string.play_rules_killersudoku
    "arrows" -> R.string.play_rules_arrows
    "coloursort" -> R.string.play_rules_coloursort
    "mosaic" -> R.string.play_rules_mosaic
    "klondike" -> R.string.play_rules_klondike
    "tactics" -> R.string.play_rules_tactics
    else -> R.string.play_rules_minisudoku
}
