package io.github.ringwdr.novelignite

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.ringwdr.novelignite.di.appModule
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import io.github.ringwdr.novelignite.features.workshop.FileWorkshopTemplateSelectionPersistence
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.nio.file.Paths

fun main() = application {
    ActiveWorkshopTemplateStore.configure(defaultWorkshopTemplateSelectionPersistence())
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

private fun defaultWorkshopTemplateSelectionPersistence(): FileWorkshopTemplateSelectionPersistence {
    val appDirectory = Paths.get(System.getProperty("user.home"), ".novelignite")
    return FileWorkshopTemplateSelectionPersistence(appDirectory.resolve("active-workshop-template.selection"))
}
