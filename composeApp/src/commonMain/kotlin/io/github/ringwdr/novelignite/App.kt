package io.github.ringwdr.novelignite

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.ringwdr.novelignite.navigation.AppDestination
import io.github.ringwdr.novelignite.navigation.AppNavHost

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentDestination by remember {
            mutableStateOf(AppDestination.topLevelDestinations.first())
        }

        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            PrimaryTabRow(
                selectedTabIndex = AppDestination.topLevelDestinations.indexOf(currentDestination),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppDestination.topLevelDestinations.forEach { destination ->
                    Tab(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        text = { Text(destination.label) },
                    )
                }
            }

            AppNavHost(
                destination = currentDestination,
                onNavigate = { currentDestination = it },
            )
        }
    }
}
