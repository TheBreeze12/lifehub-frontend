package com.example.lifehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifehub.data.local.entity.DietRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 饮食记录DAO - Phase 34
 * 提供饮食记录的本地CRUD操作，支持同步状态跟踪
 */
@Dao
interface DietRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DietRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DietRecordEntity>)

    @Update
    suspend fun update(record: DietRecordEntity)

    @Query("SELECT * FROM diet_records WHERE user_id = :userId ORDER BY record_date DESC, created_at DESC")
    suspend fun getRecordsByUserId(userId: Int): List<DietRecordEntity>

    @Query("SELECT * FROM diet_records WHERE user_id = :userId ORDER BY record_date DESC, created_at DESC")
    fun observeRecordsByUserId(userId: Int): Flow<List<DietRecordEntity>>

    @Query("SELECT * FROM diet_records WHERE user_id = :userId AND record_date = :date ORDER BY created_at DESC")
    suspend fun getRecordsByDate(userId: Int, date: String): List<DietRecordEntity>

    @Query("SELECT * FROM diet_records WHERE user_id = :userId AND record_date = :date ORDER BY created_at DESC")
    fun observeRecordsByDate(userId: Int, date: String): Flow<List<DietRecordEntity>>

    @Query("SELECT * FROM diet_records WHERE localId = :localId")
    suspend fun getRecordByLocalId(localId: Long): DietRecordEntity?

    @Query("SELECT * FROM diet_records WHERE server_id = :serverId")
    suspend fun getRecordByServerId(serverId: Int): DietRecordEntity?

    @Query("DELETE FROM diet_records WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: Long)

    @Query("DELETE FROM diet_records WHERE server_id = :serverId")
    suspend fun deleteByServerId(serverId: Int)

    @Query("SELECT * FROM diet_records WHERE is_synced = 0 AND user_id = :userId")
    suspend fun getUnsyncedRecords(userId: Int): List<DietRecordEntity>

    @Query("DELETE FROM diet_records WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: Int)

    @Query("SELECT COUNT(*) FROM diet_records WHERE user_id = :userId")
    suspend fun getRecordCount(userId: Int): Int

    @Query("SELECT SUM(calories) FROM diet_records WHERE user_id = :userId AND record_date = :date")
    suspend fun getTotalCaloriesByDate(userId: Int, date: String): Double?
}
