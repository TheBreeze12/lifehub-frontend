package com.example.lifehub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifehub.data.local.dao.DietRecordDao
import com.example.lifehub.data.local.dao.ExerciseRecordDao
import com.example.lifehub.data.local.dao.TripPlanDao
import com.example.lifehub.data.local.dao.UserDao
import com.example.lifehub.data.local.entity.DietRecordEntity
import com.example.lifehub.data.local.entity.ExerciseRecordEntity
import com.example.lifehub.data.local.entity.TripPlanEntity
import com.example.lifehub.data.local.entity.UserEntity

/**
 * Room数据库 - Phase 34
 * LifeHub本地数据库，支持离线存储用户数据、饮食记录、运动记录、运动计划
 */
@Database(
    entities = [
        UserEntity::class,
        DietRecordEntity::class,
        ExerciseRecordEntity::class,
        TripPlanEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun dietRecordDao(): DietRecordDao
    abstract fun exerciseRecordDao(): ExerciseRecordDao
    abstract fun tripPlanDao(): TripPlanDao

    companion object {
        private const val DATABASE_NAME = "lifehub_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库单例
         * 使用双重检查锁定确保线程安全
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * 仅用于测试：允许注入自定义数据库实例
         */
        internal fun setInstance(database: AppDatabase) {
            INSTANCE = database
        }
    }
}
