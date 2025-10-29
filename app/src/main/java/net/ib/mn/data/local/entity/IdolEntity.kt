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
    val name: String,
    val group: String?,
    val imageUrl: String?,
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
