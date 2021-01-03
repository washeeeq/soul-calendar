package org.allatra.calendar.data.db.conv

import androidx.room.TypeConverter
import org.joda.time.DateTime
import org.joda.time.Instant
import org.joda.time.LocalTime
import java.util.*

class Converters {
    @TypeConverter
    fun toNotificationTime(value: Long) = LocalTime.fromMillisOfDay(value)

    @TypeConverter
    fun fromNotificationTime(value: LocalTime) = value.millisOfDay

    @TypeConverter
    fun toLastDownloadAt(value: Long) = DateTime(value)

    @TypeConverter
    fun fromLastDownloadAt(value: DateTime) = value.millis
}
