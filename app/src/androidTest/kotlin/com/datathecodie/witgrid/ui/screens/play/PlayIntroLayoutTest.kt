package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import com.datathecodie.witgrid.ui.theme.WitgridTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayIntroLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fullCoachAndUsableBoardFitShortPhone() = assertIntroLayout(fontScale = 1f)

    @Test
    fun fullCoachAndUsableBoardFitShortPhoneWithLargeText() = assertIntroLayout(fontScale = 2f)

    private fun assertIntroLayout(fontScale: Float) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val coachLine = context.getString(R.string.intro_coach_minisudoku)
        var density = 1f
        composeRule.setContent {
            density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                WitgridTheme {
                    Box(Modifier.requiredSize(360.dp, 640.dp)) {
                        PlayScreenLayout(
                            levelRef = LevelRef("minisudoku", "MS-INTRO-01", Pack.EASY),
                            elapsedTimeMillis = 0,
                            canUndo = false,
                            canRedo = false,
                            saveFailed = false,
                            introCoachLine = coachLine,
                            statusMessage = null,
                            hintOperationPending = false,
                            board = { Box(Modifier.fillMaxSize().testTag("intro-board")) },
                            gameControls = {},
                            onUndo = {},
                            onRedo = {},
                            onRequestHint = {},
                            onCheck = {},
                            onBack = {},
                        )
                    }
                }
            }
        }

        val coach = composeRule.onNodeWithText(coachLine)
        coach.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        coach.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse("The entire coach line must be laid out", layouts.single().hasVisualOverflow)
        val coachBounds = coach.fetchSemanticsNode().boundsInRoot
        assertTrue("All coach lines must be visible", coachBounds.height >= layouts.single().size.height - 1f)

        val board = composeRule.onNodeWithTag("intro-board")
        board.performScrollTo().assertIsDisplayed()
        val boardBounds = board.fetchSemanticsNode().boundsInRoot
        assertTrue("Keep a usable six-cell board", boardBounds.height / density >= 300f)
        assertTrue("Keep the board below the top 15%", boardBounds.top / density >= 640f * 0.15f - 1f)
    }
}
