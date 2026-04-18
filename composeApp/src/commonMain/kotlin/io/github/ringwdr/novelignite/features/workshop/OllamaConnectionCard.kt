package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OllamaConnectionCard(
    baseUrl: String = "http://localhost:11434",
    modelName: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Local Ollama")
            Text("Endpoint: $baseUrl")
            if (modelName != null) {
                Text("Model: $modelName")
            }
        }
    }
}
