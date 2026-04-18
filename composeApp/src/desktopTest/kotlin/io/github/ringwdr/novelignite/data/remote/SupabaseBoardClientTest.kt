package io.github.ringwdr.novelignite.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseBoardClientTest {
    @Test
    fun mapTemplatePost_returnsBoardCard() {
        val dto = TemplatePostDto(
            id = "post-1",
            title = "Noir Seoul",
            summary = "Ghost debts in neon rain",
            likes = 12,
            remixes = 4
        )

        val model = dto.toBoardCard()

        assertEquals("Noir Seoul", model.title)
        assertEquals(4, model.remixCount)
    }
}
