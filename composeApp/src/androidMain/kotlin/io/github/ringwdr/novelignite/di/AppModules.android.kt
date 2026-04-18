package io.github.ringwdr.novelignite.di

import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine

actual fun createPlatformInferenceEngine(): InferenceEngine = FakeInferenceEngine()
