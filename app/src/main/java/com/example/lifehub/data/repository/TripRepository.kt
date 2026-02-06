package com.example.lifehub.data.repository

import com.example.lifehub.data.TripPlan
import com.example.lifehub.data.TripSummary
import com.example.lifehub.data.local.EntityMapper
import com.example.lifehub.data.local.dao.TripPlanDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 运动计划仓库 - Phase 34
 * 封装运动计划的本地CRUD操作，支持离线查看已下载的计划
 */
class TripRepository(private val tripPlanDao: TripPlanDao) {

    /** 保存运动计划到本地（来自云端） */
    suspend fun savePlan(plan: TripPlan, userId: Int) {
        val entity = EntityMapper.toTripPlanEntity(plan, userId)
        tripPlanDao.insertOrUpdate(entity)
    }

    /** 批量保存运动计划 */
    suspend fun saveAllPlans(plans: List<TripPlan>, userId: Int) {
        val entities = plans.map { EntityMapper.toTripPlanEntity(it, userId) }
        tripPlanDao.insertAll(entities)
    }

    /** 获取运动计划详情 */
    suspend fun getPlanById(tripId: Int): TripPlan? {
        return tripPlanDao.getPlanById(tripId)?.let {
            EntityMapper.toTripPlan(it)
        }
    }

    /** 观察运动计划详情（Flow） */
    fun observePlanById(tripId: Int): Flow<TripPlan?> {
        return tripPlanDao.observePlanById(tripId).map { entity ->
            entity?.let { EntityMapper.toTripPlan(it) }
        }
    }

    /** 获取用户所有运动计划摘要 */
    suspend fun getPlanSummaries(userId: Int): List<TripSummary> {
        return tripPlanDao.getPlansByUserId(userId).map {
            EntityMapper.toTripSummary(it)
        }
    }

    /** 观察用户所有运动计划（Flow） */
    fun observePlansByUserId(userId: Int): Flow<List<TripSummary>> {
        return tripPlanDao.observePlansByUserId(userId).map { entities ->
            entities.map { EntityMapper.toTripSummary(it) }
        }
    }

    /** 获取最近的运动计划 */
    suspend fun getRecentPlans(userId: Int, limit: Int = 5): List<TripSummary> {
        return tripPlanDao.getRecentPlans(userId, limit).map {
            EntityMapper.toTripSummary(it)
        }
    }

    /** 删除运动计划 */
    suspend fun deletePlan(tripId: Int) {
        tripPlanDao.deleteById(tripId)
    }

    /** 删除用户所有计划 */
    suspend fun deleteAllByUserId(userId: Int) {
        tripPlanDao.deleteAllByUserId(userId)
    }

    /** 获取计划数量 */
    suspend fun getPlanCount(userId: Int): Int {
        return tripPlanDao.getPlanCount(userId)
    }
}
