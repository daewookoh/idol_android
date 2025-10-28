package net.ib.mn.data.model

import net.ib.mn.domain.model.Anniversary
import org.junit.Assert.assertEquals
import org.junit.Test

class AnniversaryMapperTest {

    @Test
    fun `toEntity should map Anniversary to AnniversaryEntity correctly`() {
        // given
        val anniversary = Anniversary(
            idolId = 123,
            anniversary = "D",
            anniversaryDays = 5,
            burningDay = "2025-05-05",
            heart = 999L,
            top3 = "1,2,3",
            top3Type = "P,P,P"
        )

        // when
        val entity = anniversary.toEntity()

        // then
        assertEquals(anniversary.idolId, entity.idolId)
        assertEquals(anniversary.anniversary, entity.anniversary)
        assertEquals(anniversary.anniversaryDays, entity.anniversaryDays)
        assertEquals(anniversary.burningDay, entity.burningDay)
        assertEquals(anniversary.heart, entity.heart)
        assertEquals(anniversary.top3, entity.top3)
        assertEquals(anniversary.top3Type, entity.top3Type)
    }
}
