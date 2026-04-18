package io.github.ringwdr.novelignite

data class TopLevelDestination(
    val route: String,
    val label: String,
)

data class AppBootstrap(
    val topLevelDestinations: List<TopLevelDestination>,
)

fun createAppBootstrap(): AppBootstrap = AppBootstrap(
    topLevelDestinations = listOf(
        TopLevelDestination("workshop", "Workshop"),
        TopLevelDestination("templates", "Templates"),
        TopLevelDestination("board", "Board"),
        TopLevelDestination("library", "Library"),
    ),
)
