package net.ib.mn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.ib.mn.data.model.Idol

/**
 * Room Entity for Idol data.
 * This represents a table in the SQLite database.
 */
@Entity(tableName = "idols")
data class IdolEntity(
    @PrimaryKey
    val id: Int,
    val name: String = "",                 // 기본값 추가 (old 프로젝트와 동일)
    val group: String? = null,
    val imageUrl: String? = null,
    val imageUrl2: String? = null,
    val imageUrl3: String? = null,
    val top3: String? = null,              // comma-separated top3 image IDs
    val top3Type: String? = null,          // comma-separated top3 types
    val top3ImageVer: String? = null,      // comma-separated version numbers for cache busting
    val heartCount: Int = 0,
    val isTop3: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Extension function to convert Entity to Domain model.
 */
fun IdolEntity.toDomain(): Idol {
    return Idol(
        id = id,
        name = name,
        group = group,
        imageUrl = imageUrl,
        heartCount = heartCount,
        isTop3 = isTop3
    )
}

/**
 * Extension function to convert Domain model to Entity.
 */
fun Idol.toEntity(): IdolEntity {
    return IdolEntity(
        id = id,
        name = name,
        group = group,
        imageUrl = imageUrl,
        heartCount = heartCount,
        isTop3 = isTop3
    )
}
