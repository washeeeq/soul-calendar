package org.allatra.calendar.db

import org.joda.time.LocalTime

data class Settings(
    var id: Long,
    var language: String,
    var allowNotifications: Boolean,
    var notificationTime: LocalTime
)