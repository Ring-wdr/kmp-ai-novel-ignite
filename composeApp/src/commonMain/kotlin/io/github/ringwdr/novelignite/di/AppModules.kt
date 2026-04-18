package io.github.ringwdr.novelignite.di

import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import org.koin.dsl.module

expect fun createPlatformInferenceEngine(): InferenceEngine

val appModule = module {
    single<InferenceEngine> { createPlatformInferenceEngine() }
}
