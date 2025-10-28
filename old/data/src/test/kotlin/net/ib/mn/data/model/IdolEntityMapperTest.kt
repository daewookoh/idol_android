package net.ib.mn.data.model

import net.ib.mn.domain.model.Idol
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class IdolEntityMapperTest {

    @Test
    fun `toDomain should correctly map IdolEntity to Idol`() {
        // given
        val entity = IdolEntity(
            id = 1,
            miracleCount = 3,
            angelCount = 2,
            rookieCount = 1,
            anniversary = "Y",
            anniversaryDays = 100,
            birthDay = "2000-01-01",
            burningDay = "2025-04-01",
            category = "vocal",
            comebackDay = "2025-05-01",
            debutDay = "2020-01-01",
            description = "Popular Idol",
            fairyCount = 5,
            groupId = 10,
            heart = 999L,
            imageUrl = "url1",
            imageUrl2 = "url2",
            imageUrl3 = "url3",
            isViewable = "Y",
            name = "IU",
            nameEn = "IU",
            nameJp = "아이유",
            nameZh = "爱尤",
            nameZhTw = "愛尤",
            resourceUri = "resUri",
            top3 = "1,2,3",
            top3Type = "P,P,P",
            top3Seq = 1,
            type = "solo",
            infoSeq = 101,
            isLunarBirthday = "N",
            mostCount = 30,
            mostCountDesc = "Most awarded",
            updateTs = 123456,
            sourceApp = "app",
            fdName = "Fan Name",
            fdNameEn = "Fan Name En",
            top3ImageVer = "img1"
        )

        // when
        val domain = entity.toDomain()

        // then
        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.heart, domain.heart)
        assertEquals(entity.top3, domain.top3)
    }

    @Test
    fun `toIdolEntity should correctly map Idol to IdolEntity`() {
        // given
        val idol = Idol(
            id = 2,
            miracleCount = 1,
            angelCount = 0,
            rookieCount = 2,
            anniversary = "N",
            anniversaryDays = null,
            birthDay = null,
            burningDay = null,
            category = "dance",
            comebackDay = null,
            debutDay = "2021-01-01",
            description = "New Idol",
            fairyCount = 1,
            groupId = 20,
            heart = 1234L,
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
            isViewable = "Y",
            name = "Newbie",
            nameEn = "Newbie",
            nameJp = "뉴비",
            nameZh = "新人",
            nameZhTw = "新人",
            resourceUri = "resource",
            top3 = null,
            top3Type = null,
            top3Seq = 2,
            type = "group",
            infoSeq = 202,
            isLunarBirthday = null,
            mostCount = 10,
            mostCountDesc = "Rookie award",
            updateTs = 654321,
            sourceApp = "mobile",
            fdName = "Fan Name",
            fdNameEn = "Fan Name En",
            top3ImageVer = "img3"
        )

        // when
        val entity = idol.toIdolEntity()

        // then
        assertEquals(idol.id, entity.id)
        assertEquals(idol.name, entity.name)
        assertEquals(idol.heart, entity.heart)
        assertEquals(idol.type, entity.type)
        assertEquals(idol.birthDay, entity.birthDay)
    }

    @Test
    fun `toDomain should throw NPE if IdolEntity is null`() {
        val entity: IdolEntity? = null

        assertFailsWith<NullPointerException> {
            entity!!.toDomain()
        }
    }
}
