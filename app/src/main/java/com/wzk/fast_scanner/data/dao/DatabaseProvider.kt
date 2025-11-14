package com.wzk.fast_scanner.data.dao

import android.content.Context
import androidx.room.Room

/**
 * @author wangzikang
 * @date 2025/11/13 9:46
 */
class DatabaseProvider {

    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
    instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "fast_scanner.db"
        ).build().also { instance = it }
    }

}