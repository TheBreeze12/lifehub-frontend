package com.example.lifehub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifehub.data.local.entity.TripPlanEntity
import kotlinx.coroutines.flow.Flow

/**
 * 运动计划DAO - Phase 34
 * 提供运动计划的本地CRUD操作
 */
@Dao
interface TripPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(plan: TripPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<TripPlanEntity>)

    @Update
    suspend fun update(plan: TripPlanEntity)

    @Query("SELECT * FROM trip_plans WHERE user_id = :userId ORDER BY start_date DESC")
    suspend fun getPlansByUserId(userId: Int): List<TripPlanEntity>

    @Query("SELECT * FROM trip_plans WHERE user_id = :userId ORDER BY start_date DESC")
    fun observePlansByUserId(userId: Int): Flow<List<TripPlanEntity>>

    @Query("SELECT * FROM trip_plans WHERE trip_id = :tripId")
    suspend fun getPlanById(tripId: Int): TripPlanEntity?

    @Query("SELECT * FROM trip_plans WHERE trip_id = :tripId")
    fun observePlanById(tripId: Int): Flow<TripPlanEntity?>

    @Query("SELECT * FROM trip_plans WHERE user_id = :userId ORDER BY start_date DESC LIMIT :limit")
    suspend fun getRecentPlans(userId: Int, limit: Int = 5): List<TripPlanEntity>

    @Query("DELETE FROM trip_plans WHERE trip_id = :tripId")
    suspend fun deleteById(tripId: Int)

    @Query("DELETE FROM trip_plans WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: Int)

    @Query("SELECT COUNT(*) FROM trip_plans WHERE user_id = :userId")
    suspend fun getPlanCount(userId: Int): Int
}
