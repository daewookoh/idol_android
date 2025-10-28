package net.ib.mn.chatting.roomMigration

import androidx.room.TypeConverter
import com.google.gson.Gson
import net.ib.mn.addon.IdolGson
import net.ib.mn.model.FriendModel
import net.ib.mn.model.UserModel
import org.json.JSONObject
import java.util.*

class Converters {

    private val gson: Gson? = IdolGson.getInstance()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }


    //notification용  메세지 모델에서 ->  usermodel을  db에 저장하기 위한  converter
    @TypeConverter
    fun userModelToJson(userModel: UserModel?) = gson?.toJson(userModel)


    //notification용 로컬   db에서  usermode 값을 string 으로 받아와 Usermodel로  변환
    @TypeConverter
    fun jsonToUserModel(userModelJson: String?)=  gson?.fromJson(userModelJson, UserModel::class.java)

}