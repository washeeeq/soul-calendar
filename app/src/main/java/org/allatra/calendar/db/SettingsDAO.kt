package org.allatra.calendar.db
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.joda.time.LocalTime

open class SettingsDAO: RealmObject() {
    @PrimaryKey
    private var id: Long = 1
    private var language: String = ""
    private var allowNotifications: Boolean = false
    private var notificationTime: String = ""

    fun getId(): Long = id
    fun setId(id: Long){
        this.id = id
    }

    fun getLanguage(): String = language
    fun setLanguage(value: String){
        this.language = value
    }

    fun getAllowNotifications(): Boolean = allowNotifications
    fun setAllowNotifications(value: Boolean){
        this.allowNotifications = value
    }

    fun getNotificationTimeString(): String = notificationTime
    fun getNotificationTime(): LocalTime {
        return LocalTime.parse(notificationTime)
    }

    fun setNotificationTime(value: String){
        this.notificationTime = value
    }

    override fun toString(): String {
        return "SettingsDAO = { id = $id, language = $language, allowNotifications = $allowNotifications, notificationTime = $notificationTime}"
    }
}