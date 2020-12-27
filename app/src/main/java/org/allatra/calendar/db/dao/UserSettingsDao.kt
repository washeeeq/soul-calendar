package org.allatra.calendar.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.allatra.calendar.common.Constants.DEFAULT_USER_SETTINGS_ID
import org.allatra.calendar.db.entity.UserSettings

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM UserSettings WHERE uid = $DEFAULT_USER_SETTINGS_ID")
    fun getUserSettings(): Flow<UserSettings>

    @Insert
    fun insert(userSettings: UserSettings)

    @Update
    fun update(userSettings: UserSettings)

    @Delete
    fun delete(userSettings: UserSettings)
}
