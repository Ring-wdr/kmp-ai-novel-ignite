package io.github.ringwdr.novelignite.di

import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import org.koin.dsl.module

val appModule = module {
    single<InferenceEngine> { FakeInferenceEngine() }
}
