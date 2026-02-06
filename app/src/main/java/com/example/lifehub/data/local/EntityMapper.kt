package com.example.lifehub.data.local

import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.TripItem
import com.example.lifehub.data.TripPlan
import com.example.lifehub.data.TripSummary
import com.example.lifehub.data.UserPreferencesData
import com.example.lifehub.data.ExerciseRecordResponseData
import com.example.lifehub.data.local.entity.DietRecordEntity
import com.example.lifehub.data.local.entity.ExerciseRecordEntity
import com.example.lifehub.data.local.entity.TripPlanEntity
import com.example.lifehub.data.local.entity.UserEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entity与网络模型之间的映射工具 - Phase 34
 * 负责本地Entity和API数据模型之间的相互转换
 */
object EntityMapper {
    private val gson = Gson()

    // ==================== User 映射 ====================

    /** 网络模型 -> 本地Entity */
    fun toUserEntity(data: UserPreferencesData): UserEntity {
        return UserEntity(
            id = data.userId,
            nickname = data.nickname,
            healthGoal = data.healthGoal,
            allergens = data.allergens,
            travelPreference = data.travelPreference,
            dailyBudget = data.dailyBudget,
            weight = data.weight,
            height = data.height,
            age = data.age,
            gender = data.gender,
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    /** 本地Entity -> 网络模型 */
    fun toUserPreferencesData(entity: UserEntity): UserPreferencesData {
        return UserPreferencesData(
            userId = entity.id,
            nickname = entity.nickname,
            healthGoal = entity.healthGoal,
            allergens = entity.allergens,
            travelPreference = entity.travelPreference,
            dailyBudget = entity.dailyBudget,
            weight = entity.weight,
            height = entity.height,
            age = entity.age,
            gender = entity.gender
        )
    }

    // ==================== DietRecord 映射 ====================

    /** 网络模型 -> 本地Entity */
    fun toDietRecordEntity(record: DietRecord): DietRecordEntity {
        return DietRecordEntity(
            serverId = record.id,
            userId = record.userId,
            foodName = record.foodName,
            calories = record.calories,
            protein = record.protein,
            fat = record.fat,
            carbs = record.carbs,
            mealType = record.mealType,
            recordDate = record.recordDate,
            createdAt = record.createdAt,
            isSynced = true
        )
    }

    /** 本地Entity -> 网络模型 */
    fun toDietRecord(entity: DietRecordEntity): DietRecord {
        return DietRecord(
            id = entity.serverId ?: entity.localId.toInt(),
            userId = entity.userId,
            foodName = entity.foodName,
            calories = entity.calories,
            protein = entity.protein,
            fat = entity.fat,
            carbs = entity.carbs,
            mealType = entity.mealType,
            recordDate = entity.recordDate,
            createdAt = entity.createdAt
        )
    }

    /** 批量转换：网络模型列表 -> Entity列表 */
    fun toDietRecordEntities(records: List<DietRecord>): List<DietRecordEntity> {
        return records.map { toDietRecordEntity(it) }
    }

    /** 批量转换：Entity列表 -> 网络模型列表 */
    fun toDietRecords(entities: List<DietRecordEntity>): List<DietRecord> {
        return entities.map { toDietRecord(it) }
    }

    // ==================== ExerciseRecord 映射 ====================

    /** 网络模型 -> 本地Entity */
    fun toExerciseRecordEntity(data: ExerciseRecordResponseData): ExerciseRecordEntity {
        return ExerciseRecordEntity(
            serverId = data.id,
            userId = data.userId,
            planId = data.planId,
            exerciseType = data.exerciseType,
            actualCalories = data.actualCalories,
            actualDuration = data.actualDuration,
            distance = data.distance,
            exerciseDate = data.exerciseDate,
            startedAt = data.startedAt,
            endedAt = data.endedAt,
            notes = data.notes,
            createdAt = data.createdAt,
            isSynced = true
        )
    }

    /** 本地Entity -> 网络模型 */
    fun toExerciseRecordResponseData(entity: ExerciseRecordEntity): ExerciseRecordResponseData {
        return ExerciseRecordResponseData(
            id = entity.serverId ?: entity.localId.toInt(),
            userId = entity.userId,
            planId = entity.planId,
            exerciseType = entity.exerciseType,
            actualCalories = entity.actualCalories,
            actualDuration = entity.actualDuration,
            distance = entity.distance,
            exerciseDate = entity.exerciseDate,
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
            notes = entity.notes,
            createdAt = entity.createdAt
        )
    }

    // ==================== TripPlan 映射 ====================

    /** 网络模型 -> 本地Entity */
    fun toTripPlanEntity(plan: TripPlan, userId: Int): TripPlanEntity {
        return TripPlanEntity(
            tripId = plan.tripId,
            userId = userId,
            title = plan.title,
            destination = plan.destination,
            startDate = plan.startDate,
            endDate = plan.endDate,
            itemsJson = gson.toJson(plan.items),
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    /** 本地Entity -> TripPlan（含items反序列化） */
    fun toTripPlan(entity: TripPlanEntity): TripPlan {
        val itemsType = object : TypeToken<List<TripItem>>() {}.type
        val items: List<TripItem> = try {
            gson.fromJson(entity.itemsJson, itemsType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return TripPlan(
            tripId = entity.tripId,
            title = entity.title,
            destination = entity.destination,
            startDate = entity.startDate,
            endDate = entity.endDate,
            items = items
        )
    }

    /** 本地Entity -> TripSummary（列表展示用） */
    fun toTripSummary(entity: TripPlanEntity): TripSummary {
        val itemsType = object : TypeToken<List<TripItem>>() {}.type
        val items: List<TripItem> = try {
            gson.fromJson(entity.itemsJson, itemsType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return TripSummary(
            tripId = entity.tripId,
            title = entity.title,
            destination = entity.destination,
            startDate = entity.startDate,
            endDate = entity.endDate,
            status = null,
            itemCount = items.size
        )
    }
}
