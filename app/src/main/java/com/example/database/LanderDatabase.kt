package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PilotProfile::class, Achievement::class], version = 1, exportSchema = false)
abstract class LanderDatabase : RoomDatabase() {
    abstract fun landerDao(): LanderDao

    companion object {
        @Volatile
        private var INSTANCE: LanderDatabase? = null

        fun getDatabase(context: Context): LanderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LanderDatabase::class.java,
                    "lander_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun initializeAchievements(dao: LanderDao) {
            val initial = listOf(
                Achievement("first_flight", "First Landing", "Successfully land the spacecraft on any planet.", false, null, "flight"),
                Achievement("soft_touch", "Feather Touch", "Land with an ultra-gentle touchdown (vertical speed < 0.5 m/s).", false, null, "eco"),
                Achievement("fuel_saver", "Eco Pilot", "Land with more than 40% fuel remaining.", false, null, "fuel"),
                Achievement("expert_pilot", "Hard Mode Certified", "Successfully touch down on Hard difficulty.", false, null, "star"),
                Achievement("space_explorer", "Space Veteran", "Reach Pilot Level 5.", false, null, "explore"),
                Achievement("crash_test_dummy", "Extreme Metal Crunching", "Crash with a vertical speed exceeding 6.0 m/s.", false, null, "crash"),
                Achievement("rich_pilot", "High Net Worth Pilot", "Reach 500 Credits in your balance.", false, null, "credits")
            )
            dao.insertAchievements(initial)
        }
    }
}
