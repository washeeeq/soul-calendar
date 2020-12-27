package org.allatra.calendar.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.joda.time.LocalTime

/**
 * Object that is used to filter/narrow down search results.
 */
@Entity
data class UserSettings(
    @PrimaryKey
    val uid: Int,
    @ColumnInfo(name = "api_language_id") var apiLanguageId: String,
    @ColumnInfo(name = "send_notifications") var sendNotifications: Boolean,
    @ColumnInfo(name = "notification_time") var notificationTime: LocalTime
)
