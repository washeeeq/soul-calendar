package org.allatra.calendar.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.allatra.calendar.common.Constants.DB_NAME
import org.allatra.calendar.db.conv.Converters
import org.allatra.calendar.db.dao.MotivatorDao
import org.allatra.calendar.db.dao.UserSettingsDao
import org.allatra.calendar.db.entity.Motivator
import org.allatra.calendar.db.entity.UserSettings

@Database(entities = [UserSettings::class, Motivator::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun motivatorDao(): MotivatorDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
