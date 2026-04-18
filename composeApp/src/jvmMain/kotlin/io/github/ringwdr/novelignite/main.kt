package io.github.ringwdr.novelignite

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "kmp-ai-novel-ignite",
    ) {
        App()
    }
}