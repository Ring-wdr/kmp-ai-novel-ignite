package io.github.ringwdr.novelignite.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AppDestination {
    abstract val route: String
    abstract val label: String

    @Serializable
    @SerialName("workshop")
    data object Workshop : AppDestination() {
        override val route: String = "workshop"
        override val label: String = "Workshop"
    }

    @Serializable
    @SerialName("templates")
    data object Templates : AppDestination() {
        override val route: String = "templates"
        override val label: String = "Templates"
    }

    @Serializable
    @SerialName("board")
    data object Board : AppDestination() {
        override val route: String = "board"
        override val label: String = "Board"
    }

    @Serializable
    @SerialName("library")
    data object Library : AppDestination() {
        override val route: String = "library"
        override val label: String = "Library"
    }

    companion object {
        val topLevelDestinations: List<AppDestination> = listOf(Workshop, Templates, Board, Library)

        fun fromRoute(route: String): AppDestination = topLevelDestinations.firstOrNull { it.route == route }
            ?: error("Unknown app destination route: $route")
    }
}
