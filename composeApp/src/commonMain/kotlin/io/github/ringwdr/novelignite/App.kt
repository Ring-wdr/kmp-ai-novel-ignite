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
        val bootstrap = remember { createAppBootstrap() }
        var currentDestination by remember {
            mutableStateOf(AppDestination.fromRoute(bootstrap.topLevelDestinations.first().route))
        }

        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            PrimaryTabRow(
                selectedTabIndex = bootstrap.topLevelDestinations.indexOfFirst {
                    it.route == currentDestination.route
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                bootstrap.topLevelDestinations.forEach { topLevelDestination ->
                    val destination = AppDestination.fromRoute(topLevelDestination.route)
                    Tab(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        text = { Text(topLevelDestination.label) },
                    )
                }
            }

            AppNavHost(currentDestination)
        }
    }
}
