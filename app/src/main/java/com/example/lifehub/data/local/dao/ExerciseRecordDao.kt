package com.example.lifehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifehub.data.local.entity.ExerciseRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 运动记录DAO - Phase 34
 * 提供运动记录的本地CRUD操作
 */
@Dao
interface ExerciseRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExerciseRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ExerciseRecordEntity>)

    @Update
    suspend fun update(record: ExerciseRecordEntity)

    @Query("SELECT * FROM exercise_records WHERE user_id = :userId ORDER BY exercise_date DESC, created_at DESC")
    suspend fun getRecordsByUserId(userId: Int): List<ExerciseRecordEntity>

    @Query("SELECT * FROM exercise_records WHERE user_id = :userId ORDER BY exercise_date DESC, created_at DESC")
    fun observeRecordsByUserId(userId: Int): Flow<List<ExerciseRecordEntity>>

    @Query("SELECT * FROM exercise_records WHERE user_id = :userId AND exercise_date = :date ORDER BY created_at DESC")
    suspend fun getRecordsByDate(userId: Int, date: String): List<ExerciseRecordEntity>

    @Query("SELECT * FROM exercise_records WHERE localId = :localId")
    suspend fun getRecordByLocalId(localId: Long): ExerciseRecordEntity?

    @Query("SELECT * FROM exercise_records WHERE server_id = :serverId")
    suspend fun getRecordByServerId(serverId: Int): ExerciseRecordEntity?

    @Query("DELETE FROM exercise_records WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("DELETE FROM exercise_records WHERE server_id = :serverId")
    suspend fun deleteByServerId(serverId: Int)

    @Query("SELECT * FROM exercise_records WHERE is_synced = 0 AND user_id = :userId")
    suspend fun getUnsyncedRecords(userId: Int): List<ExerciseRecordEntity>

    @Query("DELETE FROM exercise_records WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: Int)

    @Query("SELECT COUNT(*) FROM exercise_records WHERE user_id = :userId")
    suspend fun getRecordCount(userId: Int): Int

    @Query("SELECT SUM(actual_calories) FROM exercise_records WHERE user_id = :userId AND exercise_date = :date")
    suspend fun getTotalCaloriesByDate(userId: Int, date: String): Double?
}
