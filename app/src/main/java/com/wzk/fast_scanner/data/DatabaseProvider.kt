package com.wzk.fast_scanner.data

import android.content.Context
import androidx.room.Room
import com.wzk.fast_scanner.data.dao.AppDatabase
/**
 * @author wangzikang
 * @date 2025/11/13 15:43
 */

/**
 * 单例模式，确保整个应用只有一个数据库实例
 */
object DatabaseProvider {
    
    @Volatile
    private var INSTANCE: AppDatabase? = null
    
    /**
     * 获取数据库实例
     */
    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "fast_scanner_database"
            )
                .fallbackToDestructiveMigration() // 简单起见，数据库升级时清空数据
                .build()
            INSTANCE = instance
            instance
        }
    }
    
    /**
     * 获取 RecordDAO
     */
    fun getRecordDao(context: Context) = getDatabase(context).recordDAO()
}

