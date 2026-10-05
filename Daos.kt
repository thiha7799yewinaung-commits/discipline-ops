package com.disciplinequest.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY deadline ASC, id DESC") fun observeAll(): Flow<List<Task>>
    @Insert suspend fun insert(task: Task): Long
    @Insert suspend fun insertAll(tasks: List<Task>)
    @Update suspend fun update(task: Task)
    @Delete suspend fun delete(task: Task)
    @Query("DELETE FROM tasks") suspend fun clear()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY deadline ASC") fun observeAll(): Flow<List<Mission>>
    @Insert suspend fun insert(mission: Mission): Long
    @Update suspend fun update(mission: Mission)
    @Delete suspend fun delete(mission: Mission)
    @Query("DELETE FROM missions") suspend fun clear()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY active DESC, id DESC") fun observeAll(): Flow<List<Habit>>
    @Insert suspend fun insert(habit: Habit): Long
    @Update suspend fun update(habit: Habit)
    @Delete suspend fun delete(habit: Habit)
    @Query("DELETE FROM habits") suspend fun clear()
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id=1 LIMIT 1") fun observe(): Flow<Player?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(player: Player)
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_items ORDER BY category, price") fun observeAll(): Flow<List<ShopItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ShopItem>)
    @Update suspend fun update(item: ShopItem)
    @Query("UPDATE shop_items SET equipped=0 WHERE category=:category") suspend fun unequipCategory(category: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlocked DESC, id") fun observeAll(): Flow<List<Achievement>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Achievement>)
    @Update suspend fun update(item: Achievement)
}

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_records ORDER BY date DESC") fun observeAll(): Flow<List<DailyRecord>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(record: DailyRecord)
    @Query("DELETE FROM daily_records") suspend fun clear()
}
