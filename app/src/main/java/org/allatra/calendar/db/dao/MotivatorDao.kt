package org.allatra.calendar.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.allatra.calendar.common.Constants.DEFAULT_MOTIVATOR_ID
import org.allatra.calendar.db.entity.Motivator

@Dao
interface MotivatorDao {
    @Query("SELECT * FROM Motivator WHERE uid = $DEFAULT_MOTIVATOR_ID")
    fun getMotivator(): Flow<Motivator>

    @Insert
    fun insert(motivator: Motivator)

    @Update
    fun update(motivator: Motivator)

    @Delete
    fun delete(motivator: Motivator)
}
