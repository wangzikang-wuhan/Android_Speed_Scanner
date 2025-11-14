package com.wzk.fast_scanner.data.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wzk.fast_scanner.data.RecordEntity

/**
 * @author wangzikang
 * @date 2025/11/13 9:44
 */
@Database(entities = [RecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){

    abstract fun recordDAO(): RecordDAO



}