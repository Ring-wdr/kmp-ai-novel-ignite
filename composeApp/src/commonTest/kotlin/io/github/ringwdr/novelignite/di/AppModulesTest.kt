package io.github.ringwdr.novelignite.di

import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertIs
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin

class AppModulesTest {
    @Test
    fun appModule_bindsInferenceEngineToFakeInferenceEngine() {
        stopKoin()
        startKoin {
            modules(appModule)
        }

        val inferenceEngine = getKoin().get<InferenceEngine>()

        assertIs<FakeInferenceEngine>(inferenceEngine)
        stopKoin()
    }
}
