package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatPanel(
    generatedText: String,
    isGenerating: Boolean,
    onContinueScene: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Workshop Chat")
            Text(
                text = generatedText.ifBlank { "Ask the workshop to continue the scene." },
            )
            Button(
                onClick = onContinueScene,
                enabled = !isGenerating,
            ) {
                Text(if (isGenerating) "Continuing..." else "Continue scene")
            }
        }
    }
}
