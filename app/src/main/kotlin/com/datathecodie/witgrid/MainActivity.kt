package com.datathecodie.witgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.datathecodie.witgrid.nav.AppNavigator
import com.datathecodie.witgrid.nav.WitgridNavigation
import com.datathecodie.witgrid.data.settings.DataStorePlayerDataRepository
import com.datathecodie.witgrid.data.settings.PlayerData
import com.datathecodie.witgrid.data.settings.ThemeMode
import com.datathecodie.witgrid.ui.theme.Colourway
import com.datathecodie.witgrid.ui.theme.WitgridTheme

class MainActivity : ComponentActivity() {
    private val navigator = AppNavigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val playerDataRepository = remember(this) { DataStorePlayerDataRepository(this) }
            val playerData by playerDataRepository
                .playerData.collectAsState(initial = PlayerData())
            val settings = playerData.settings
            val colourway = Colourway.entries.firstOrNull { it.id == settings.colourwayId }
                ?: Colourway.INDIGO
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            WitgridTheme(colourway = colourway, darkTheme = darkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .imePadding(),
                ) {
                    WitgridNavigation(navigator)
                }
            }
        }
    }
}
