package com.datathecodie.witgrid.ui.screens.failed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.ui.layout.AdaptiveScreen

@Composable
fun LevelFailedScreen(
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
) {
    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.screen_level_failed), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.failed_continue_placeholder), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onContinue, enabled = false) {
                Text(stringResource(R.string.button_continue))
            }
            OutlinedButton(onClick = onRestart) {
                Text(stringResource(R.string.button_restart))
            }
            OutlinedButton(onClick = onHome) {
                Text(stringResource(R.string.button_home))
            }
        }
    }
}
