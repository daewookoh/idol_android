package net.ib.mn.local.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {

    private val gson: Gson = Gson()

    // Date <-> Long (timestamp)
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // List<String> <-> JSON
    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(value: String?): Unit? =
        value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }

    // Map<String, String> <-> JSON
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? =
        map?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? =
        value?.let {
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, mapType)
        }
}