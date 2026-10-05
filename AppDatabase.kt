package com.disciplinequest.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@androidx.room.TypeConverters(Converters::class)
@Database(
    entities = [Task::class, Mission::class, Habit::class, ShopItem::class, Achievement::class, DailyRecord::class, Player::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun missionDao(): MissionDao
    abstract fun habitDao(): HabitDao
    abstract fun playerDao(): PlayerDao
    abstract fun shopDao(): ShopDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyDao(): DailyDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "discipline_quest.db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
