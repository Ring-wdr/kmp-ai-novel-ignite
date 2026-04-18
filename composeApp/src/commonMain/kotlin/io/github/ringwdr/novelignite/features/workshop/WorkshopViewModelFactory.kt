package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.InferenceEngine

expect fun createDefaultWorkshopViewModel(inferenceEngine: InferenceEngine): WorkshopViewModel
