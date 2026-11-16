package com.datathecodie.witgrid.nav

import com.datathecodie.witgrid.games.LevelRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AppDestination {
    data object Home : AppDestination
    data class Game(val gameId: String) : AppDestination
    data class Play(val levelRef: LevelRef) : AppDestination
    data class Result(val levelRef: LevelRef) : AppDestination
    data class LevelFailed(val levelRef: LevelRef) : AppDestination
    data object Calendar : AppDestination
    data object Statistics : AppDestination
    data object Settings : AppDestination
}

class AppNavigator(startDestination: AppDestination = AppDestination.Home) {
    private val backStack = mutableListOf(startDestination)
    private val mutableDestination = MutableStateFlow(startDestination)

    val destination: StateFlow<AppDestination> = mutableDestination.asStateFlow()

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun navigate(destination: AppDestination) {
        backStack += destination
        mutableDestination.value = destination
    }

    fun goBack(): Boolean {
        if (!canGoBack) return false
        backStack.removeAt(backStack.lastIndex)
        mutableDestination.value = backStack.last()
        return true
    }

    fun returnHome() {
        backStack.clear()
        backStack += AppDestination.Home
        mutableDestination.value = AppDestination.Home
    }
}
