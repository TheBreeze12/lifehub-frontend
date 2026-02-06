package com.example.lifehub.data.repository

import com.example.lifehub.data.ExerciseRecordResponseData
import com.example.lifehub.data.local.EntityMapper
import com.example.lifehub.data.local.dao.ExerciseRecordDao
import com.example.lifehub.data.local.entity.ExerciseRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 运动记录仓库 - Phase 34
 * 封装运动记录的本地CRUD操作
 */
class ExerciseRepository(private val exerciseRecordDao: ExerciseRecordDao) {

    /** 保存运动记录到本地（来自云端同步） */
    suspend fun saveFromServer(data: ExerciseRecordResponseData) {
        val entity = EntityMapper.toExerciseRecordEntity(data)
        exerciseRecordDao.insert(entity)
    }

    /** 本地新增运动记录（离线模式） */
    suspend fun addLocalRecord(
        userId: Int,
        planId: Int? = null,
        exerciseType: String = "walking",
        actualCalories: Double,
        actualDuration: Int,
        distance: Double? = null,
        exerciseDate: String,
        startedAt: String? = null,
        endedAt: String? = null,
        notes: String? = null,
        createdAt: String
    ): Long {
        val entity = ExerciseRecordEntity(
            userId = userId,
            planId = planId,
            exerciseType = exerciseType,
            actualCalories = actualCalories,
            actualDuration = actualDuration,
            distance = distance,
            exerciseDate = exerciseDate,
            startedAt = startedAt,
            endedAt = endedAt,
            notes = notes,
            createdAt = createdAt,
            isSynced = false
        )
        return exerciseRecordDao.insert(entity)
    }

    /** 获取用户所有运动记录 */
    suspend fun getRecordsByUserId(userId: Int): List<ExerciseRecordResponseData> {
        return exerciseRecordDao.getRecordsByUserId(userId).map {
            EntityMapper.toExerciseRecordResponseData(it)
        }
    }

    /** 观察用户所有运动记录（Flow） */
    fun observeRecordsByUserId(userId: Int): Flow<List<ExerciseRecordResponseData>> {
        return exerciseRecordDao.observeRecordsByUserId(userId).map { entities ->
            entities.map { EntityMapper.toExerciseRecordResponseData(it) }
        }
    }

    /** 获取指定日期的运动记录 */
    suspend fun getRecordsByDate(userId: Int, date: String): List<ExerciseRecordResponseData> {
        return exerciseRecordDao.getRecordsByDate(userId, date).map {
            EntityMapper.toExerciseRecordResponseData(it)
        }
    }

    /** 通过localId删除记录 */
    suspend fun deleteByLocalId(localId: Long) {
        exerciseRecordDao.deleteByLocalId(localId)
    }

    /** 通过serverId删除记录 */
    suspend fun deleteByServerId(serverId: Int) {
        exerciseRecordDao.deleteByServerId(serverId)
    }

    /** 获取未同步的记录 */
    suspend fun getUnsyncedRecords(userId: Int): List<ExerciseRecordEntity> {
        return exerciseRecordDao.getUnsyncedRecords(userId)
    }

    /** 标记记录为已同步 */
    suspend fun markAsSynced(localId: Long, serverId: Int) {
        val entity = exerciseRecordDao.getRecordByLocalId(localId) ?: return
        exerciseRecordDao.update(entity.copy(serverId = serverId, isSynced = true))
    }

    /** 获取指定日期的总消耗热量 */
    suspend fun getTotalCaloriesByDate(userId: Int, date: String): Double {
        return exerciseRecordDao.getTotalCaloriesByDate(userId, date) ?: 0.0
    }

    /** 获取记录数量 */
    suspend fun getRecordCount(userId: Int): Int {
        return exerciseRecordDao.getRecordCount(userId)
    }

    /** 删除用户所有记录 */
    suspend fun deleteAllByUserId(userId: Int) {
        exerciseRecordDao.deleteAllByUserId(userId)
    }
}
