package com.example.lifehub.data.repository

import com.example.lifehub.data.UserPreferencesData
import com.example.lifehub.data.local.EntityMapper
import com.example.lifehub.data.local.dao.UserDao
import com.example.lifehub.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用户数据仓库 - Phase 34
 * 封装本地数据库操作，为Phase 35端云同步做准备
 */
class UserRepository(private val userDao: UserDao) {

    /** 保存用户偏好到本地数据库 */
    suspend fun saveUserPreferences(data: UserPreferencesData) {
        val entity = EntityMapper.toUserEntity(data)
        userDao.insertOrUpdate(entity)
    }

    /** 从本地数据库获取用户偏好 */
    suspend fun getUserPreferences(userId: Int): UserPreferencesData? {
        return userDao.getUserById(userId)?.let {
            EntityMapper.toUserPreferencesData(it)
        }
    }

    /** 观察用户偏好变化（Flow） */
    fun observeUserPreferences(userId: Int): Flow<UserPreferencesData?> {
        return userDao.observeUser(userId).map { entity ->
            entity?.let { EntityMapper.toUserPreferencesData(it) }
        }
    }

    /** 直接保存UserEntity */
    suspend fun saveUserEntity(entity: UserEntity) {
        userDao.insertOrUpdate(entity)
    }

    /** 删除用户本地数据 */
    suspend fun deleteUser(userId: Int) {
        userDao.deleteUser(userId)
    }

    /** 清空所有用户数据 */
    suspend fun deleteAll() {
        userDao.deleteAll()
    }

    /** 获取本地用户数量 */
    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
}
