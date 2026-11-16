package com.datathecodie.witgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.datathecodie.witgrid.nav.AppNavigator
import com.datathecodie.witgrid.nav.WitgridNavigation
import com.datathecodie.witgrid.ui.theme.WitgridTheme

class MainActivity : ComponentActivity() {
    private val navigator = AppNavigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WitgridTheme {
                WitgridNavigation(navigator)
            }
        }
    }
}
