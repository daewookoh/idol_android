package net.ib.mn.data.model

import net.ib.mn.domain.model.IdolFiledData
import org.junit.Assert.assertEquals
import org.junit.Test

class IdolFiledDataEntityMapperTest {

    @Test
    fun `toIdolFiledDataEntity should correctly map IdolFiledData to IdolFiledDataEntity`() {
        // given
        val data = IdolFiledData(
            id = 100,
            heart = 1234L,
            top3 = "101,102,103",
            top3Type = "P,P,V",
            top3ImageVer = "",
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
        )

        // when
        val entity = data.toIdolFiledDataEntity()

        // then
        assertEquals(data.id, entity.id)
        assertEquals(data.heart, entity.heart)
        assertEquals(data.top3, entity.top3)
        assertEquals(data.top3Type, entity.top3Type)
    }
}
