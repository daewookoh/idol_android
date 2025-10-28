package net.ib.mn.local.model

import net.ib.mn.data.model.InAppBannerPrefsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InAppBannerPrefsTest {

    @Test
    fun `toLocal should map InAppBannerPrefsEntity to InAppBannerPrefs correctly`() {
        val entity = InAppBannerPrefsEntity(
            id = 1,
            imageUrl = "https://example.com/image.png",
            link = "https://example.com",
            section = "M"
        )

        val local = entity.toLocal()

        assertEquals(entity.id, local.id)
        assertEquals(entity.imageUrl, local.imageUrl)
        assertEquals(entity.link, local.link)
        assertEquals(entity.section, local.section)
    }

    @Test
    fun `toData should map InAppBannerPrefs to InAppBannerPrefsEntity correctly`() {
        val local = InAppBannerPrefs(
            id = 2,
            imageUrl = "https://example.com/image2.png",
            link = null,
            section = "S"
        )

        val entity = local.toData()

        assertEquals(local.id, entity.id)
        assertEquals(local.imageUrl, entity.imageUrl)
        assertEquals(local.link, entity.link)
        assertEquals(local.section, entity.section)
    }
}