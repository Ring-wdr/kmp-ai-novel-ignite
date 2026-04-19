package io.github.ringwdr.novelignite.navigation

import androidx.compose.runtime.Composable
import io.github.ringwdr.novelignite.features.board.BoardScreen
import io.github.ringwdr.novelignite.features.library.LibraryScreen
import io.github.ringwdr.novelignite.features.templates.TemplatesScreen
import io.github.ringwdr.novelignite.features.workshop.WorkshopScreen

@Composable
fun AppNavHost(
    destination: AppDestination,
    onNavigate: (AppDestination) -> Unit = {},
) {
    when (destination) {
        AppDestination.Workshop -> WorkshopScreen()
        AppDestination.Templates -> TemplatesScreen()
        AppDestination.Board -> BoardScreen(
            onOpenTemplates = { onNavigate(AppDestination.Templates) },
        )
        AppDestination.Library -> LibraryScreen()
    }
}
