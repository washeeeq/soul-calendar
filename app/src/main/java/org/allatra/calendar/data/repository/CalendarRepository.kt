package org.allatra.calendar.data.repository

import org.allatra.calendar.data.api.ApiClient
import org.allatra.calendar.data.db.dao.MotivatorDao
import org.allatra.calendar.data.db.dao.UserSettingsDao
import org.allatra.calendar.data.db.entity.Motivator
import org.allatra.calendar.data.db.entity.UserSettings

/**
 * This class is the only single point of contact for the ViewModel.
 */
class CalendarRepository(private val userSettingsDao: UserSettingsDao, private val motivatorDao: MotivatorDao) {

    /**
     * Returns resource of book languages.
     */
    fun getLanguagesFromApi() = ApiClient.getLanguages()

    /**
     * Returns user stored settings.
     */
    fun getUserSettings() = userSettingsDao.getUserSettings()

    /**
     * Inserts new settings.
     */
    fun insertUserSettings(userSettings: UserSettings) = userSettingsDao.insert(userSettings)

    /**
     * Updates existing settings.
     */
    fun updateUserSettings(userSettings: UserSettings) = userSettingsDao.update(userSettings)

    fun insertMotivator(motivator: Motivator) = motivatorDao.insert(motivator)

    fun updateMotivator(motivator: Motivator) = motivatorDao.update(motivator)

    fun getMotivator() = motivatorDao.getMotivator()
}
