package net.ib.mn.addon

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IdolGson {
    private const val REMOTE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    // articles, comments는 KST로 오고 나머지는 UTC로 와서 부득이...
    @JvmStatic
    fun getInstance(useKST: Boolean = false): Gson {
        val builder = GsonBuilder()
        builder.setDateFormat(REMOTE_DATE_FORMAT)
            .setPrettyPrinting()
            .registerTypeAdapter(Date::class.java, DateDeserializer(useKST))
            .setFieldNamingPolicy(
                FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES
            ).create()
        return builder.create()
    }

    @JvmStatic
    val instance: Gson
        get() = getInstance(false)

    // 서버에서 KST로 보내는 시간을 타임존 반영하여 저장
    class DateDeserializer internal constructor(private val useKST: Boolean = false) :
        JsonSerializer<Date?>, JsonDeserializer<Date?> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            element: JsonElement,
            arg1: Type,
            arg2: JsonDeserializationContext
        ): Date? {
            val date = element.asString
            val formatter = SimpleDateFormat(REMOTE_DATE_FORMAT, Locale.US)
            if (useKST) {
                formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            } else {
                formatter.timeZone = TimeZone.getTimeZone("UTC")
            }
            return try {
                formatter.parse(date)
            } catch (e: ParseException) {
                e.printStackTrace()
                null
            }
        }

        override fun serialize(
            src: Date?, typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            @Suppress("NAME_SHADOWING")
            val src = src ?: return JsonPrimitive("")
            val dateFormat: DateFormat = SimpleDateFormat(UTC_DATE_FORMAT, Locale.US)
            if (useKST) {
                dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            } else {
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            }
            return JsonPrimitive(dateFormat.format(src))
        }
    }
}
