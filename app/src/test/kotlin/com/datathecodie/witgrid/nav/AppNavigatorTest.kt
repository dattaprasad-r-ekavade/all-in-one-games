package com.datathecodie.witgrid.nav

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppNavigatorTest {
    @Test
    fun gamePlayResultFlowSupportsBackNavigation() {
        val navigator = AppNavigator()
        val levelRef = LevelRef("binary", "BI-EASY-001", Pack.EASY)
        navigator.navigate(AppDestination.Game(levelRef.gameId))
        navigator.navigate(AppDestination.Play(levelRef))
        navigator.navigate(AppDestination.Result(levelRef))

        assertEquals(AppDestination.Result(levelRef), navigator.destination.value)
        assertTrue(navigator.goBack())
        assertEquals(AppDestination.Play(levelRef), navigator.destination.value)
        assertTrue(navigator.goBack())
        assertEquals(AppDestination.Game(levelRef.gameId), navigator.destination.value)
    }

    @Test
    fun homeIsTheLastBackDestinationAndUtilitiesCanReturnHome() {
        val navigator = AppNavigator()
        navigator.navigate(AppDestination.Settings)

        assertTrue(navigator.goBack())
        assertEquals(AppDestination.Home, navigator.destination.value)
        assertFalse(navigator.goBack())

        navigator.navigate(AppDestination.Calendar)
        navigator.returnHome()
        assertEquals(AppDestination.Home, navigator.destination.value)
        assertFalse(navigator.canGoBack)
    }
}
