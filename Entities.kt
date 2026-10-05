package com.disciplinequest.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskType { ONE_TIME, HABIT }
enum class Difficulty { EASY, MEDIUM, HARD, CRITICAL }
enum class TaskStatus { PENDING, COMPLETED, FAILED, RECOVERED }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: TaskType = TaskType.ONE_TIME,
    val category: String = "General",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val deadline: String,
    val xp: Int = 20,
    val coins: Int = 10,
    val hpPenalty: Int = 10,
    val status: TaskStatus = TaskStatus.PENDING,
    val missionId: Long? = null,
    val habitKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "missions")
data class Mission(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val goal: String,
    val startDate: String,
    val deadline: String,
    val targetCount: Int = 30,
    val rewardXp: Int = 200,
    val rewardCoins: Int = 100,
    val completed: Boolean = false
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val frequency: String = "DAILY",
    val xp: Int = 10,
    val coins: Int = 5,
    val hpPenalty: Int = 5,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completionCount: Int = 0,
    val active: Boolean = true
)

@Entity(tableName = "shop_items")
data class ShopItem(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val owned: Boolean = false,
    val equipped: Boolean = false
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: String,
    val completed: Int = 0,
    val failed: Int = 0,
    val recovered: Int = 0,
    val xpEarned: Int = 0,
    val coinsEarned: Int = 0,
    val hpAtEnd: Int = 100
)

@Entity(tableName = "player")
data class Player(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 0,
    val level: Int = 1,
    val hp: Int = 100,
    val coins: Int = 0,
    val overallStreak: Int = 0,
    val bestStreak: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val recovered: Int = 0,
    val disciplineScore: Int = 0,
    val lastActiveDate: String? = null
)
