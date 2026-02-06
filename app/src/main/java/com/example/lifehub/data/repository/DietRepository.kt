package com.example.lifehub.data.repository

import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.local.EntityMapper
import com.example.lifehub.data.local.dao.DietRecordDao
import com.example.lifehub.data.local.entity.DietRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 饮食记录仓库 - Phase 34
 * 封装饮食记录的本地CRUD操作，支持离线新增和同步状态追踪
 */
class DietRepository(private val dietRecordDao: DietRecordDao) {

    /** 保存单条饮食记录到本地（来自云端同步） */
    suspend fun saveFromServer(record: DietRecord) {
        val entity = EntityMapper.toDietRecordEntity(record)
        dietRecordDao.insert(entity)
    }

    /** 批量保存饮食记录到本地（来自云端同步） */
    suspend fun saveAllFromServer(records: List<DietRecord>) {
        val entities = EntityMapper.toDietRecordEntities(records)
        dietRecordDao.insertAll(entities)
    }

    /** 本地新增饮食记录（离线模式，isSynced=false） */
    suspend fun addLocalRecord(
        userId: Int,
        foodName: String,
        calories: Double,
        protein: Double = 0.0,
        fat: Double = 0.0,
        carbs: Double = 0.0,
        mealType: String,
        recordDate: String,
        createdAt: String
    ): Long {
        val entity = DietRecordEntity(
            userId = userId,
            foodName = foodName,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            mealType = mealType,
            recordDate = recordDate,
            createdAt = createdAt,
            isSynced = false
        )
        return dietRecordDao.insert(entity)
    }

    /** 获取用户所有饮食记录 */
    suspend fun getRecordsByUserId(userId: Int): List<DietRecord> {
        return EntityMapper.toDietRecords(dietRecordDao.getRecordsByUserId(userId))
    }

    /** 观察用户所有饮食记录（Flow） */
    fun observeRecordsByUserId(userId: Int): Flow<List<DietRecord>> {
        return dietRecordDao.observeRecordsByUserId(userId).map {
            EntityMapper.toDietRecords(it)
        }
    }

    /** 获取指定日期的饮食记录 */
    suspend fun getRecordsByDate(userId: Int, date: String): List<DietRecord> {
        return EntityMapper.toDietRecords(dietRecordDao.getRecordsByDate(userId, date))
    }

    /** 观察指定日期的饮食记录（Flow） */
    fun observeRecordsByDate(userId: Int, date: String): Flow<List<DietRecord>> {
        return dietRecordDao.observeRecordsByDate(userId, date).map {
            EntityMapper.toDietRecords(it)
        }
    }

    /** 更新饮食记录 */
    suspend fun updateRecord(entity: DietRecordEntity) {
        dietRecordDao.update(entity)
    }

    /** 通过localId删除记录 */
    suspend fun deleteByLocalId(localId: Long) {
        dietRecordDao.deleteByLocalId(localId)
    }

    /** 通过serverId删除记录 */
    suspend fun deleteByServerId(serverId: Int) {
        dietRecordDao.deleteByServerId(serverId)
    }

    /** 获取所有记录Entity（用于Phase 35端云同步冲突检测） */
    suspend fun getAllEntities(userId: Int): List<DietRecordEntity> {
        return dietRecordDao.getRecordsByUserId(userId)
    }

    /** 获取未同步的记录（用于Phase 35端云同步） */
    suspend fun getUnsyncedRecords(userId: Int): List<DietRecordEntity> {
        return dietRecordDao.getUnsyncedRecords(userId)
    }

    /** 标记记录为已同步 */
    suspend fun markAsSynced(localId: Long, serverId: Int) {
        val entity = dietRecordDao.getRecordByLocalId(localId) ?: return
        dietRecordDao.update(entity.copy(serverId = serverId, isSynced = true))
    }

    /** 获取指定日期的总热量 */
    suspend fun getTotalCaloriesByDate(userId: Int, date: String): Double {
        return dietRecordDao.getTotalCaloriesByDate(userId, date) ?: 0.0
    }

    /** 获取记录数量 */
    suspend fun getRecordCount(userId: Int): Int {
        return dietRecordDao.getRecordCount(userId)
    }

    /** 删除用户所有记录 */
    suspend fun deleteAllByUserId(userId: Int) {
        dietRecordDao.deleteAllByUserId(userId)
    }
}
