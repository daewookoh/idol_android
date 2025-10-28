package net.ib.mn.local.model

import net.ib.mn.data.model.IdolEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdolMapperTest {

    @Test
    fun `toLocal should map IdolEntity with non-null optional fields correctly`() {
        val entity = IdolEntity(
            id = 1,
            name = "IU",
            description = "Solo singer",
            heart = 1234,
            miracleCount = 1,
            angelCount = 2,
            rookieCount = 3,
            anniversary = "Y",
            anniversaryDays = 100,
            birthDay = "1993-05-16",
            burningDay = "2024-04-01",
            category = "solo",
            comebackDay = "2025-01-01",
            debutDay = "2008-09-18",
            fairyCount = 5,
            groupId = 0,
            imageUrl = "img1",
            imageUrl2 = "img2",
            imageUrl3 = "img3",
            isViewable = "Y",
            nameEn = "IU",
            nameJp = "アイユー",
            nameZh = "艾悠",
            nameZhTw = "艾悠台灣",
            resourceUri = "/resource/iu",
            top3 = "top3Data",
            top3Type = "typeA",
            top3Seq = 10,
            type = "artist",
            infoSeq = 999,
            isLunarBirthday = "N",
            mostCount = 50,
            mostCountDesc = "most loved",
            updateTs = 1680000000,
            sourceApp = "AwardsApp",
            fdName = "아이유",
            fdNameEn = "IU English",
            top3ImageVer = "img3_extra"
        )

        val local = entity.toLocal()

        // assertion 예시 (전부는 아니고 대표적인 것만)
        assertEquals(entity.birthDay, local.birthDay)
        assertEquals(entity.imageUrl3, local.imageUrl3)
        assertEquals(entity.isLunarBirthday, local.isLunarBirthday)
        assertEquals(entity.sourceApp, local.sourceApp)
    }

    @Test
    fun `toLocal should map IdolEntity with null optional fields correctly`() {
        val entity = IdolEntity(
            id = 2,
            name = "NullTester",
            description = "testing null fields",
            heart = 0,
            miracleCount = 0,
            angelCount = 0,
            rookieCount = 0,
            anniversary = "N",
            anniversaryDays = null,
            birthDay = null,
            burningDay = null,
            category = "",
            comebackDay = null,
            debutDay = null,
            fairyCount = 0,
            groupId = 0,
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
            isViewable = "N",
            nameEn = "",
            nameJp = "",
            nameZh = "",
            nameZhTw = "",
            resourceUri = "",
            top3 = null,
            top3Type = null,
            top3Seq = -1,
            type = "",
            infoSeq = -1,
            isLunarBirthday = null,
            mostCount = 0,
            mostCountDesc = null,
            updateTs = 0,
            sourceApp = null,
            fdName = "아이유",
            fdNameEn = "IU English",
            top3ImageVer = "img3_extra"
        )

        val local = entity.toLocal()

        assertNull(local.anniversaryDays)
        assertNull(local.birthDay)
        assertNull(local.imageUrl)
        assertNull(local.imageUrl2)
        assertNull(local.imageUrl3)
        assertNull(local.sourceApp)
    }

    @Test
    fun `toData should handle non-null fields correctly`() {
        val local = IdolLocal(
            id = 3,
            name = "G-Dragon",
            description = "Rapper",
            heart = 5000,
            isLunarBirthday = "Y",
            imageUrl = "gdragon1",
            imageUrl2 = "gdragon2",
            imageUrl3 = "gdragon3",
            top3 = "VIPs",
            top3Type = "Fandom",
            mostCountDesc = "Top Idol",
            sourceApp = "AwardsApp"
        )

        val entity = local.toData()

        assertEquals(local.imageUrl, entity.imageUrl)
        assertEquals(local.isLunarBirthday, entity.isLunarBirthday)
        assertEquals(local.sourceApp, entity.sourceApp)
    }

    @Test
    fun `toData should handle null optional fields correctly`() {
        val local = IdolLocal(
            id = 4,
            name = "NullLocal",
            description = "Null test",
            heart = 0,
            anniversaryDays = null,
            birthDay = null,
            burningDay = null,
            comebackDay = null,
            debutDay = null,
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
            isLunarBirthday = null,
            top3 = null,
            top3Type = null,
            mostCountDesc = null,
            sourceApp = null
        )

        val entity = local.toData()

        assertNull(entity.birthDay)
        assertNull(entity.imageUrl)
        assertNull(entity.imageUrl3)
        assertNull(entity.isLunarBirthday)
        assertNull(entity.sourceApp)
    }
}