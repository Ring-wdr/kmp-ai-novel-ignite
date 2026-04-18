package io.github.ringwdr.novelignite

import io.github.ringwdr.novelignite.navigation.AppDestination

data class TopLevelDestination(
    val route: String,
    val label: String,
)

data class AppBootstrap(
    val topLevelDestinations: List<TopLevelDestination>,
)

fun createAppBootstrap(): AppBootstrap = AppBootstrap(
    topLevelDestinations = AppDestination.topLevelDestinations.map {
        TopLevelDestination(route = it.route, label = it.label)
    },
)
