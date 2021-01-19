package org.allatra.calendar.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.joda.time.DateTime
import org.joda.time.LocalTime
import java.util.*

/**
 * Object that is used to filter/narrow down search results.
 */
@Entity
data class Motivator(
    @PrimaryKey
    val uid: Int,
    @ColumnInfo(name = "last_download_at") var lastDownloadAt: DateTime
)
