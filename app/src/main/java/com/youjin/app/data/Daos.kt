package com.youjin.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY pinned DESC, pinOrder ASC, updatedAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("DELETE FROM goals")
    suspend fun deleteAll()

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT COALESCE(MAX(pinOrder), 0) FROM goals")
    suspend fun maxPinOrder(): Int

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM progress_entries ORDER BY occurDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<ProgressEntryEntity>>

    @Query("SELECT * FROM progress_entries WHERE goalId = :goalId ORDER BY occurDate DESC, createdAt DESC")
    fun observeByGoal(goalId: Long): Flow<List<ProgressEntryEntity>>

    @Query("SELECT * FROM progress_entries WHERE goalId = :goalId ORDER BY occurDate DESC, createdAt DESC")
    suspend fun getByGoal(goalId: Long): List<ProgressEntryEntity>

    @Query("SELECT * FROM progress_entries WHERE id = :id")
    suspend fun getById(id: Long): ProgressEntryEntity?

    @Query("SELECT * FROM progress_entries")
    suspend fun getAll(): List<ProgressEntryEntity>

    @Insert
    suspend fun insert(entry: ProgressEntryEntity): Long

    @Update
    suspend fun update(entry: ProgressEntryEntity)

    @Query("DELETE FROM progress_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM progress_entries")
    suspend fun deleteAll()
}

@Dao
interface StepDao {
    @Query("SELECT * FROM goal_steps ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<GoalStepEntity>>

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY sortOrder ASC, id ASC")
    fun observeByGoal(goalId: Long): Flow<List<GoalStepEntity>>

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByGoal(goalId: Long): List<GoalStepEntity>

    @Query("SELECT * FROM goal_steps WHERE id = :id")
    suspend fun getById(id: Long): GoalStepEntity?

    @Query("SELECT * FROM goal_steps")
    suspend fun getAll(): List<GoalStepEntity>

    @Insert
    suspend fun insert(step: GoalStepEntity): Long

    @Update
    suspend fun update(step: GoalStepEntity)

    @Delete
    suspend fun delete(step: GoalStepEntity)

    @Query("DELETE FROM goal_steps")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE goalId = :goalId LIMIT 1")
    fun observeByGoal(goalId: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE goalId = :goalId LIMIT 1")
    suspend fun getByGoal(goalId: Long): ReminderEntity?

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE goalId = :goalId")
    suspend fun deleteByGoal(goalId: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM weekly_reviews ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<WeeklyReviewEntity>>

    @Query("SELECT * FROM weekly_reviews WHERE weekStart = :weekStart LIMIT 1")
    fun observeByWeek(weekStart: Long): Flow<WeeklyReviewEntity?>

    @Query("SELECT * FROM weekly_reviews WHERE weekStart = :weekStart LIMIT 1")
    suspend fun getByWeek(weekStart: Long): WeeklyReviewEntity?

    @Query("SELECT * FROM weekly_reviews")
    suspend fun getAll(): List<WeeklyReviewEntity>

    @Upsert
    suspend fun upsert(review: WeeklyReviewEntity)

    @Query("DELETE FROM weekly_reviews")
    suspend fun deleteAll()
}

@Dao
interface ChangeDao {
    @Insert
    suspend fun insert(change: GoalChangeEntity)

    @Query("SELECT * FROM goal_changes ORDER BY changedAt DESC")
    suspend fun getAll(): List<GoalChangeEntity>

    @Query("DELETE FROM goal_changes")
    suspend fun deleteAll()
}
