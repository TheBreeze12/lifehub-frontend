package com.example.lifehub.data.sync

import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.local.entity.DietRecordEntity
import com.example.lifehub.data.local.entity.ExerciseRecordEntity

/**
 * 冲突解决器 - Phase 35
 * 当本地和云端数据存在冲突时，决定保留哪个版本
 * 策略：服务端优先（Server-wins），因为服务端是数据权威来源
 * 例外：本地未同步的离线新增记录保留本地版本等待上传
 */
object ConflictResolver {

    /**
     * 解决饮食记录冲突
     * @param local 本地记录Entity
     * @param server 服务端记录
     * @return 解决后的Entity（已标记为已同步）
     */
    fun resolveDietRecord(
        local: DietRecordEntity,
        server: DietRecord
    ): DietRecordEntity {
        // 如果本地记录未同步且没有serverId（离线新增），保留本地版本等待上传
        if (!local.isSynced && local.serverId == null) {
            return local
        }
        // 服务端优先：用服务端数据覆盖本地
        return local.copy(
            serverId = server.id,
            foodName = server.foodName,
            calories = server.calories,
            protein = server.protein,
            fat = server.fat,
            carbs = server.carbs,
            mealType = server.mealType,
            recordDate = server.recordDate,
            createdAt = server.createdAt,
            isSynced = true
        )
    }

    /**
     * 判断是否需要更新本地饮食记录
     * @return true 如果服务端数据与本地不同，需要覆盖
     */
    fun shouldUpdateLocalDietRecord(
        local: DietRecordEntity,
        server: DietRecord
    ): Boolean {
        // 未同步的本地记录不覆盖（保留离线新增数据）
        if (!local.isSynced && local.serverId == null) return false
        // 比较关键字段是否有变化
        return local.foodName != server.foodName ||
               local.calories != server.calories ||
               local.protein != server.protein ||
               local.fat != server.fat ||
               local.carbs != server.carbs ||
               local.mealType != server.mealType ||
               local.recordDate != server.recordDate
    }

    /**
     * 在本地记录列表中查找与服务端记录匹配的本地记录（通过serverId）
     */
    fun findMatchingLocalRecord(
        localRecords: List<DietRecordEntity>,
        serverRecord: DietRecord
    ): DietRecordEntity? {
        return localRecords.find { it.serverId == serverRecord.id }
    }

    /**
     * 在本地运动记录列表中查找匹配的记录（通过serverId）
     */
    fun findMatchingLocalExerciseRecord(
        localRecords: List<ExerciseRecordEntity>,
        serverId: Int
    ): ExerciseRecordEntity? {
        return localRecords.find { it.serverId == serverId }
    }
}
