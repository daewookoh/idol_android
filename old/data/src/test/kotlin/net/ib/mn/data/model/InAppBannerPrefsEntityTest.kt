package net.ib.mn.data.model

import net.ib.mn.domain.model.InAppBanner
import org.junit.Assert.assertEquals
import org.junit.Test

class InAppBannerPrefsEntityMapperTest {

    @Test
    fun `toDomain should map InAppBannerPrefsEntity to InAppBanner correctly`() {
        // given
        val entity = InAppBannerPrefsEntity(
            id = 1,
            imageUrl = "https://example.com/image.png",
            link = "https://example.com",
            section = "M"
        )

        // when
        val domain = entity.toDomain()

        // then
        assertEquals(entity.id, domain.id)
        assertEquals(entity.imageUrl, domain.imageUrl)
        assertEquals(entity.link, domain.link)
        assertEquals(entity.section, domain.section)
    }

    @Test
    fun `toEntity should map InAppBanner to InAppBannerPrefsEntity correctly`() {
        // given
        val domain = InAppBanner(
            id = 2,
            imageUrl = "https://cdn.com/banner.png",
            link = null,
            section = "S"
        )

        // when
        val entity = domain.toEntity()

        // then
        assertEquals(domain.id, entity.id)
        assertEquals(domain.imageUrl, entity.imageUrl)
        assertEquals(domain.link, entity.link)
        assertEquals(domain.section, entity.section)
    }
}