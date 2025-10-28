package net.ib.mn.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import net.ib.mn.local.model.NotificationLocal
import net.ib.mn.local.room.IdolRoomConstant

@Dao
interface NotificationDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationLocal>)

    @Query("SELECT * FROM ${IdolRoomConstant.Table.NOTIFICATION} ORDER BY createdAt DESC")
    suspend fun getLocalNotifications(): List<NotificationLocal>

    @Query("DELETE FROM ${IdolRoomConstant.Table.NOTIFICATION} WHERE id = :logId")
    suspend fun deleteNotification(logId:Long)

    @Query("DELETE FROM ${IdolRoomConstant.Table.NOTIFICATION}")
    suspend fun clearLocalNotifications()

    @Query("DELETE FROM ${IdolRoomConstant.Table.NOTIFICATION} WHERE createdAt < :time")
    suspend fun deleteOldNotifications(time: Long)
}