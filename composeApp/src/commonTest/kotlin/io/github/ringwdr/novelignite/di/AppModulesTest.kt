package io.github.ringwdr.novelignite.di

import io.github.ringwdr.novelignite.data.inference.LocalOllamaEngine
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertIs
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin

class AppModulesTest {
    @Test
    fun appModule_bindsInferenceEngineToPlatformImplementation() {
        stopKoin()
        startKoin {
            modules(appModule)
        }

        val inferenceEngine = getKoin().get<InferenceEngine>()

        assertIs<LocalOllamaEngine>(inferenceEngine)
        stopKoin()
    }
}
