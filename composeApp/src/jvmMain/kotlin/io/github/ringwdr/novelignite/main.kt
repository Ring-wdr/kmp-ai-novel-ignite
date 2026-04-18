package io.github.ringwdr.novelignite

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.ringwdr.novelignite.di.appModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun main() = application {
    ensureKoinStarted()
    Window(
        onCloseRequest = ::exitApplication,
        title = "kmp-ai-novel-ignite",
    ) {
        App()
    }
}

private fun ensureKoinStarted() {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(appModule)
        }
    }
}
