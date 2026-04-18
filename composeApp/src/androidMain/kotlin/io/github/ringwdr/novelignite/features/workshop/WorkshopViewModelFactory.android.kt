package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.InferenceEngine

actual fun createDefaultWorkshopViewModel(inferenceEngine: InferenceEngine): WorkshopViewModel =
    WorkshopViewModel(inferenceEngine = inferenceEngine)
