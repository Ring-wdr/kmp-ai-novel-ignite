package io.github.ringwdr.novelignite

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.ringwdr.novelignite.di.appModule
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import io.github.ringwdr.novelignite.features.workshop.FileWorkshopTemplateSelectionPersistence
import io.github.ringwdr.novelignite.features.workshop.workshopStreamMode
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.nio.file.Paths

fun main() = application {
    if (workshopStreamMode() == "fixture") {
        println("Workshop typed stream fixture mode enabled. Set NOVEL_IGNITE_WORKSHOP_STREAM_MODE=fixture to replay the manual validation path.")
    }
    ActiveWorkshopTemplateStore.configure(defaultWorkshopTemplateSelectionPersistence())
    ensureKoinStarted()
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 980.dp,
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "kmp-ai-novel-ignite",
        state = windowState,
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

private fun defaultWorkshopTemplateSelectionPersistence(): FileWorkshopTemplateSelectionPersistence {
    val appDirectory = Paths.get(System.getProperty("user.home"), ".novelignite")
    return FileWorkshopTemplateSelectionPersistence(appDirectory.resolve("active-workshop-template.selection"))
}
